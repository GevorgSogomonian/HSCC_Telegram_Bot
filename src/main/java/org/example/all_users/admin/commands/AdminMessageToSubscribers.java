package org.example.all_users.admin.commands;

import lombok.RequiredArgsConstructor;
import org.example.data_classes.data_base.entity.Event;
import org.example.data_classes.enums.UserState;
import org.example.data_classes.data_base.entity.Usr;
import org.example.repository.EventRepository;
import org.example.repository.UserRepository;
import org.example.util.state.StateManager;
import org.example.util.telegram.api.TelegramSender;
import org.example.util.telegram.helpers.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AdminMessageToSubscribers {

    private final Map<Long, Long> eventIdMap = new ConcurrentHashMap<>();

    private final UpdateUtil updateUtil;
    private final TelegramSender telegramSender;
    private final UserRepository userRepository;
    private final StateManager stateManager;
    private final AdminStart adminStart;
    private final EventRepository eventRepository;

    public void processCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = updateUtil.getChatId(update);
        String[] callbackTextArray = callbackData.split("_");

        switch (callbackTextArray[1]) {
            case "to-event-subscribers" -> handleMessageToSubscribersCommand(chatId, callbackData, update);
            case "accept-to-event-subscribers" -> acceptForwardMessages(chatId, callbackData, messageId, update);
            case "cancel-to-event-subscribers" -> cancelForwardMessages(chatId, messageId, update);
            default -> sendUnknownCallbackResponse(chatId);
        }

        telegramSender.answerCallbackQuerry(chatId, AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.getId())
                .text("""
                        Команда обработана.""")
                .showAlert(false)
                .build());
    }

    private void handleMessageToSubscribersCommand(Long chatId, String callbackText, Update update) {
        Long eventId = Long.parseLong(callbackText.split("_")[2]);
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (eventOptional.isPresent()) {
            ReplyKeyboardRemove replyKeyboardRemove = ReplyKeyboardRemove.builder()
                    .removeKeyboard(true)
                    .build();

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .replyMarkup(replyKeyboardRemove)
                    .text(String.format("""
                        Пришлите сюда сообщение, а мы отправим его всем пользователям, подписанным на мероприятие: *%s*""", eventOptional.get().getEventName()))
                    .build());

            eventIdMap.put(chatId, eventId);
            stateManager.setUserState(chatId, UserState.ACCEPTING_FORWARD_MESSAGE_TO_EVENT_SUBSCRIBERS);
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                                    Мероприятие не найдено.""")
                    .build());
            adminStart.handleStartState(update);
        }
    }

    private void sendUnknownCallbackResponse(Long chatId) {
        SendMessage unknownCallbackMessage = new SendMessage();
        unknownCallbackMessage.setChatId(chatId.toString());
        unknownCallbackMessage.setText("Неизвестная команда.");

        telegramSender.sendText(chatId, unknownCallbackMessage);
    }

    public void acceptingForwardMessageToEventSubscribers(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Integer forwardMessageId = update.getMessage().getMessageId();

        InlineKeyboardButton yesButton = InlineKeyboardButton.builder()
                .text("Да")
                .callbackData("message-to-subscribers_accept-to-event-subscribers_" + forwardMessageId)
                .build();

        InlineKeyboardButton noButton = InlineKeyboardButton.builder()
                .text("Нет")
                .callbackData("message-to-subscribers_cancel-to-event-subscribers")
                .build();

        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder()
                .clearKeyboard()
                .keyboardRow(List.of(yesButton, noButton))
                .build();

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Разослать это сообщение?""")
                .replyMarkup(inlineKeyboardMarkup)
                .build());
    }

    private void acceptForwardMessages(Long chatId, String callbackText, Integer botMessageId, Update update) {
        List<Usr> allUsers = userRepository.findAll();
        Long eventId = eventIdMap.get(chatId);
        Integer messageId = Integer.parseInt(callbackText.split("_")[2]);

        if (!allUsers.isEmpty()) {
            for (Usr user : allUsers) {
                if (user.getSubscribedEventIds().contains(eventId.toString())) {
                    telegramSender.forwardMessage(chatId, ForwardMessage.builder()
                            .fromChatId(chatId)
                            .chatId(user.getChatId())
                            .messageId(messageId)
                            .build());
                }
            }
        }

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Сообщения отправлены.""")
                .build());

        telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                .chatId(chatId)
                .messageId(botMessageId)
                .build());

        adminStart.handleStartState(update);
    }

    private void cancelForwardMessages(Long chatId, Integer botMessageId, Update update) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Рассылка отменена.""")
                .build());

        telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                .chatId(chatId)
                .messageId(botMessageId)
                .build());

        adminStart.handleStartState(update);
    }
}
