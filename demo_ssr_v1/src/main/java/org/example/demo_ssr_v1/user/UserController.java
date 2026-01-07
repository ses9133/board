package org.example.demo_ssr_v1.user;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.demo_ssr_v1.payment.PaymentResponse;
import org.example.demo_ssr_v1.payment.PaymentService;
import org.example.demo_ssr_v1.purchase.PurchaseResponse;
import org.example.demo_ssr_v1.purchase.PurchaseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * 사용자 Controller (표현 계층)
 * 핵심 개념:
 *  - HTTP 요청을 받아서 처리
 *  - 요청 데이터 검증 및 파라미터 바인딩
 *  - Service 레이어에 비즈니스 로직을 위임
 *  - 응답 데이터를 View에 전달함
 * */

@RequiredArgsConstructor
@Controller
public class UserController {
    private final UserService userService;
    private final PurchaseService purchaseService;
    private final PaymentService paymentService;

    // /user/payment/list
    @GetMapping("/user/payment/list")
    public String paymentList(Model model, HttpSession session) {
        User sessionUser = (User) session.getAttribute("sessionUser");
        List<PaymentResponse.ListDTO> paymentList = paymentService.paymentList(sessionUser.getId());
        model.addAttribute("paymentList", paymentList);
        return "user/payment-list";
    }

    // /user/purchase/list
    @GetMapping("/user/purchase/list")
    public String purchaseList(Model model, HttpSession session) {
        User sessionUser = (User) session.getAttribute("sessionUser");
        List<PurchaseResponse.ListDTO> purchaseList = purchaseService.구매내역조회(sessionUser.getId());

        model.addAttribute("purchaseList", purchaseList);
        return "user/purchase-list";
    }

    // 프로필 이미지 삭제하기
    @PostMapping("/user/profile-image/delete")
    public String deleteProfileImage(HttpSession session) {
        User sessionUser = (User) session.getAttribute("sessionUser");
        User updatedUser = userService.프로필이미지삭제(sessionUser.getId());
        // 왜 user 를 다시 받을까 ? -- 프로필이 삭제되었기 때문에 세션 정보 갱신 처리 해주기 위함이다.
        session.setAttribute("sessionUser", updatedUser);

        // 일반적으로 POST 요청이 오면 PRG 패턴으로 설계
        // POST -> Redirect 처리 ---> GET 요청
        return "redirect:/user/detail";
    }

    // 마이 페이지
    @GetMapping("/user/detail")
    public String detailForm(Model model, HttpSession session) {
        User sessionUser = (User) session.getAttribute("sessionUser");
        User user = userService.마이페이지(sessionUser.getId());
        model.addAttribute("user", user);
        return "user/detail";
    }

    // 회원 정보 수정 화면 요청
    // http://localhost:8080/user/update
    @GetMapping("/user/update")
    public String updateForm(Model model, HttpSession session) {
        // HttpServeltRequest
        // : A 사용자가 요청 시 -- > 웹서버 --> 톰캣(WAS) Request 객체와 Response 객체를 만들어서
        // 스프링 컨테이너에게 전달

        // 1. 인증 검사(o)
        // 2. 유효성 검사(x)
        // 인증 검사를 하려면 세션 메모리에 접근해서 사용자의 정보가 있는지 없는지 유무 확인
        User sessionUser = (User) session.getAttribute("sessionUser");
//        if(sessionUser == null) {
//            System.out.println("로그인하지 않은 사용자입니다.");
//            return "redirect:/login";
//        } --> LoginInterceptor가 알아서 처리해줌

        // 2. 인가 처리
        // 세션의 사용자 ID로 회원 정보 조회
        User user = userService.회원정보수정화면(sessionUser.getId());
        model.addAttribute("user", user);

        return "user/update-form";
    }

    // 회원 정보 수정 기능 요청 - 더티체킹
    // http://localhost:8080/user/update
    @PostMapping("/user/update")
    public String updateProc(UserRequest.UpdateDTO updateDTO, HttpSession session) {
        // 1. 인증검사
        User sessionUser = (User) session.getAttribute("sessionUser");
//        if(sessionUser == null) {
//            System.out.println("로그인하지 않은 사용자입니다.");
//            return "redirect:/login";
//        }

        try {
            updateDTO.validate();
            User updatedUser = userService.회원정보수정(updateDTO, sessionUser.getId());
            // 회원 정보 수정은 세션 갱신 필요
            session.setAttribute("sessionUser", updatedUser);
            return "redirect:/";
        } catch (Exception e) {
            return "user/update-form";
        }
    }

    // 로그아웃 기능 요청
    // http://localhost:8080/logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // 세션 무효화
        session.invalidate();
        return "redirect:/";
    }

    // 로그인 화면 요청
    // http://localhost:8080/login
    @GetMapping("/login")
    public String loginForm() {
        return "user/login-form";
}

    // 세션 기반 인증 처리(JWT 토큰 기반 인증 x)
    // 로그인 기능 요청
    // http://localhost:8080/login
    @PostMapping("/login")
    public String loginProc(UserRequest.LoginDTO loginDTO, HttpSession session) {
        // 1. 인증검사 X - 로그인 요청
        // 2. 유효성 검사
        // 3. DB 에 사용자 이름과 비밀번호 확인
        // 4. 로그인 성공 또는 실패 처리
        // 5. 웹 서버는 바보이기에 사용자의 정보를 세션 메모리에 저장시켜야
        //     다음번 요청이 오더라도 알 수 있음  - 세션 저장 처리
        try {
            loginDTO.validate();
            User sessionUser = userService.로그인(loginDTO);
            // 세션에 저장
            session.setAttribute("sessionUser", sessionUser);
            return "redirect:/";
        } catch (Exception e) {
            return "user/login-form";
        }
    }

    // http://localhost:8080/join
    @GetMapping("/join")
    public String joinForm() {
        return "user/join-form";
    }

    // 회원가입 기능 요청
    // http://localhost:8080/join
    @PostMapping("/join")
    public String joinProc(UserRequest.JoinDTO joinDTO) {
        // 1. 인증검사(X) - 필요없음 (회원가입임)
        // 2. 유효성 검사
        joinDTO.validate();
        userService.회원가입(joinDTO);

        return "redirect:/login";
    }

    @GetMapping("/user/point/charge")
    public String chargePointForm(Model model, HttpSession session) {
        User sessionUser = (User) session.getAttribute("sessionUser");
        model.addAttribute("user", sessionUser);
        return "user/charge-point";
    }
}
