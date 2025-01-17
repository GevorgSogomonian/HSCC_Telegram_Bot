package org.example.repository;

import org.example.entity.Admin;
import org.example.entity.Usr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByChatId(Long chatId);
}