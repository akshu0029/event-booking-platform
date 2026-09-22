package com.eventbooking.platform.controller;

import com.eventbooking.platform.dto.BookingRequest;
import com.eventbooking.platform.service.BookingService;
import com.eventbooking.platform.service.EventService;
import com.eventbooking.platform.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final EventService eventService;
    private final BookingService bookingService;
    private final RedisService redisService;

    @GetMapping("/web/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/web/events")
    public String eventsPage(Model model) {
        model.addAttribute("events", eventService.getUpcomingEvents());
        return "events";
    }

    @GetMapping("/web/events/{id}")
    public String eventDetailPage(@PathVariable Long id, Model model) {
        model.addAttribute("event", eventService.getEventById(id));
        return "event-detail";
    }

    @PostMapping("/web/events/{id}/book")
    public String bookEvent(
            @PathVariable Long id,
            @RequestParam Integer numberOfSeats,
            Authentication authentication,
            Model model
    ) {
        try {
            BookingRequest request = new BookingRequest();
            request.setEventId(id);
            request.setNumberOfSeats(numberOfSeats);

            bookingService.bookTickets(request, authentication);

            model.addAttribute("event", eventService.getEventById(id));
            model.addAttribute("success", "Booking confirmed!");
        } catch (Exception ex) {
            model.addAttribute("event", eventService.getEventById(id));
            model.addAttribute("error", ex.getMessage());
        }

        return "event-detail";
    }
    @PostMapping("/web/bookings/{id}/confirm")
    public String confirmBookingWeb(@PathVariable Long id, Authentication authentication, Model model) {
        try {
            bookingService.confirmBooking(id, authentication);
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
        }
        model.addAttribute("bookings", bookingService.getMyBookings(authentication));
        return "my-bookings";
    }

    @GetMapping("/web/my-bookings")
    public String myBookingsPage(Authentication authentication, Model model) {
        model.addAttribute("bookings", bookingService.getMyBookings(authentication));
        return "my-bookings";
    }

    
}