package org.example.demo_ssr_v1.refund;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.demo_ssr_v1._core.errors.exception.Exception401;
import org.example.demo_ssr_v1.payment.Payment;
import org.example.demo_ssr_v1.payment.PaymentResponse;
import org.example.demo_ssr_v1.user.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class RefundController {
    private final RefundService refundService;

    @GetMapping("/refund/request/{paymentId}")
    public String refundRequestForm(@PathVariable Long paymentId, Model model, HttpSession session) {
        // 인증 인터셉터 아직 적용전
        // TODO 인터셉터 추가
        User sessionUser = (User) session.getAttribute("sessionUser");

        // 서비스에 paymentId 관련 정보를 요청
        Payment payment = refundService.환불요청폼화면검증(paymentId, sessionUser.getId());
        PaymentResponse.ListDTO paymentDTO = new PaymentResponse.ListDTO(payment);
        model.addAttribute("payment", paymentDTO);

        return "refund/request-form";
    }

    @PostMapping("/refund/request")
    @ResponseBody
    public String refundRequest(RefundRequestDTO.RequestDTO reqDTO, HttpSession session) {
        User sessionUser = (User) session.getAttribute("sessionUser");
        if(sessionUser == null) {
            throw new Exception401("로그인이 필요합니다.");
        }
        reqDTO.validate();
        refundService.환불요청(sessionUser.getId(), reqDTO);
        return "redirect:/refund/list";
    }

    // 내 환불 요청 내역 조회
    @GetMapping("/refund/list")
    public String refundList(Model model, HttpSession session) {
        User sessionUser = (User) session.getAttribute("sessionUser");
        if(sessionUser == null) {
            throw new Exception401("로그인이 필요합니다.");
        }
        List<RefundResponse.ListDTO> refundList = refundService.환불요청목록조회(sessionUser.getId());
        model.addAttribute("refundList", refundList);
        return "refund/list";
    }
}
