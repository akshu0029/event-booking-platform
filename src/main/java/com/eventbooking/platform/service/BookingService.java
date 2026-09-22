package com.eventbooking.platform.service;

import com.eventbooking.platform.dto.BookingRequest;
import com.eventbooking.platform.dto.BookingResponse;
import com.eventbooking.platform.entity.Booking;
import com.eventbooking.platform.entity.BookingActivityLog;
import com.eventbooking.platform.entity.Event;
import com.eventbooking.platform.entity.User;
import com.eventbooking.platform.enums.BookingStatus;
import com.eventbooking.platform.repository.BookingActivityLogRepository;
import com.eventbooking.platform.repository.BookingRepository;
import com.eventbooking.platform.repository.EventRepository;
import com.eventbooking.platform.repository.UserRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final BookingActivityLogRepository activityLogRepository;

    @Value("${seat-hold.ttl-seconds}")
    private long holdTtlSeconds;

    @Transactional
    public BookingResponse bookTickets(BookingRequest request, Authentication authentication) {

        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (event.getAvailableSeats() < request.getNumberOfSeats()) {
            throw new IllegalStateException("Not enough seats available. Only "
                    + event.getAvailableSeats() + " left.");
        }

        try {
            event.setAvailableSeats(event.getAvailableSeats() - request.getNumberOfSeats());
            eventRepository.save(event);
        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException ex) {
            throw new IllegalStateException("Seats were just booked by someone else. Please try again.");
        }

        Double totalAmount = event.getTicketPrice() * request.getNumberOfSeats();
        String holdReference = UUID.randomUUID().toString();

        Booking booking = Booking.builder()
                .user(user)
                .event(event)
                .numberOfSeats(request.getNumberOfSeats())
                .totalAmount(totalAmount)
                .status(BookingStatus.HELD)
                .holdReference(holdReference)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        redisService.set("booking-hold:" + savedBooking.getId(), holdReference, holdTtlSeconds);

        logActivity(user.getId(), event.getId(), savedBooking.getId(), "BOOKING_HELD",
                "User held " + request.getNumberOfSeats() + " seat(s) for event: " + event.getTitle());

        return toResponse(savedBooking);
    }

    @Transactional
    public BookingResponse confirmBooking(Long bookingId, Authentication authentication) {

        String userEmail = authentication.getName();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("Booking not found");
        }

        if (booking.getStatus() != BookingStatus.HELD) {
            throw new IllegalStateException("This booking is no longer pending confirmation.");
        }

        String redisKey = "booking-hold:" + booking.getId();
        if (!redisService.exists(redisKey)) {
            throw new IllegalStateException("Your hold has expired. Please book again.");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);

        redisService.delete(redisKey);

        logActivity(booking.getUser().getId(), booking.getEvent().getId(), booking.getId(),
                "BOOKING_CONFIRMED", "Booking confirmed for event: " + booking.getEvent().getTitle());

        return toResponse(savedBooking);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredHolds() {
        List<Booking> heldBookings = bookingRepository.findByStatus(BookingStatus.HELD);
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(holdTtlSeconds);

        for (Booking booking : heldBookings) {
            if (booking.getCreatedAt().isBefore(cutoff)) {
                Event event = booking.getEvent();
                event.setAvailableSeats(event.getAvailableSeats() + booking.getNumberOfSeats());
                eventRepository.save(event);

                booking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);

                redisService.delete("booking-hold:" + booking.getId());

                logActivity(booking.getUser().getId(), event.getId(), booking.getId(),
                        "BOOKING_EXPIRED", "Hold expired and seats released for event: " + event.getTitle());
            }
        }
    }

    public List<BookingResponse> getMyBookings(Authentication authentication) {
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return bookingRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void logActivity(Long userId, Long eventId, Long bookingId, String action, String message) {
        BookingActivityLog log = BookingActivityLog.builder()
                .userId(userId)
                .eventId(eventId)
                .bookingId(bookingId)
                .action(action)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        activityLogRepository.save(log);
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .eventId(booking.getEvent().getId())
                .eventTitle(booking.getEvent().getTitle())
                .numberOfSeats(booking.getNumberOfSeats())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}