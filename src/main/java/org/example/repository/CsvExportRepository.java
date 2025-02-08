package org.example.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CsvExportRepository {

    private final JdbcTemplate jdbcTemplate;

    public byte[] archivedEventCSVStatistic(Long eventId) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8))) {
            writer.println("chatId,come,first_name,last_name,middle_name,email,phone_number,is_HSE_student");

            List<String[]> data = jdbcTemplate.query("""
                        SELECT tbl.chat_id, tbl.come, u.first_name, u.last_name, ue.middle_name, ue.email, ue.phone_number, u.ishsestudent
                        FROM (
                            SELECT ev.chat_id, ev.event_id, 1 AS come
                            FROM event_visit ev
                            where event_id = ?
                            UNION
                            SELECT em.chat_id, em.event_id, 0 AS come
                            FROM event_missing em
                            where event_id = ?
                        ) tbl
                            inner join usr u
                                on tbl.chat_id = u.chat_id
                            inner join usr_extra_info ue
                                on tbl.chat_id = ue.chat_id
                        """,
                    new Object[]{eventId, eventId},
                    (rs, rowNum) -> new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)}
            );

            for (String[] row : data) {
                writer.println(String.join(",", row));
            }
            writer.flush();
        }
        return byteArrayOutputStream.toByteArray();
    }

    public byte[] actualEventCSVStatistic(Long eventId) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8))) {
            writer.println("chatId,is_HSE_student,first_name,last_name,middle_name,email,phone_number");

            List<String[]> data = jdbcTemplate.query("""
                        SELECT es.chat_id, u.ishsestudent, u.first_name, u.last_name, ue.middle_name, ue.email, ue.phone_number
                        FROM event_subscription es
                            inner join usr u
                                on es.chat_id = u.chat_id
                            inner join usr_extra_info ue
                                on es.chat_id = ue.chat_id
                        where es.event_id = ?
                        """,
                    new Object[]{eventId},
                    (rs, rowNum) -> new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)}
            );

            for (String[] row : data) {
                writer.println(String.join(",", row));
            }
            writer.flush();
        }
        return byteArrayOutputStream.toByteArray();
    }
}
