package com.eventbooking.platform.service;

import com.eventbooking.platform.dto.EventRequest;
import com.eventbooking.platform.dto.EventResponse;
import com.eventbooking.platform.entity.Event;
import com.eventbooking.platform.entity.User;
import com.eventbooking.platform.repository.EventRepository;
import com.eventbooking.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventResponse createEvent(EventRequest request, Authentication authentication) {

        String organizerEmail = authentication.getName();
        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Organizer not found"));

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .venue(request.getVenue())
                .eventDateTime(request.getEventDateTime())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .ticketPrice(request.getTicketPrice())
                .organizer(organizer)
                .build();

        Event savedEvent = eventRepository.save(event);
        return toResponse(savedEvent);
    }

    public List<EventResponse> getUpcomingEvents() {
        return eventRepository.findByEventDateTimeAfterOrderByEventDateTimeAsc(LocalDateTime.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        return toResponse(event);
    }

    private EventResponse toResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .venue(event.getVenue())
                .eventDateTime(event.getEventDateTime())
                .totalSeats(event.getTotalSeats())
                .availableSeats(event.getAvailableSeats())
                .ticketPrice(event.getTicketPrice())
                .organizerName(event.getOrganizer().getFullName())
                .organizerId(event.getOrganizer().getId())
                .build();
    }
}