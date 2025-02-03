package org.example.all_users.admin.commands.actual_event;

import lombok.RequiredArgsConstructor;
import org.example.all_users.admin.commands.AdminStart;
import org.example.data_classes.data_base.comparison_tables.EventVisit;
import org.example.data_classes.data_base.entity.Event;
import org.example.data_classes.data_base.entity.Usr;
import org.example.data_classes.enums.UserState;
import org.example.repository.EventMissingRepository;
import org.example.repository.EventSubscriptionRepository;
import org.example.repository.EventVisitRepository;
import org.example.repository.MySQLInfo;
import org.example.repository.UserRepository;
import org.example.repository.EventRepository;
import org.example.util.state.StateManager;
import org.example.util.telegram.api.TelegramSender;
import org.example.util.telegram.helpers.ActionsChainUtil;
import org.example.util.telegram.helpers.UpdateUtil;
import org.example.util.validation.StringValidator;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminRegistrateUsers {
    private final EventRepository eventRepository;
    private final TelegramSender telegramSender;
    private final UpdateUtil updateUtil;
    private final StateManager stateManager;
    private final StringValidator stringValidator;
    private final UserRepository userRepository;

    private static final Map<Long, Long> adminCurrentEvent = new HashMap<>();
    private final AdminStart adminStart;
    private final EventVisitRepository eventVisitRepository;
    private final EventSubscriptionRepository eventSubscriptionRepository;
    private final ActionsChainUtil actionsChainUtil;
    private final MySQLInfo mySQLInfo;
    private final EventMissingRepository eventMissingRepository;

    public void processCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = updateUtil.getChatId(update);
        String[] callbackTextArray = callbackData.split("_");

        switch (callbackTextArray[1]) {
            case "start-marking" -> handleMarkingVisitors(chatId, callbackData, messageId);
            default -> sendUnknownCallbackResponse(chatId);
        }

        telegramSender.answerCallbackQuerry(chatId, AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.getId())
                .text("""
                        Команда обработана.""")
                .showAlert(false)
                .build());
    }

    private void sendUnknownCallbackResponse(Long chatId) {
        SendMessage unknownCallbackMessage = new SendMessage();
        unknownCallbackMessage.setChatId(chatId.toString());
        unknownCallbackMessage.setText("Неизвестная команда.");

        telegramSender.sendText(chatId, unknownCallbackMessage);
    }

    public void handleMarkingVisitors(Long chatId, String callbackText, Integer messageId) {
        String[] callbackTextArray = callbackText.split("_");
        Long eventId = Long.parseLong(callbackTextArray[2]);
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (eventOptional.isPresent()) {
            Event event = eventOptional.get();
            if (mySQLInfo.getCurrentTimeStamp().isBefore(event.getStartTime().minusHours(24))) {
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                            Регистрация начнётся за сутки до начала мероприятия.""")
                        .build());
                stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
                return;
            }

            ReplyKeyboardRemove replyKeyboardRemove = ReplyKeyboardRemove.builder()
                    .removeKeyboard(true)
                    .build();
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .replyMarkup(replyKeyboardRemove)
                    .text("""
                            Введите ID участника""")
                    .build());
            adminCurrentEvent.put(chatId, eventId);
            stateManager.setUserState(chatId, UserState.ENTERING_VISITOR_ID);
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Мероприятие не найдено.""")
                    .build());

            telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .build());

            stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
        }
    }

    public void checkVisitorID(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Long visitorID = stringValidator.validateVisitorID(chatId, update.getMessage().getText());

        if (visitorID != null) {
            Optional<Usr> visitorOptional = userRepository.findByUserId(visitorID);
            if (visitorOptional.isPresent()) {
                Long eventId = adminCurrentEvent.get(chatId);
                Usr visitor = visitorOptional.get();

                if (validateVisitor(chatId, visitor.getChatId(), visitorID, eventId, update)) {
                    EventVisit eventVisit = new EventVisit();
                    eventVisit.setChatId(visitor.getChatId());
                    eventVisit.setEventId(eventId);
//                    visitor.setNumberOfVisitedEvents(visitor.getNumberOfVisitedEvents() + 1);
                    eventVisitRepository.save(eventVisit);
                    userRepository.save(visitor);
                    eventMissingRepository.removeEventMissingsByEventIdAndChatId(eventId, visitor.getChatId());
                    telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text(String.format("""
                                Отметили пользователя с ID *%s*.""", visitorID))
                            .build());

                    offerEnterNextVisitorID(chatId);
                }
            } else {
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text(String.format("""
                                Пользователя с ID *%s* не существует.""", visitorID))
                        .build());
                offerEnterNextVisitorID(chatId);
            }
        }
    }

    private boolean validateVisitor(Long chatId, Long visitorChatId, Long visitorID, Long eventId, Update update) {
        if (eventId == null || !eventRepository.existsById(eventId)) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                        Ошибка, попробуйте ещё раз.""")
                    .build());
            adminStart.handleStartState(update);
            return false;
        }

        if (eventVisitRepository.existsByChatIdAndEventId(visitorChatId, eventId)) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                                Пользователя с ID *%s* уже отмечен.""", visitorID))
                    .build());
            offerEnterNextVisitorID(chatId);
            return false;
        }

        if (!eventSubscriptionRepository.existsByChatIdAndEventId(visitorChatId, eventId)) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                                Пользователя с ID *%s* не подписан на данное мероприятие(""", visitorID))
                    .build());
            offerEnterNextVisitorID(chatId);
            return false;
        }

        return true;
    }

    private void offerEnterNextVisitorID(Long chatId) {
        actionsChainUtil.offerNextAction(chatId, """
                            Продолжить регистрировать посетителей?""", UserState.ACCEPTING_MARKING_VISITORS);
    }

    public void checkAnswer(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Boolean answer = actionsChainUtil.checkAnswer(update);

        if (answer == null) {
            return;
        }

        if (answer) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Введите ID участника""")
                    .build());
            stateManager.setUserState(chatId, UserState.ENTERING_VISITOR_ID);
        } else {
            adminStart.handleStartState(update);
        }
    }
}
