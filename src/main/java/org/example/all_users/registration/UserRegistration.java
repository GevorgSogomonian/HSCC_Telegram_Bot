package org.example.all_users.registration;

import lombok.RequiredArgsConstructor;
import org.example.all_users.base_user.BaseUserService;
import org.example.data_classes.data_base.entity.UsrExtraInfo;
import org.example.data_classes.dto.ChatBotResponse;
import org.example.data_classes.enums.UserState;
import org.example.data_classes.data_base.entity.Usr;
import org.example.repository.UserRepository;
import org.example.repository.UsrExtraInfoRepository;
import org.example.util.state.StateManager;
import org.example.util.state.TemporaryDataService;
import org.example.util.UserUtilService;
import org.example.util.telegram.api.TelegramApiQueue;
import org.example.util.telegram.api.TelegramSender;
import org.example.util.validation.StringValidator;
import org.example.util.telegram.helpers.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRegistration {

    private final UpdateUtil updateUtil;
    private final TelegramApiQueue telegramApiQueue;
    private final StateManager stateManager;
    private final UserUtilService userUtilService;
    private final TelegramSender telegramSender;
    private final StringValidator stringValidator;
    private final UserRepository userRepository;
    private final BaseUserService baseUserService;
    private final TemporaryDataService<Usr> temporaryUserService;
    private final TemporaryDataService<UsrExtraInfo> usrExtraInfoTemporaryDataService = new TemporaryDataService<>();
    private final UsrExtraInfoRepository usrExtraInfoRepository;

    public void startRegistration(Update update) {
        Long chatId = updateUtil.getChatId(update);
        telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                                Пожалуйста, введите ваше настоящее имя и фамилию.
                                Иначе вы не сможете получать бонусы за посещения наших мероприятий.""")
                .build()));

        Usr newUser = userUtilService.getNewUser(update);

        temporaryUserService.putTemporaryData(chatId, newUser);

        requestFirstName(chatId);
    }

    private void requestFirstName(Long chatId) {
        telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Введите ваше имя:""")
                .build()));

        stateManager.setUserState(chatId, UserState.ENTERING_FIRSTNAME);
    }

    public void firstNameCheck(Update update) {
        String firstName = update.getMessage().getText();
        Long chatId = updateUtil.getChatId(update);

        String formattedFirstName = stringValidator.validateAndFormatFirstName(chatId, firstName);

        if (formattedFirstName != null) {
            Usr user = temporaryUserService.getTemporaryData(chatId);
            user.setFirstName(firstName);
            if (updateUtil.getAdmin(update).isPresent()) {
                user.setIsAdminClone(true);
            }
            temporaryUserService.putTemporaryData(chatId, user);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Ваше имя сохранено!""")
                    .build());

            requestLastName(chatId);
        }
    }

    private void requestLastName(Long chatId) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                                Введите вашу фамилию:""")
                .build());

        stateManager.setUserState(chatId, UserState.ENTERING_LASTNAME);
    }

    public void lastNameCheck(Update update) {
        String lastName = update.getMessage().getText();
        Long chatId = updateUtil.getChatId(update);

        String formattedLastName = stringValidator.validateAndFormatLastName(chatId, lastName);

        if (formattedLastName != null) {
            Usr user = temporaryUserService.getTemporaryData(chatId);
            user.setLastName(lastName);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                Ваша фамилия сохранена!""")
                    .build());

            requestStudyPlace(chatId);
        }
    }

    private void requestStudyPlace(Long chatId) {
        KeyboardButton yesButton = new KeyboardButton("Да");
        KeyboardButton noButton = new KeyboardButton("Нет");

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .clearKeyboard()
                .keyboard(List.of(new KeyboardRow(List.of(yesButton, noButton))))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .replyMarkup(keyboardMarkup)
                        .text("""
                                Вы учитесь в ВШЭ?
                                
                                Большинство мероприятий проходят на территории ВШЭ. Если вы не являетесь студентом ВШЭ, нам нужно будет сделать на вас проходку.""")
                .build());

        stateManager.setUserState(chatId, UserState.ENTERING_STUDY_PLACE);
    }

    public void studyPlaceCheck(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText().toLowerCase();
        Usr user = temporaryUserService.getTemporaryData(chatId);

        if (!userMessage.equals("да") && !userMessage.equals("нет")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Введите 'да' или 'нет'""")
                    .build());
        } else {
            if (userMessage.equals("да")) {
                user.setIsHSEStudent(true);
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Ваш выбор сохранён!""")
                        .build());

                saveNewUser(chatId, user);
                baseUserService.onUpdateRecieved(update);
            } else if (userMessage.equals("нет")) {
                user.setIsHSEStudent(false);
                temporaryUserService.putTemporaryData(chatId, user);
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Ваш выбор сохранён!""")
                        .build());

                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Вы не студент HSE, поэтому вам нужно будет заполнить ешё несколько пунктов. Эта информация нужна для оформления проходок! Указывайте корректные данные!""")
                        .build());

                requestMiddleName(chatId);
            }
        }
    }

    private void saveNewUser(Long chatId, Usr user) {
        userRepository.save(user);

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Поздравляю, вы зарегистрированы!""")
                .build());

        stateManager.removeUserState(chatId);
    }

    private void requestMiddleName(Long chatId) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Введите ваше отчество:""")
                .build());

        stateManager.setUserState(chatId, UserState.ENTERING_MIDDLE_NAME);
    }

    public void middleNameCheck(Update update) {
        String middleName = update.getMessage().getText();
        Long chatId = updateUtil.getChatId(update);

        String formattedMiddleName = stringValidator.validateAndFormatMiddleName(chatId, middleName);

        if (formattedMiddleName != null) {
//            Usr user = temporaryUserService.getTemporaryData(chatId);
            UsrExtraInfo usrExtraInfo = new UsrExtraInfo();
            usrExtraInfo.setChatId(chatId);
//            usrExtraInfo.setFirstName(user.getFirstName());
//            usrExtraInfo.setLastName(user.getLastName());
            usrExtraInfo.setMiddleName(formattedMiddleName);

            usrExtraInfoTemporaryDataService.putTemporaryData(chatId, usrExtraInfo);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Ваше отчество сохранено!""")
                    .build());

            requestEmail(chatId);
        }
    }

    private void requestEmail(Long chatId) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Введите ваш адрес электронной почты:""")
                .build());

        stateManager.setUserState(chatId, UserState.ENTERING_EMAIL);
    }

    public void emailCheck(Update update) {
        String email = update.getMessage().getText();
        Long chatId = updateUtil.getChatId(update);

        if (StringValidator.isValidEmail(email)) {
            UsrExtraInfo usrExtraInfo = usrExtraInfoTemporaryDataService.getTemporaryData(chatId);
            usrExtraInfo.setEmail(email);

            usrExtraInfoTemporaryDataService.putTemporaryData(chatId, usrExtraInfo);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Ваша почта сохранена!""")
                    .build());

            requestPhoneNumber(chatId);
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Некорректный формат данных!""")
                    .build());
        }
    }

    private void requestPhoneNumber(Long chatId) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Введите ваш номер телефона:""")
                .build());

        stateManager.setUserState(chatId, UserState.ENTERING_PHONE_NUMBER);
    }

    public void phoneNumberCheck(Update update) {
        String phoneNumber = update.getMessage().getText();
        Long chatId = updateUtil.getChatId(update);

        if (StringValidator.isValidPhoneNumber(phoneNumber)) {
            UsrExtraInfo usrExtraInfo = usrExtraInfoTemporaryDataService.getTemporaryData(chatId);
            Usr user = temporaryUserService.getTemporaryData(chatId);
            usrExtraInfo.setPhoneNumber(phoneNumber);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Ваш номер телефона сохранен!""")
                    .build());

            saveNewUserAndExtraInfo(chatId, user, usrExtraInfo);
            baseUserService.onUpdateRecieved(update);
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Некорректный формат данных!""")
                    .build());
        }
    }

    private void saveNewUserAndExtraInfo(Long chatId, Usr user, UsrExtraInfo usrExtraInfo) {
        userRepository.save(user);
        usrExtraInfoRepository.save(usrExtraInfo);

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Поздравляю, вы зарегистрированы!""")
                .build());

        stateManager.removeUserState(chatId);
    }
}
