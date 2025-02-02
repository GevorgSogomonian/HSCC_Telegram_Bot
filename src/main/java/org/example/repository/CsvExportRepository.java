package org.example.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@Repository
@RequiredArgsConstructor
public class CsvExportRepository {

    private final JdbcTemplate jdbcTemplate;

    public byte[] archivedEventCSVStatistic(Long eventId) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8))) {
            writer.println("id,first_name,last_name"); // Заголовки CSV

//            List<String[]> data = jdbcTemplate.query("""
//                        SELECT id, first_name, last_name,
//                        FROM (
//                            select *
//                            from usr
//                            where
//                        )
//                        """,
//                    new Object[]{userId},  // Подставляем параметр
//                    (rs, rowNum) -> new String[]{rs.getString(1), rs.getString(2), rs.getString(3)}
//            );

//            for (String[] row : data) {
//                writer.println(String.join(",", row));
//            }
            writer.flush();
        }
        return byteArrayOutputStream.toByteArray(); // Возвращаем CSV в виде массива байтов
    }

    public byte[] actualEventCSVStatistic(Long eventId) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8))) {
            writer.println("id,first_name,last_name"); // Заголовки CSV

//            List<String[]> data = jdbcTemplate.query("""
//                        SELECT id, first_name, last_name,
//                        FROM (
//                            select *
//                            from usr
//                            where
//                        )
//                        """,
//                    new Object[]{userId},  // Подставляем параметр
//                    (rs, rowNum) -> new String[]{rs.getString(1), rs.getString(2), rs.getString(3)}
//            );

//            for (String[] row : data) {
//                writer.println(String.join(",", row));
//            }
            writer.flush();
        }
        return byteArrayOutputStream.toByteArray(); // Возвращаем CSV в виде массива байтов
    }
}
