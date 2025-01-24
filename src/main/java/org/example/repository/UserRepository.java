package org.example.repository;

import org.example.entity.Usr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Usr, Long> {
    Optional<Usr> findByChatId(Long chatId); // Поиск пользователя по идентификатору чата

    @Query(nativeQuery = true, value = """
            SELECT nextval('unique_numbers_for_user_id');
            """)
    Long getUniqueNumber();

    @Query("SELECT u.chatId FROM Usr u")
    List<Long> getAllUsersChatId();

    @Query("SELECT u.subscribedEventIds FROM Usr u")
    List<String> getAllsubscribedEventIds();
}