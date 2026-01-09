package org.example.demo_ssr_v1.refund;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.demo_ssr_v1.payment.Payment;
import org.example.demo_ssr_v1.user.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "refund_request_tb")
@NoArgsConstructor
@Data
public class RefundRequest { // 환불 요청 테이블
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 환불 요청자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 결제내역
    // 환불 정책 -- 전체 환불 정책 1:1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    // 원래는 우리 프로젝트 비즈니스 로직에서는 전체 환불 정책 1:1 구조이다.
    // 추후 확장성을 위해서 부분환불을 도입한다면 1:N 설계되어야 한다.
    // @OneToOne 대신 @ManyToOne 에 unique=true 제약조건을 걸면 1:1 구현 됨
    // 추후 변경이 일어난다면 unique=true만 제거하면 됨
    private Payment payment;

    // 환불 사유
    @Column(length = 500)
    private String reason;

    // 관리자 환불 거절 사유
    @Column(length = 500)
    private String rejectReason;

    // 환불 상태
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RefundStatus status = RefundStatus.PENDING;

    // 생성 시간
    @CreationTimestamp
    @Column(nullable = false)
    private Timestamp createdAt;

    // 수정 시간
    @UpdateTimestamp
    private Timestamp updatedAt;

    // 사용자가 먼저 환불 요청에 의해서 row 생성되기 떄문
    @Builder
    public RefundRequest(User user, Payment payment, String reason) {
        this.user = user;
        this.payment = payment;
        this.reason = reason;
        this.status = RefundStatus.PENDING;
    }

    // ==== 편의 기능

    // 환불 승인 처리
    public void approve() {
        this.status = RefundStatus.APPROVED;
    }

    public void reject(String rejectReason) {
        this.status = RefundStatus.REJECT;
        this.rejectReason = rejectReason;
    }

    // 대기 중인 상태인지 확인
    public boolean isPending() {
        return this.status == RefundStatus.PENDING;
    }

    // 승인 상태인지 확인
    public boolean isApproved() {
        return this.status == RefundStatus.APPROVED;
    }

    // 거절 상태인지
    public boolean isRejected() {
        return this.status == RefundStatus.REJECT;
    }
}
