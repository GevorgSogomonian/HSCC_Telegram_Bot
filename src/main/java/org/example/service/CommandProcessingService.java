package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.entity.Role;
import org.example.entity.Usr;
import org.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
@RequiredArgsConstructor
@Transactional
public class CommandProcessingService {
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(CommandProcessingService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String truncateDescription(String description) {
        int maxLength = 2000;
        if (description != null && description.length() > maxLength) {
            return description.substring(0, maxLength) + "...";
        }
        return description != null ? description : "Описание недоступно.";
    }

//    @Scheduled(cron = "0 0 0 * * *")
//    public void cachePopularMovies() {
//        for (int i = 1; i <= 30; i++) {
//            Map<String, Object> response = tmdbService.fetchMoviesFromAllPages(i);
//
//            if (response != null && response.containsKey("results")) {
//                ((List<Map<String, Object>>) response.get("results")).forEach(this::saveOrUpdateMovie);
//            }
//        }
//    }

    public void saveNewUser(Update update, Role role) {
        Long chatId = update.getMessage().getChatId();
        org.telegram.telegrambots.meta.api.objects.User fromUser = update.getMessage().getFrom();

        Usr newUsr = new Usr();
        newUsr.setChatId(chatId);
        newUsr.setUsername(fromUser.getUserName());
        newUsr.setFirstName(fromUser.getFirstName());
        newUsr.setLastName(fromUser.getLastName());
        newUsr.setRole(role);
        newUsr.setLanguageCode(fromUser.getLanguageCode());
        newUsr.setIsPremium(fromUser.getIsPremium());
        newUsr.setIsBot(fromUser.getIsBot());

        userRepository.save(newUsr);
    }


//    @PostConstruct
//    public void init() {
//        cachePopularMovies();
//        logger.info("Популярные фильмы успешно загружены");
//    }
}
