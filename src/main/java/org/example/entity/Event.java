package org.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.Duration;

@Data
@Entity
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long creatorChatId;
    private String eventName;
    private String description;
    private String telegramFileId;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "duration")
    private Duration duration;

    @Column(name = "image_url")
    private String imageUrl;

    @Override
    public String toString() {
        return String.format(
                """
                        *%s*:
                        
                        %s
                        
                        Начало: %s
                        Продолжительность: %s минут""",
                eventName, description, startTime, duration.toMinutes()
        );
    }
}
