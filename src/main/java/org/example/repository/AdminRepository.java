package org.example.repository;

import org.example.data_classes.data_base.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByChatId(Long chatId);
}