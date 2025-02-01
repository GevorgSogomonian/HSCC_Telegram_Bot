package org.example.util.state;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TemporaryDataService<T> {

    private final Map<Long, T> temporaryData = new ConcurrentHashMap<>();

    /**
     * Добавляет временные данные.
     *
     * @param chatId Идентификатор чата.
     * @param data   Временные данные для сохранения.
     */
    public void putTemporaryData(Long chatId, T data) {
        temporaryData.put(chatId, data);
    }

    /**
     * Получает временные данные по идентификатору чата.
     *
     * @param chatId Идентификатор чата.
     * @return Временные данные или {@code null}, если данных нет.
     */
    public T getTemporaryData(Long chatId) {
        return temporaryData.get(chatId);
    }

    /**
     * Удаляет временные данные по идентификатору чата.
     *
     * @param chatId Идентификатор чата.
     */
    public void removeTemporaryData(Long chatId) {
        temporaryData.remove(chatId);
    }
}
