package com.eventbooking.platform.repository;

import com.eventbooking.platform.entity.BookingActivityLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BookingActivityLogRepository extends MongoRepository<BookingActivityLog, String> {
    List<BookingActivityLog> findByUserId(Long userId);
    List<BookingActivityLog> findByEventId(Long eventId);
}