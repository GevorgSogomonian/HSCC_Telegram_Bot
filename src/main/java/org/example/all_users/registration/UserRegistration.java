package org.example.all_users.registration;

import lombok.RequiredArgsConstructor;
import org.example.base_user.BaseUserService;
import org.example.dto.ChatBotResponse;
import org.example.entity.Role;
import org.example.entity.UserState;
import org.example.entity.Usr;
import org.example.repository.AdminRepository;
import org.example.repository.UserRepository;
import org.example.state_manager.StateManager;
import org.example.util.TemporaryDataService;
import org.example.util.UserUtilService;
import org.example.telegram.api.TelegramApiQueue;
import org.example.telegram.api.TelegramSender;
import org.example.util.StringValidator;
import org.example.util.UpdateUtil;
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

    public void startRegistration(Update update) {
        Long chatId = updateUtil.getChatId(update);
        telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                                Пожалуйста, введите ваше настоящее имя и фамилию.
                                Иначе вы не сможете получать бонусы за посещения наших мероприятий.""")
                .build()));

        Usr newUser = userUtilService.getNewUser(update, Role.USER);
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

        if (!formattedFirstName.isEmpty()) {
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

        if (!formattedLastName.isEmpty()) {
            Usr user = temporaryUserService.getTemporaryData(chatId);
            user.setLastName(lastName);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                Ваша фамилия сохранена!""")
                    .build());

            requestStudyPlace(chatId);
//            saveNewUser(chatId, user);
//            baseUserService.onUpdateRecieved(update);
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
//        Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);
        Usr user = temporaryUserService.getTemporaryData(chatId);

        if (userMessage.equals("да")) {
            user.setIsHSEStudent(true);
//            requestNewEventLocation(chatId, editedEvent);
        } else if (userMessage.equals("нет")) {
            user.setIsHSEStudent(false);
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Введите 'да' или 'нет'""")
                    .build());
        }

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                                Ваш выбор сохранён!""")
                .build());

        saveNewUser(chatId, user);
        baseUserService.onUpdateRecieved(update);

//        String lastName = update.getMessage().getText();
//        Long chatId = updateUtil.getChatId(update);
//
//        String formattedLastName = stringValidator.validateAndFormatLastName(chatId, lastName);
//
//        if (!formattedLastName.isEmpty()) {
//            Usr user = temporaryUserService.getTemporaryData(chatId);
//            user.setLastName(lastName);
//
//            telegramSender.sendText(chatId, SendMessage.builder()
//                    .chatId(chatId)
//                    .text("""
//                                Ваша фамилия сохранена!""")
//                    .build());
//
//            saveNewUser(chatId, user);
//            baseUserService.onUpdateRecieved(update);
//        }
    }

    private void saveNewUser(Long chatId, Usr user) {
        userRepository.save(user);
        stateManager.removeUserState(chatId);

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                                Поздравляю, вы зарегистрированы!""")
                .build());
    }
}
