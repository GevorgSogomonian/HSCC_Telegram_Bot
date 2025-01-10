package org.example.entity;

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
    private Long chatId; // Идентификатор чата (уникальный)

    private String username;
    private String firstName;
    private String lastName;
    private Role role;
    private String languageCode; // Код языка пользователя (например, "ru", "en")
    private Boolean isPremium; // Информация о премиум-аккаунте
    private Boolean isBot; // Является ли пользователь ботом
    private UserState userState;
    private Boolean registered;
}