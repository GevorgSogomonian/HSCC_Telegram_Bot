package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.ChatBotResponse;
import org.example.entity.Role;
import org.example.entity.UserState;
import org.example.entity.Usr;
import org.example.telegram_api.TelegramApiQueue;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class RegistrationUserService {

    private final TelegramApiQueue telegramApiQueue;
    private final UpdateUtil updateUtil;
    private final UserStateService userStateService;
    private final CommandProcessingService commandProcessingService;
    private final MessageDistributor messageDistributor;

    public void onUpdateRecieved(Update update) {
        Long chatId = updateUtil.getChatId(update);

        if (!userStateService.checkEntry(chatId)) {
            userStateService.start(update, getActionsQueue());
        } else {
            userStateService.executeNextAction(update);
        }
    }

    private Queue<Consumer<Update>> getActionsQueue() {
        Queue<Consumer<Update>> actions = new LinkedList<>();
        actions.add(this::startRegisterNewUser);
        actions.add(this::roleChooser);
        actions.add(this::adminPasswordCheck);
        return actions;
    }

    private void startRegisterNewUser(Update update) {
        Long chatId = update.getMessage().getChatId();

        if (update.hasMessage() && update.getMessage().getFrom() != null) {

            telegramApiQueue.addResponse(ChatBotResponse.builder()
                            .chatId(chatId)
                            .method(SendMessage.builder()
                                    .chatId(chatId)
                                    .text("""
                    Давайте начнём регистрацию!
                    
                    Выберите свою роль:
                    (admin)
                    (usr)
                
                    ps: чтобы стать администратором вам нужен специальный ключ!""")
                                    .build())
                    .build());

            userStateService.completeCurrentAction(update);
        }
    }

    private void roleChooser(Update update) {
        Long chatId = update.getMessage().getChatId();
        String userMessage = update.getMessage().getText();

        switch (userMessage) {
            case "admin":
                telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                Введите специальный ключ:""")
                        .build()));
                userStateService.completeCurrentAction(update);
                break;

            case "usr":
                commandProcessingService.saveNewUser(update, Role.USER);
                telegramApiQueue.addResponse(ChatBotResponse.builder()
                                .chatId(chatId)
                                .method(SendMessage.builder()
                                        .chatId(chatId)
                                        .text("""
                                                Поздравляю, ты обычный юзер""")
                                        .build())
                        .build());
                userStateService.completeAllActions(chatId);
                messageDistributor.processTextMessage(update);
                break;

            default:
                telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                Введите корректные данные.""")
                        .build()));
        }
    }

    private void adminPasswordCheck(Update update) {
        String message = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if (message.equals("1234")) {
            telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Ключ верный.""")
                    .build()));
            commandProcessingService.saveNewUser(update, Role.ADMIN);

            userStateService.completeAllActions(chatId);
            messageDistributor.processTextMessage(update);
        } else {
            telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Ключ неверный.
                            
                            Начните регистрацию заново)""")
                    .build()));
            userStateService.start(update, getActionsQueue());
        }
    }

//    @Override
//    public void endTask(Update update) {
//        Usr user = updateUtil.getUser(update).get();
//        Role userRole = user.getRole();
//
//        switch (userRole) {
//            case ADMIN -> user.setUserState(UserState.ADMIN_START);
//            case USER -> user.setUserState(UserState.BASE_START);
//        }
//    }
}
