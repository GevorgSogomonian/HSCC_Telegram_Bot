package org.example.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class EventSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long chatId;
    private Long eventId;

//    @ManyToOne
//    @JoinColumn(name = "user_id", nullable = false)
//    private Usr user; // Пользователь, который оценил фильм
//
//    @ManyToOne
//    @JoinColumn(name = "event_id", nullable = false)
//    private Event event; // Ссылка на фильм
}
