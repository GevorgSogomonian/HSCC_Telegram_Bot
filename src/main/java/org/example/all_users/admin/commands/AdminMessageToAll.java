package org.example.all_users.admin.commands;

import lombok.RequiredArgsConstructor;
import org.example.data_classes.enums.UserState;
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

@Service
@RequiredArgsConstructor
public class AdminMessageToAll {

    private final UpdateUtil updateUtil;
    private final TelegramSender telegramSender;
    private final UserRepository userRepository;
    private final StateManager stateManager;
    private final AdminStart adminStart;

    public void handleMessageToAllCommand(Update update) {
        Long chatId = updateUtil.getChatId(update);
        ReplyKeyboardRemove replyKeyboardRemove = ReplyKeyboardRemove.builder()
                .removeKeyboard(true)
                .build();

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .replyMarkup(replyKeyboardRemove)
                .text("""
                        Пришлите сюда сообщение, а мы отправим его всем зарегистрированным пользователям.""")
                .build());

        stateManager.setUserState(chatId, UserState.ACCEPTING_FORWARD_MESSAGE_TO_ALL);
    }

    public void processCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = updateUtil.getChatId(update);
        String[] callbackTextArray = callbackData.split("_");

        switch (callbackTextArray[1]) {
            case "accept-to-all" -> acceptForwardMessages(chatId, callbackData, messageId);
            case "cancel-to-all" -> cancelForwardMessages(chatId, messageId);
            default -> sendUnknownCallbackResponse(chatId);
        }

        telegramSender.answerCallbackQuerry(chatId, AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.getId())
                .text("""
                        Команда обработана.""")
                .showAlert(false)
                .build());

        adminStart.handleStartState(update);
    }

    private void sendUnknownCallbackResponse(Long chatId) {
        SendMessage unknownCallbackMessage = new SendMessage();
        unknownCallbackMessage.setChatId(chatId.toString());
        unknownCallbackMessage.setText("Неизвестная команда.");

        telegramSender.sendText(chatId, unknownCallbackMessage);
    }

    public void acceptingForwardMessagesToAll(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Integer forwardMessageId = update.getMessage().getMessageId();

        InlineKeyboardButton yesButton = InlineKeyboardButton.builder()
                .text("Да")
                .callbackData("message-to-all_accept-to-all_" + forwardMessageId)
                .build();

        InlineKeyboardButton noButton = InlineKeyboardButton.builder()
                .text("Нет")
                .callbackData("message-to-all_cancel-to-all")
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

    private void acceptForwardMessages(Long chatId, String callbackText, Integer botMessageId) {
        List<Long> allUsersChatId = userRepository.getAllUsersChatId();
        Integer messageId = Integer.parseInt(callbackText.split("_")[2]);

        if (!allUsersChatId.isEmpty()) {
            for (Long forwardChatId : allUsersChatId) {
                telegramSender.forwardMessage(chatId, ForwardMessage.builder()
                        .fromChatId(chatId)
                        .chatId(forwardChatId)
                        .messageId(messageId)
                        .build());
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
    }

    private void cancelForwardMessages(Long chatId, Integer botMessageId) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Рассылка отменена.""")
                .build());

        telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                .chatId(chatId)
                .messageId(botMessageId)
                .build());
    }
}
