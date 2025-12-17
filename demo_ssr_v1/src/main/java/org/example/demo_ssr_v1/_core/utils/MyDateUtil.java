package org.example.demo_ssr_v1._core.utils;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

public class MyDateUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String format(Timestamp timestamp) {
        if (timestamp == null) {
            throw new RuntimeException("timestamp는 비어있을 수 없습니다.");
        }

         return timestamp.toLocalDateTime().format(FORMATTER);
    }
}
