package org.example.all_users.base_user.commands;

import lombok.RequiredArgsConstructor;
import org.example.data_classes.data_base.entity.Usr;
import org.example.data_classes.data_base.entity.UsrExtraInfo;
import org.example.data_classes.enums.UserState;
import org.example.repository.UserRepository;
import org.example.repository.UsrExtraInfoRepository;
import org.example.util.state.StateManager;
import org.example.util.state.TemporaryDataService;
import org.example.util.telegram.api.TelegramSender;
import org.example.util.telegram.helpers.ActionsChainUtil;
import org.example.util.telegram.helpers.CallbackUtil;
import org.example.util.telegram.helpers.UpdateUtil;
import org.example.util.validation.StringValidator;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BaseEditInfo {
    private final TemporaryDataService<Usr> usrTemporaryDataService = new TemporaryDataService<>();
    private final TemporaryDataService<UsrExtraInfo> usrExtraInfoTemporaryDataService = new TemporaryDataService<>();
    private final UpdateUtil updateUtil;
    private final UserRepository userRepository;
    private final TelegramSender telegramSender;
    private final ActionsChainUtil actionsChainUtil;
    private final StateManager stateManager;
    private final StringValidator stringValidator;
    private final UsrExtraInfoRepository usrExtraInfoRepository;
    private final BaseStart baseStart;
    private final CallbackUtil callbackUtil;

    public void processCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = updateUtil.getChatId(update);
        String[] callbackTextArray = callbackData.split("_");

        switch (callbackTextArray[1]) {
            case "offer-editing" -> handleInfoEditCommand(chatId, messageId);
            case "accept-editing" -> acceptProfileEditing(chatId, messageId);
            case "cancel-editing" -> cancelEditingPtofile(chatId, messageId);
//            case "duration" -> checkEventDuration(update);
            default -> callbackUtil.sendUnknownCallbackResponse(chatId);
        }

        callbackUtil.answerCallback(chatId, callbackQuery.getId());
    }

    public void handleInfoEditCommand(Long chatId, Integer oldMessageId) {
        Optional<Usr> userOptional = userRepository.findByChatId(chatId);

        if (userOptional.isPresent()) {
            InlineKeyboardButton yesButton = InlineKeyboardButton.builder()
                    .text("Да")
                    .callbackData("user-edit_accept-editing")
                    .build();

            InlineKeyboardButton noButton = InlineKeyboardButton.builder()
                    .text("Нет")
                    .callbackData("user-edit_cancel-editing")
                    .build();

            InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder()
                    .clearKeyboard()
                    .keyboardRow(List.of(yesButton, noButton))
                    .build();

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Начать редактирование прифиля?""")
                    .replyMarkup(inlineKeyboardMarkup)
                    .build());
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Пользователь не найден!""")
                    .build());

            telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(oldMessageId)
                    .build());
        }
    }

    private void acceptProfileEditing(Long chatId, Integer messageId) {
        Optional<Usr> userOptional = userRepository.findByChatId(chatId);

        if (userOptional.isPresent()) {
            Usr user = userOptional.get();
            usrTemporaryDataService.putTemporaryData(chatId, user);
            if (!user.getIsHSEStudent()) {
                UsrExtraInfo usrExtraInfo = usrExtraInfoRepository.findByChatId(chatId).get();
                usrExtraInfoTemporaryDataService.putTemporaryData(chatId, usrExtraInfo);
            }
            ReplyKeyboardRemove replyKeyboardRemove = ReplyKeyboardRemove.builder()
                    .removeKeyboard(true)
                    .build();
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Редактируем профиль""" )
                    .replyMarkup(replyKeyboardRemove)
                    .build());

            telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .build());

            offerEnterNewName(chatId);
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Профиль не найден!""")
                    .build());

            telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .build());
        }
    }

    private void cancelEditingPtofile(Long chatId, Integer messageId) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Редактирование профиля отменено.""")
                .build());

        telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build());
    }

    private void offerEnterNewName(Long chatId) {
        actionsChainUtil.offerNextAction(chatId, """
                Хотите изменить *имя*?""", UserState.ACCEPTING_EDITING_PROFILE_FIRST_NAME);
    }

    public void acceptingEditingFirstName(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Usr usr = usrTemporaryDataService.getTemporaryData(chatId);
        Boolean answer = actionsChainUtil.checkAnswer(update);

        if (answer == null) {
            return;
        }

        if (answer) {
            requestNewProfileName(chatId, usr);
        } else {
            offerEnterNewLastName(chatId);
        }
    }

    private void requestNewProfileName(Long chatId, Usr user) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Текущее имя:""")
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text(String.format("""
                        *%s*""", user.getFirstName()))
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Введите новое имя:""")
                .build());

        stateManager.setUserState(chatId, UserState.EDITING_PROFILE_FIRST_NAME);
    }

    public void checkEditedFirstName(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Usr user = usrTemporaryDataService.getTemporaryData(chatId);
        String validatedFirstName = stringValidator.validateAndFormatFirstName(chatId, update.getMessage().getText());

        if (!validatedFirstName.isEmpty()) {
            user.setFirstName(validatedFirstName);
            usrTemporaryDataService.putTemporaryData(chatId, user);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Отлично! Новое имя сохранено!""")
                    .build());

            offerEnterNewLastName(chatId);
        }
    }

    private void offerEnterNewLastName(Long chatId) {
        actionsChainUtil.offerNextAction(chatId, """
                Хотите изменить *фамалию*?""", UserState.ACCEPTING_EDITING_PROFILE_LAST_NAME);
    }

    public void acceptingEditingLastName(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Usr usr = usrTemporaryDataService.getTemporaryData(chatId);
        Boolean answer = actionsChainUtil.checkAnswer(update);

        if (answer == null) {
            return;
        }

        if (answer) {
            requestNewLastName(chatId, usr);
        } else {
            offerEnterNewMiddleName(chatId);
        }
    }

    private void requestNewLastName(Long chatId, Usr user) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Текущая фамилия:""")
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text(String.format("""
                        *%s*""", user.getLastName()))
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Введите новую фамилию:""")
                .build());

        stateManager.setUserState(chatId, UserState.EDITING_PROFILE_LAST_NAME);
    }

    public void checkEditedLastName(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Usr user = usrTemporaryDataService.getTemporaryData(chatId);
        String validatedLastName = stringValidator.validateAndFormatLastName(chatId, update.getMessage().getText());

        if (!validatedLastName.isEmpty()) {
            user.setLastName(validatedLastName);
            usrTemporaryDataService.putTemporaryData(chatId, user);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Отлично! Новая фамилия сохранена!""")
                    .build());

            if (user.getIsHSEStudent()) {
                offerSaveEditedProfile(chatId);
            } else {
                offerEnterNewMiddleName(chatId);
            }
        }
    }

    private void offerEnterNewMiddleName(Long chatId) {
        actionsChainUtil.offerNextAction(chatId, """
                Хотите изменить *отчество*?""", UserState.ACCEPTING_EDITING_PROFILE_MIDDLE_NAME);
    }

    public void acceptingEditingMiddleName(Update update) {
        Long chatId = updateUtil.getChatId(update);
        UsrExtraInfo usrExtraInfo = usrExtraInfoTemporaryDataService.getTemporaryData(chatId);
        Boolean answer = actionsChainUtil.checkAnswer(update);

        if (answer == null) {
            return;
        }

        if (answer) {
            requestNewMiddleName(chatId, usrExtraInfo);
        } else {
            offerEnterNewEmail(chatId);
        }
    }

    private void requestNewMiddleName(Long chatId, UsrExtraInfo usrExtraInfo) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Текущее отчество:""")
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text(String.format("""
                        *%s*""", usrExtraInfo.getMiddleName()))
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Введите новое отчество:""")
                .build());

        stateManager.setUserState(chatId, UserState.EDITING_PROFILE_MIDDLE_NAME);
    }

    public void checkEditedMiddleName(Update update) {
        Long chatId = updateUtil.getChatId(update);
        UsrExtraInfo usrExtraInfo = usrExtraInfoTemporaryDataService.getTemporaryData(chatId);
        String validatedMiddleName = stringValidator.validateAndFormatMiddleName(chatId, update.getMessage().getText());

        if (!validatedMiddleName.isEmpty()) {
            usrExtraInfo.setMiddleName(validatedMiddleName);
            usrExtraInfoTemporaryDataService.putTemporaryData(chatId, usrExtraInfo);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Отлично! Новое отчество сохранено!""")
                    .build());

            offerEnterNewEmail(chatId);
        }
    }

    private void offerEnterNewEmail(Long chatId) {
        actionsChainUtil.offerNextAction(chatId, """
                Хотите изменить *почту*?""", UserState.ACCEPTING_EDITING_PROFILE_EMAIL);
    }

    public void acceptingEditingEmail(Update update) {
        Long chatId = updateUtil.getChatId(update);
        UsrExtraInfo usrExtraInfo = usrExtraInfoTemporaryDataService.getTemporaryData(chatId);
        Boolean answer = actionsChainUtil.checkAnswer(update);

        if (answer == null) {
            return;
        }

        if (answer) {
            requestNewEmail(chatId, usrExtraInfo);
        } else {
            offerEnterNewPhoneNumber(chatId);
        }
    }

    private void requestNewEmail(Long chatId, UsrExtraInfo usrExtraInfo) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Текущая почта:""")
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text(String.format("""
                        *%s*""", usrExtraInfo.getEmail()))
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Введите новую почту:""")
                .build());

        stateManager.setUserState(chatId, UserState.EDITING_PROFILE_EMAIL);
    }

    public void checkEditedEmail(Update update) {
        Long chatId = updateUtil.getChatId(update);
        UsrExtraInfo usrExtraInfo = usrExtraInfoTemporaryDataService.getTemporaryData(chatId);
        String email = update.getMessage().getText();

        if (StringValidator.isValidEmail(email)) {
            usrExtraInfo.setEmail(email);
            usrExtraInfoTemporaryDataService.putTemporaryData(chatId, usrExtraInfo);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Отлично! Новая почта сохранена!""")
                    .build());

            offerEnterNewPhoneNumber(chatId);
        }
    }

    private void offerEnterNewPhoneNumber(Long chatId) {
        actionsChainUtil.offerNextAction(chatId, """
                Хотите изменить *номер телефона*?""", UserState.ACCEPTING_EDITING_PROFILE_PHONE_NUMBER);
    }

    public void acceptingEditingPhoneNumber(Update update) {
        Long chatId = updateUtil.getChatId(update);
        UsrExtraInfo usrExtraInfo = usrExtraInfoTemporaryDataService.getTemporaryData(chatId);
        Boolean answer = actionsChainUtil.checkAnswer(update);

        if (answer == null) {
            return;
        }

        if (answer) {
            requestNewPhoneNumber(chatId, usrExtraInfo);
        } else {
            offerSaveEditedProfile(chatId);
        }
    }

    private void requestNewPhoneNumber(Long chatId, UsrExtraInfo usrExtraInfo) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Текущий номер телефона:""")
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text(String.format("""
                        *%s*""", usrExtraInfo.getPhoneNumber()))
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Введите новый номер телефона:""")
                .build());

        stateManager.setUserState(chatId, UserState.EDITING_PROFILE_PHONE_NUMBER);
    }

    public void checkEditedPhoneNumber(Update update) {
        Long chatId = updateUtil.getChatId(update);
        UsrExtraInfo usrExtraInfo = usrExtraInfoTemporaryDataService.getTemporaryData(chatId);
        String phoneNumberl = update.getMessage().getText();

        if (StringValidator.isValidPhoneNumber(phoneNumberl)) {
            usrExtraInfo.setPhoneNumber(phoneNumberl);
            usrExtraInfoTemporaryDataService.putTemporaryData(chatId, usrExtraInfo);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Отлично! Новый номер телефона сохранен!""")
                    .build());

            offerSaveEditedProfile(chatId);
        }
    }

    private void offerSaveEditedProfile(Long chatId) {
        actionsChainUtil.offerNextAction(chatId, """
                *Сохранить* изменения?""", UserState.ACCEPTING_SAVE_EDITED_PROFILE);
    }

    public void acceptingSavingEditedEvent(Update update) {
        Boolean answer = actionsChainUtil.checkAnswer(update);

        if (answer == null) {
            return;
        }

        if (answer) {
            acceptSavingEditedProfile(update);
        } else {
            cancelSavingEditedProfile(update);
        }
    }

    private void acceptSavingEditedProfile(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Usr usr = usrTemporaryDataService.getTemporaryData(chatId);
        UsrExtraInfo usrExtraInfo = usrExtraInfoTemporaryDataService.getTemporaryData(chatId);

        userRepository.save(usr);
        usrTemporaryDataService.removeTemporaryData(chatId);

        if (usrExtraInfo != null) {
            usrExtraInfoRepository.save(usrExtraInfo);
            usrExtraInfoTemporaryDataService.removeTemporaryData(chatId);
        }

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Профиль обновлён.""")
                .build());
        baseStart.handleStartState(update);
    }

    private void cancelSavingEditedProfile(Update update) {
        Long chatId = updateUtil.getChatId(update);
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Отредактированные данные не сохранены.""")
                .build());
        stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
        baseStart.handleStartState(update);
    }
}
