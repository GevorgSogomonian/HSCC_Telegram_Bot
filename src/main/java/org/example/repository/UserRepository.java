package org.example.repository;

import org.example.entity.Usr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Usr, Long> {
    Optional<Usr> findByChatId(Long chatId); // Поиск пользователя по идентификатору чата

    @Query(nativeQuery = true, value = """
            select seq.id
            from seq
            order by id desc
            limit 1;
            """)
    Long getUniqueNumber();

    @Modifying
    @Query(nativeQuery = true, value = """
            insert into seq () VALUES ();
            """)
    void insertUniqueNumber();

    @Query("SELECT u.chatId FROM Usr u")
    List<Long> getAllUsersChatId();

    @Query("SELECT u.subscribedEventIds FROM Usr u")
    List<String> getAllsubscribedEventIds();
}