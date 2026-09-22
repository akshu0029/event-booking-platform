package com.eventbooking.platform.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "booking_activity_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingActivityLog {

    @Id
    private String id;

    private Long userId;
    private Long eventId;
    private Long bookingId;

    private String action;

    private String message;

    private LocalDateTime timestamp;
}