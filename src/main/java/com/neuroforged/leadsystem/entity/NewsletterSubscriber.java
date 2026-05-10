package com.neuroforged.leadsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * LSB-152: Newsletter subscriber — idempotent on email (unique constraint).
 */
@Entity
@Table(name = "newsletter_subscriber")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 60)
    private String source;

    @Column(name = "subscribed_at", nullable = false)
    private LocalDateTime subscribedAt;
}
