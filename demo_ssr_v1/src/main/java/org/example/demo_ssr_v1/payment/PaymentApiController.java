package org.example.demo_ssr_v1.payment;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.demo_ssr_v1.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PaymentApiController {
    private final PaymentService paymentService;

    @PostMapping("/api/payment/prepare")
    public ResponseEntity<?> preparePayment(@RequestBody PaymentRequest.PrepareDTO reqDTO, HttpSession session) {
        reqDTO.validate();

        // 누가 요청한지 - 세션에서 추출
        User sessionUser = (User) session.getAttribute("sessionUser");
        if(sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));

        }
        // 결제 서비스 - 결제 요청 생성(주문번호 생성 및 중복 확인0
        PaymentResponse.PrepareDTO prepareDTO = paymentService.결제요청생성(sessionUser.getId(), reqDTO.getAmount());

        // js - > 성공 응답 반환
        return ResponseEntity.ok()
                .body(Map.of("merchant_uid", prepareDTO.getMerchantUid(),
                        "amount", prepareDTO.getAmount(),
                        "impKey", prepareDTO.getImpKey()));
    }

    // /api/payment/verify
    @PostMapping("/api/payment/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentRequest.verifyDTO reqDTO, HttpSession session) {
        User sessionUser = (User) session.getAttribute("sessionUser");
        if(sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));

        }
        reqDTO.validate();
        PaymentResponse.VerifyDTO verifyDTO = paymentService.결제검증및충전(
                sessionUser.getId(),
                reqDTO.getImpUid(),
                reqDTO.getMerchantUid()
        );

        // 세션에서 사용자 포인트 정보 즉시 업데이트
        sessionUser.setPoint(verifyDTO.getCurrentPoint());
        // 반드시 갱신해주어야함
        session.setAttribute("sessionUser", sessionUser);

        return ResponseEntity.ok()
                .body(Map.of("message", "결제 성공",
                        "amount", verifyDTO.getAmount(),
                        "currentPoint", verifyDTO.getCurrentPoint()));
    }
}
