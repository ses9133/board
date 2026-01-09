package org.example.demo_ssr_v1.refund;

import lombok.RequiredArgsConstructor;
import org.example.demo_ssr_v1._core.errors.exception.Exception400;
import org.example.demo_ssr_v1._core.errors.exception.Exception403;
import org.example.demo_ssr_v1._core.errors.exception.Exception404;
import org.example.demo_ssr_v1._core.errors.exception.Exception500;
import org.example.demo_ssr_v1.payment.Payment;
import org.example.demo_ssr_v1.payment.PaymentRepository;
import org.example.demo_ssr_v1.payment.PaymentResponse;
import org.example.demo_ssr_v1.user.User;
import org.example.demo_ssr_v1.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {
    @Value("${portone.imp-key}")
    private String impKey;

    @Value("${portone.imp-secret}")
    private String impSecret;

    private final PaymentRepository paymentRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final UserRepository userRepository;

    // 0 단계: 환불 요청화면 진입시 검증
    public Payment 환불요청폼화면검증(Long paymentId, Long userId) {
        // 결제 내역 정보 확인해야함 ---> 누가 결제했는지 정보 있음
        // userId == payment.getUser.getId() 확인해야함

        // 1. 결제 내역 조회 (User 정보 함께)
        Payment payment = paymentRepository.findByIdWithUser(paymentId);

        // 2. 본인 확인
        if(!payment.getUser().getId().equals(userId)) {
            throw new Exception403("본인 결제 내역만 환불 요청 가능합니다.");
        }

        // 3. 결제 완료 상태 인지 확인("paid", "canceled" 가 있는데 paid 일 때만 폼 보여줄 수 있음)
        if(!"paid".equals(payment.getStatus())) {
            throw new Exception400("결제 완료된 상태만 환불 요청 가능합니다.");
        }

        // 4. 이미 사용자가 환불 요청 한 상태인지 확인 (요청한 상태인데 또 요청 폼을 보여줄 수 없음)
        if(refundRequestRepository.findByPaymentId(paymentId).isPresent()) {
            throw new Exception400("이미 환불 요청이 진행중입니다.");
        }

        return payment;
    }

    @Transactional
    // 1 단계: 사용자가 환불 요청함
    public void 환불요청(Long userId, RefundRequestDTO.RequestDTO reqDTO) {
        // 화면 검증 로직 재사용
        Payment payment = 환불요청폼화면검증(reqDTO.getPaymentId(), userId);

        // 사용자 조회 (세션으로 넘어온 id 가 실제 존재하는지 확인)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("사용자를 찾을 수 없습니다."));

        // 환불 요청 테이블에 이력 저장
        RefundRequest refundRequest = RefundRequest.builder()
                .user(user)
                .payment(payment)
                .reason(reqDTO.getReason())
                .build();
        refundRequestRepository.save(refundRequest);
    }

    // 내 아이디로 조회(환불 요청 내역)
    public List<RefundResponse.ListDTO> 환불요청목록조회(Long userId) {
        List<RefundRequest> refundList = refundRequestRepository.findAllByUserId(userId);
        return refundList.stream()
                .map(RefundResponse.ListDTO::new)
                .toList();
    }

    public List<RefundResponse.AdminListDTO> 관리자환불요청목록조회() {
        List<RefundRequest> refundRequestList = refundRequestRepository.findAllWithUserAndPayment();
        return refundRequestList.stream()
                .map(RefundResponse.AdminListDTO::new)
                .toList();
    }

    @Transactional
    public void 환불거절(Long refundRequestId, String rejectReason) {
        RefundRequest refundRequest = refundRequestRepository.findById(refundRequestId)
                .orElseThrow(() -> new Exception404("환불 요청을 찾을 수 없습니다."));
        if(!refundRequest.isPending()) {
            throw new Exception400( "대기 중인 환불 요청만 거절 할 수 있습니다.");
        }
        refundRequest.reject(rejectReason);
    }

    @Transactional
    public void 환불승인(Long refundRequestId) {
        // 1. 환불 요청 조회 ( + User / Payment 정보도 같이 들고 와야함)
        RefundRequest refundRequest = refundRequestRepository.findByIdWithUserAndPayment(refundRequestId)
                .orElseThrow(() -> new Exception404("환불 요청을 찾을 수 없습니다."));

        // 2. 환불 상태 확인
        if(!refundRequest.isPending()) {
            throw new Exception400("대기 중인 환불 요청만 승인할 수 있습니다.");
        }

        // 3. 포인트 잔액 검증
        Payment payment = refundRequest.getPayment();
        User user = refundRequest.getUser();
        Integer refundAmount = payment.getAmount();

        if(user.getPoint() < refundAmount) {
            // 이미 돈 사용한 상태
            throw new Exception400("사용자의 포인트 잔액이 부족하여 환불 불가");
        }

        // 포트원 액세스 토큰 발급 요청(포트원 인증 서버)
        // 포트원 자원 서버에서 refund row update 요청(결제 취소)
        포트원결제취소(payment.getImpUid(), payment.getAmount());

        refundRequest.setStatus(RefundStatus.APPROVED);
        payment.setStatus("cancelled");
        // 내 포인트 잔액을 환불한 금액 만큼 차감해야함
        user.deductPoint(refundAmount);
    }

    private void 포트원결제취소(String impUid, Integer amount) {
        //1 . 액세스 토큰 발급
        String accessToken = 포트원액세스토큰발급();
        System.out.println("=== 취소용 accessToken: " + accessToken);

        // 2. 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        // 3. 요청 바디 생성
        Map<String, Object> body = new HashMap<>();
        body.put("reason", "관리자 환불 승인");
        body.put("imp_uid", impUid);
        body.put("amount", amount);

        // 4. HTTP 요청 메시지 만들기
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // 5. HTTP 클라이언트 객체 ---> RestTemplate 사용할 예정
        RestTemplate restTemplate = new RestTemplate();

        // 응답 DTO 따로 안만들고 Map 형태로
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.iamport.kr/payments/cancel",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
             System.out.println("포트원 결제 취소 응답 : " + response);

             // 6. 응답 처리
            Map<String, Object> responseBody = response.getBody();
            if(responseBody == null) {
                throw new Exception500("포트원 결제취소 응답이 비어있습니다.");
            }
            /*
            * 취소 시 유의할 점
                REST API(POST https://api.iamport.kr/payments/cancel) 요청에 대한 응답 코드가 200이라도 응답 body의 code가 0이 아니면 환불에 실패했다는 의미입니다.
                실패 사유는 body의 message를 통해 확인하셔야 합니다.
            * */
            Integer code = (Integer) responseBody.get("code");
            if(code != 0) {
                String message = (String) responseBody.get("message");
                throw new Exception400("환불 실패: " + message);
            }
        } catch (Exception e) {
                throw new Exception500("포트원 결제 취소중 오류 발생");
        }
    }

    private String 포트원액세스토큰발급() {
        try {
            // https://api.iamport.kr/users/getToken
            RestTemplate restTemplate = new RestTemplate();

            // HTTP 메세지 헤더 생성
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // HTTP 메세지 바디 생성
            Map<String, String> body = new HashMap<>();
            // 포트원에서 발급 받았던 REST API KEY
            body.put("imp_key", impKey);
            body.put("imp_secret", impSecret);

            // 헤더 + body 결합
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            // 통신 요청
            ResponseEntity<PaymentResponse.PortOneTokenResponse> response = restTemplate.exchange(
                    "https://api.iamport.kr/users/getToken",
                    HttpMethod.POST,
                    request,
                    PaymentResponse.PortOneTokenResponse.class
            );
            System.out.println("액세스 토큰 확인 ");
            System.out.println(response.getBody().getResponse().getAccessToken());
            System.out.println("response : " + response);

            // 응답받은 액세스토큰 리턴
            return response.getBody().getResponse().getAccessToken();
        } catch (Exception e) {
            throw new Exception400("포트원 인증실패: 관리자 설정을 확인하세요");
        }
    }
}
