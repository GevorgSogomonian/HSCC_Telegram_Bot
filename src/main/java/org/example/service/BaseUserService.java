package org.example.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;

@Service
public class BaseUserService {
    public List<SendMessage> onUpdateRecieved (Update update) {
        return new ArrayList<>();
    }

    @PostConstruct
    public void init() {
//        System.out.println("Username: " + botUsername);
//        System.out.println("Token: " + botToken);
//
//        //Для администратора
//        commandHandlers.put("Все мероприятия", this::handleAllEventsCommand);
//        commandHandlers.put("Новое мероприятие", this::handleNewEventCommand);

//        commandHandlers.put(BotState.REGISTRATION, this::startRegisterNewUser);
//
//        //Для обычных пользователей
//        commandHandlers.put("Доступные мероприятия", this::handleMostPersonalCommand);
//        commandHandlers.put("Мои мероприятия", this::handlePersonalCommand);
    }
}
