package org.example.demo_ssr_v1.refund;

import lombok.RequiredArgsConstructor;
import org.example.demo_ssr_v1._core.errors.exception.Exception400;
import org.example.demo_ssr_v1._core.errors.exception.Exception403;
import org.example.demo_ssr_v1._core.errors.exception.Exception404;
import org.example.demo_ssr_v1.payment.Payment;
import org.example.demo_ssr_v1.payment.PaymentRepository;
import org.example.demo_ssr_v1.user.User;
import org.example.demo_ssr_v1.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {
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
}
