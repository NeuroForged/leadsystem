package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.NotificationChannel;
import com.neuroforged.leadsystem.entity.NotificationEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, Long> {

    List<NotificationChannel> findByClientId(Long clientId);

    @Query("SELECT nc FROM NotificationChannel nc JOIN nc.events e WHERE nc.client.id = :clientId AND e = :event")
    List<NotificationChannel> findByClientIdAndEvent(Long clientId, NotificationEventType event);
}
