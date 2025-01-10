package org.example.repository;

import org.example.entity.Usr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Usr, Long> {
    Optional<Usr> findByChatId(Long chatId); // Поиск пользователя по идентификатору чата
}