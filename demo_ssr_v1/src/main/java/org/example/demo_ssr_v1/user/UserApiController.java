package org.example.demo_ssr_v1.user;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserApiController {
    private final MailService mailService;
    private final UserService userService;

    @PostMapping("/api/email/send")
    public ResponseEntity<?> 인증번호발송(@RequestBody UserRequest.EmailCheckDTO reqDTO) {
        // 1. 유효성 검사
        reqDTO.validate();

        // 2. 서비스단에서 구글 메일 서버로 이메일 전송 처리
        mailService.인증번호발송(reqDTO.getEmail());

        return ResponseEntity.ok().body(Map.of("message", "인증번호가 발송되었습니다."));
    }

    @PostMapping("/api/email/verify")
    public ResponseEntity<?> 인증번호확인(@RequestBody UserRequest.EmailCheckDTO reqDTO) {
        reqDTO.validate();

        if(reqDTO.getCode().isEmpty() || reqDTO.getCode().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "인증번호를 입력해주세요"));
        }

        // 서비스단에서 인증번호 확인
        boolean isVerified = mailService.인증번호확인(reqDTO.getEmail(), reqDTO.getCode());

        // 결과값에 따라 분기처리
        if(isVerified) {
            return ResponseEntity.ok().body(Map.of("message", "인증되었습니다."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "인증 실패하였습니다."));
        }
    }

    // api/point/charge
    @PostMapping("/api/point/charge")
    public ResponseEntity<?> chargePoint(@RequestBody UserRequest.PointChargeDTO reqDTO, HttpSession session) {
        reqDTO.validate();

        User sessionUser = (User) session.getAttribute("sessionUser");
        if(sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        // 포인트 충전 처리
        User updatedUser = userService.포인트충전(sessionUser.getId(), reqDTO.getAmount());

        // 세션에 업데이트된 사용자 정보 갱신(포인트)
        session.setAttribute("sessionUser", updatedUser);
        return ResponseEntity.ok()
                .body(Map.of("message", "포인트가 충전되었슶니다.",
                        "amount", reqDTO.getAmount(),
                        "currentPoint", updatedUser.getPoint()));
    }
}
