package org.example.repository;

import org.example.data_classes.data_base.entity.Usr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Usr, Long> {
    Optional<Usr> findByChatId(Long chatId);

    Optional<Usr> findByUserId(Long userId);

    @Query(nativeQuery = true, value = """
            select u.number
            from unique_number_seq u
            order by number desc
            limit 1;
            """)
    Long getUniqueNumber();

    @Modifying
    @Query(nativeQuery = true, value = """
            insert into unique_number_seq () VALUES ();
            """)
    void insertUniqueNumber();

    @Query("SELECT u.chatId FROM Usr u")
    List<Long> getAllUsersChatId();
}