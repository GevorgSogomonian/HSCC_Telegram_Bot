package org.example.data_classes.data_base.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Usr {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long chatId;

    private String username;
    private String firstName;
    private String lastName;
    private String languageCode;
    private Boolean isPremium;
    private Boolean isBot;
    private Long userId;
//    private Integer numberOfVisitedEvents;
//    private Integer numberOfMissedEvents;
//    private String subscribedEventIds;
    private Boolean isAdminClone;
    private Boolean isHSEStudent;
}