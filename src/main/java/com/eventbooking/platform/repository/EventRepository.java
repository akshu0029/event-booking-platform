package com.eventbooking.platform.repository;

import com.eventbooking.platform.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByEventDateTimeAfterOrderByEventDateTimeAsc(LocalDateTime now);

    List<Event> findByTitleContainingIgnoreCase(String keyword);
}