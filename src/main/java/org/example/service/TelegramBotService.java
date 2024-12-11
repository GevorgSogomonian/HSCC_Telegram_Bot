package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.entity.Movie;
import org.example.entity.UserMovieRating;
import org.example.entity.Usr;
import org.example.repository.MovieRepository;
import org.example.repository.UserMovieRatingRepository;
import org.example.repository.UsrRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class TelegramBotService extends TelegramLongPollingBot {

    private final Map<String, Movie> activeRatings = new ConcurrentHashMap<>();
    private final UserMovieRatingRepository userMovieRatingRepository;
    private final CommandProcessingService commandProcessingService;
    private final UsrRepository usrRepository;
    private final MovieRepository movieRepository;
    private final TmdbService tmdbService;

    @Value("${spring.telegram.bot.username}")
    private String botUsername;

    @Value("${spring.telegram.bot.token}")
    private String botToken;

    private final Map<String, Consumer<Update>> commandHandlers = new HashMap<>();

    @PostConstruct
    public void init() {
        System.out.println("Username: " + botUsername);
        System.out.println("Token: " + botToken);

        commandHandlers.put("/search", this::handleSearchCommand);
        commandHandlers.put("/popular", this::handlePopularCommand);
        commandHandlers.put("/random", this::handleRandomCommand);
        commandHandlers.put("/mostpersonal", this::handleMostPersonalCommand);
        commandHandlers.put("/ratepopular", this::handleRatePopularCommand);
        commandHandlers.put("/personal", this::handlePersonalCommand);
        commandHandlers.put("/rateall", this::handleRateAllCommand);
        commandHandlers.put("/allrated", this::handleAllRatedCommand);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String userMessage = update.getMessage().getText();

            // Проверяем, существует ли пользователь в базе
            usrRepository.findByChatId(chatId).ifPresentOrElse(
                    usr -> System.out.println("Пользователь уже зарегистрирован: " + usr.getUsername()),
                    () -> registerNewUser(update) // Регистрация нового пользователя
            );

            if (activeRatings.containsKey(chatId.toString())) {
                handleRatingResponse(update); // Обрабатываем дальнейшее взаимодействие
                return;
            }

            // Обработка команды (если это команда)
            String command = userMessage.split(" ")[0].toLowerCase();
            commandHandlers.getOrDefault(command, this::handleUnknownCommand).accept(update);
        }
    }

    private void handleSearchCommand(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        String query = update.getMessage().getText().replace("/search ", "");
        String result = commandProcessingService.searchMovie(query);

        sendSplitResponse(chatId, result);
    }

    private void handlePopularCommand(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        String result = commandProcessingService.getPopularMoviesRandom();

        if (!result.isEmpty()) {
            sendSplitResponse(chatId, "🌟 *Популярные фильмы прямо сейчас*:\n\n" + result);
        } else {
            sendResponse(chatId, "😔 *Не удалось получить список популярных фильмов.* Попробуйте позже.");
        }
    }

    private void handleRandomCommand(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        String result = commandProcessingService.getRandomMovie();

        sendSplitResponse(chatId, result);
    }

    private void handleRatePopularCommand(Update update) {
        String chatId = update.getMessage().getChatId().toString();

        // Получаем случайный популярный фильм
        Map<String, Object> randomMovieData = tmdbService.getRandomPopularMovie();
        Movie randomMovie = commandProcessingService.saveOrUpdateMovie(randomMovieData);

        // Сохраняем фильм для дальнейшей оценки
        activeRatings.put(chatId, randomMovie);

        // Формируем сообщение с описанием, жанрами и рейтингом
        String response = String.format(
                "🎥 *Мы предлагаем вам фильм:*\n" +
                        "🎬 *Название*: %s\n📖 *Описание*: %s\n🎭 *Жанры*: %s\n⭐ *Рейтинг*: %s\n\n" +
                        "❓ *Вы уже видели этот фильм?* Ответьте 'да' или 'нет'.",
                randomMovie.getTitle(),
                truncateDescription(randomMovie.getDescription()),
                tmdbService.getGenreNames(randomMovie.getGenreIds()), // Жанры
                randomMovie.getRating() != null ? randomMovie.getRating().toString() : "Нет рейтинга"
        );

        sendSplitResponse(chatId, response);
    }

    private void handlePersonalCommand(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        String result = commandProcessingService.getPersonalRecommendation(chatId);

        sendSplitResponse(chatId, "❤️ *Ваши персональные рекомендации*:\n\n" + result);
    }

    private void handleAllRatedCommand(Update update) {
        String chatId = update.getMessage().getChatId().toString();

        try {
            String ratedMovies = commandProcessingService.getAllRatedMovies(chatId);

            sendSplitResponse(chatId, "📋 *Ваши оценки фильмов:*\n\n" + ratedMovies);
        } catch (Exception e) {
            sendResponse(chatId, "❌ *Произошла ошибка при получении списка оцененных фильмов.* Попробуйте позже.");
            e.printStackTrace();
        }
    }

    private void handleMostPersonalCommand(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        String result = commandProcessingService.getMostPersonalRecommendation(chatId);
        sendSplitResponse(chatId, result);
    }

    private void saveUserRating(String chatId, int rating) {
        Long userChatId = Long.parseLong(chatId);
        Usr user = usrRepository.findByChatId(userChatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));
        Movie movie = activeRatings.get(chatId);

        if (movie == null) {
            sendResponse(chatId, "⚠️ *Фильм для оценки не найден.* Попробуйте команду /rate или /rateall.");
            return;
        }

        Optional<UserMovieRating> existingRating = userMovieRatingRepository.findByUserIdAndMovieId(user.getId(), movie.getId());

        if (existingRating.isPresent()) {
            UserMovieRating userMovieRating = existingRating.get();
            userMovieRating.setRating(rating);
            userMovieRatingRepository.save(userMovieRating);
            sendResponse(chatId, "✅ *Ваша оценка обновлена!* Вы поставили " + rating + " баллов. 🎉");
        } else {
            UserMovieRating userMovieRating = new UserMovieRating();
            userMovieRating.setUser(user);
            userMovieRating.setMovie(movie);
            userMovieRating.setRating(rating);
            userMovieRatingRepository.save(userMovieRating);
            sendResponse(chatId, "⭐ *Спасибо за вашу оценку!* Вы поставили " + rating + " баллов. 😊");
        }

        activeRatings.remove(chatId);
    }

    private void handleRatingResponse(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        String userResponse = update.getMessage().getText().toLowerCase();

        // Проверяем, есть ли активный фильм для пользователя
        Movie movie = activeRatings.get(chatId);
        if (movie == null) {
            sendResponse(chatId, "😕 *У вас нет активного фильма для оценки.*\n" +
                    "Попробуйте команды /rate или /rateall, чтобы начать!");
            return;
        }

        if (userResponse.equals("да")) {
            // Сохраняем фильм в базу (если не был сохранен ранее)
            movieRepository.save(movie);

            sendResponse(chatId, "🎬 Отлично! Как бы вы оценили этот фильм по шкале от 1 до 10? ⭐");
        } else if (userResponse.equals("нет")) {
            sendResponse(chatId, "🙅‍♂️ *Спасибо за ваш ответ!* Если хотите, попробуйте другой фильм. 🎲");
            activeRatings.remove(chatId); // Удаляем из активных рейтингов
        } else {
            try {
                int rating = Integer.parseInt(userResponse);
                if (rating >= 1 && rating <= 10) {
                    saveUserRating(chatId, rating);
                    sendResponse(chatId, String.format(
                            "🎭 *Делитесь вашими впечатлениями!*\n\nХотите продолжить? Попробуйте команду /rateall!",
                            rating
                    ));
                } else {
                    sendResponse(chatId, "⚠️ Пожалуйста, введите число от 1 до 10. ⭐");
                }
            } catch (NumberFormatException e) {
                sendResponse(chatId, "❓ *Неизвестный ответ.* Напишите 'да', 'нет' или число от 1 до 10. 🧐");
            }
        }
    }


    private void handleRateAllCommand(Update update) {
        String chatId = update.getMessage().getChatId().toString();

        try {
            Movie randomMovie = commandProcessingService.getRandomMovieForRating();
            activeRatings.put(chatId, randomMovie);

            String response = String.format(
                    "🎲 *Случайный фильм для оценки:*\n" +
                            "🎬 *Название*: %s\n📖 *Описание*: %s\n🎭 *Жанры*: %s\n⭐ *Рейтинг*: %s\n\n" +
                            "❓ *Вы уже видели этот фильм?* Ответьте 'да' или 'нет'.",
                    randomMovie.getTitle(),
                    truncateDescription(randomMovie.getDescription()),
                    tmdbService.getGenreNames(randomMovie.getGenreIds()), // Добавление жанров
                    randomMovie.getRating() != null ? randomMovie.getRating().toString() : "Нет рейтинга"
            );

            sendSplitResponse(chatId, response);
        } catch (Exception e) {
            sendResponse(chatId, "😞 *К сожалению, не удалось получить случайный фильм для оценки.* Попробуйте позже!");
            e.printStackTrace();
        }
    }

    private String truncateDescription(String description) {
        int maxLength = 500;
        if (description != null && description.length() > maxLength) {
            return description.substring(0, maxLength) + "...";
        }
        return description != null ? description : "Описание недоступно.";
    }

    private void handleUnknownCommand(Update update) {
        String chatId = update.getMessage().getChatId().toString();

        String response = """
        🐾 *Добро пожаловать в вашего личного помощника по фильмам!* 🎥✨
        
        _Вот список доступных команд, которые вы можете использовать:_
        
        🔍 `/search <название>` — Найти фильм по названию и получить информацию о нем. Например: `/search Inception`
        
        🌟 `/popular` — Получить список случайных популярных фильмов прямо сейчас.
        
        🎲 `/random` — Увидеть абсолютно случайный фильм из всех доступных в базе TMDb.
        
        ❤️ `/personal` — Получить рекомендации фильмов, которые максимально соответствуют вашим предпочтениям.
        
        🏆 `/mostpersonal` — Узнать самый подходящий вам фильм на основе ваших оценок.
        
        🎬 `/ratepopular` — Оцените случайный популярный фильм. Вы уже видели его? Расскажите нам!
        
        🌀 `/rateall` — Оцените абсолютно случайный фильм, не обязательно популярный.
        
        📜 `/allrated` — Посмотрите список всех фильмов, которые вы оценили, и их оценки.
        
        🛠️ _Пример использования:_ Просто введите нужную команду, например, `/random`, чтобы получить фильм!
        
        🧡 _Спасибо, что пользуетесь нашим ботом! Мы здесь, чтобы сделать ваш просмотр фильмов ещё более увлекательным._ 😊
        """;

        sendSplitResponse(chatId, response);
    }

    private void sendSplitResponse(String chatId, String text) {
        int maxMessageLength = 4096;
        for (int i = 0; i < text.length(); i += maxMessageLength) {
            String part = text.substring(i, Math.min(text.length(), i + maxMessageLength));
            sendResponse(chatId, part);
        }
    }

    private void sendResponse(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void registerNewUser(Update update) {
        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            Long chatId = update.getMessage().getChatId();
            org.telegram.telegrambots.meta.api.objects.User fromUser = update.getMessage().getFrom();

            // Создаем нового пользователя
            Usr newUser = new Usr();
            newUser.setChatId(chatId);
            newUser.setUsername(fromUser.getUserName());
            newUser.setFirstName(fromUser.getFirstName());
            newUser.setLastName(fromUser.getLastName());
            newUser.setLanguageCode(fromUser.getLanguageCode());
            newUser.setIsPremium(fromUser.getIsPremium());
            newUser.setIsBot(fromUser.getIsBot());

            // Сохраняем пользователя в базу
            usrRepository.save(newUser);

            // Отправляем приветственное сообщение
            sendResponse(chatId.toString(), "Добро пожаловать, " + newUser.getFirstName() + "! Вы успешно зарегистрированы.");
        }
    }
}