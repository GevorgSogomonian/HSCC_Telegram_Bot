package org.example.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class MySQLInfo {

    @PersistenceContext
    private EntityManager entityManager;

    public LocalDateTime getCurrentTimeStamp() {
        Timestamp timestamp = (Timestamp) entityManager.createNativeQuery("SELECT CURRENT_TIMESTAMP").getSingleResult();
        return timestamp.toLocalDateTime();
    }
}
