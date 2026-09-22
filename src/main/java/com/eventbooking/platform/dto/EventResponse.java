package com.eventbooking.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private String venue;
    private LocalDateTime eventDateTime;
    private Integer totalSeats;
    private Integer availableSeats;
    private Double ticketPrice;
    private String organizerName;
    private Long organizerId;
}