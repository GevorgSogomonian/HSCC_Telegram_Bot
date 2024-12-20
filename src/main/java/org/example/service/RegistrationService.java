//package org.example.service;
//
//import org.example.entity.BotState;
//import org.springframework.stereotype.Service;
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
//import org.telegram.telegrambots.meta.api.objects.Update;
//
//@Service
//public class RegistrationService {
//    public void onUpdateRecieved(Update update) {
//
//    }
//
//    private SendMessage processMessage(Update update, BotState state) {
//        Long chatId = update.getMessage().getChatId();
//        String userMessage = update.getMessage().getText();
//
//        SendMessage message = new SendMessage();
//        message.setChatId(chatId.toString());
//
//        switch (state) {
//            case START:
//                handleStartState(update);
//                break;
//
//            case REGISTRATION:
//                startRegisterNewUser(update);
//                break;
//
//            case CHOOSING_ROLE:
//                roleChooser(update);
//                break;
//
//            case ENTERING_SPECIAL_KEY:
//                adminPasswordCheck(update);
//                break;
//
//            default:
//                processTextMessage(update);
//                break;
////                message.setText("Что-то пошло не так. Попробуй начать заново с команды /start.");
////                stateManager.removeUserState(chatId);
//        }
//
//        return message;
//    }
//}
