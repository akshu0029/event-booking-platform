package com.eventbooking.platform.dto;

import com.eventbooking.platform.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private Integer numberOfSeats;
    private Double totalAmount;
    private BookingStatus status;
    private LocalDateTime createdAt;
}