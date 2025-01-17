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
    private final AdminRepository adminRepository;

    public void startRegistration(Update update) {
        Long chatId = updateUtil.getChatId(update);
        telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                                Пожалуйста, введите ваше настоящее имя и фамилию.
                                Иначе вы не сможете получать бонусы за посещения наших мероприятий.""")
                .build()));

        telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                                Введите ваше имя:""")
                .build()));

        Usr newUser = userUtilService.getNewUser(update, Role.USER);
        temporaryUserService.putTemporaryData(chatId, newUser);
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

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                Введите вашу фамилию:""")
                    .build());
            stateManager.setUserState(chatId, UserState.ENTERING_LASTNAME);
        }
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

            userRepository.save(user);
            stateManager.removeUserState(chatId);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                Поздравляю, вы зарегистрированы!""")
                    .build());
            baseUserService.onUpdateRecieved(update);
        }
    }
}
