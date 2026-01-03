package org.example.demo_ssr_v1.payment;

import lombok.Data;
import org.example.demo_ssr_v1._core.errors.exception.Exception400;

public class PaymentRequest {
    // 결제 요청 생성 DTO
    // 비동기 통신의 body 값과 동일
    @Data
    public static class PrepareDTO {
        private Integer amount; // 충전할 포인트

        public void validate() {
            if(amount == null || amount < 0) {
                throw new Exception400("충전할 포인트는 0보다 커야합니다.");
            }

            if(amount < 100) {
                throw new Exception400("최소 충전 금액은 100포인트 입니다.");
            }

            if(amount > 100000) {
                throw new Exception400("최대 충전 금액을 100,000포인트 입니다.");
            }
        }
    }
}
