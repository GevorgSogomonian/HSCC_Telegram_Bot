package org.example.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Data
@Entity
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long creatorChatId;
    private String eventName;

    @Column(name = "description", length = 1024)
    private String description;
    private String eventLocation;
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

//    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
//    private Set<EventSubscription> subscriptions = new HashSet<>();

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM 'в' HH:mm", new Locale("ru"));
        String formattedStartTime = startTime != null ? startTime.format(formatter) : "Не указано";
        String formattedDuration;

        if (duration != null) {
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();

            if (minutes == 0) {
                formattedDuration = hours + " час" + (hours > 1 ? "а" : "");
            } else if (minutes == 30) {
                formattedDuration = hours + ",5 часа";
            } else {
                formattedDuration = hours + " час" + (hours > 1 ? "а" : "") + " " + minutes + " минут";
            }
        } else {
            formattedDuration = "Не указано";
        }

        return String.format(
                """
                *%s*:
    
                %s
    
                Начало: *%s*
                Продолжительность: *%s*
                Место проведения: *%s*""",
                eventName != null ? eventName : "Не указано",
                description != null ? description : "Описание отсутствует",
                formattedStartTime,
                formattedDuration,
                eventLocation
        );
    }

    public String getFormattedStartDate() {
        if (startTime == null) {
            return "Дата начала не указана";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'года в' HH:mm", new Locale("ru"));

        return startTime.format(formatter);
    }

    public String getFormattedDuration() {
        if (duration == null) {
            return "Продолжительность не указана";
        }

        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (minutes == 0) {
            return hours + " час" + (hours > 1 ? "а" : "");
        } else if (minutes == 30) {
            return hours + ",5 часа";
        } else {
            return hours + " час" + (hours > 1 ? "а" : "") + " " + minutes + " минут";
        }
    }
}
