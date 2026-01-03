package org.example.demo_ssr_v1._core.utils;

import java.util.Random;

public class MailUtils {
    public static String generateRandomCode() {
        Random random = new Random();

        // 하나의 랜덤 숫자 생성
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
