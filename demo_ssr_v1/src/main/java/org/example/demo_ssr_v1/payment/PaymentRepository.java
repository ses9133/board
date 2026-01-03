package org.example.demo_ssr_v1.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // imp_uid 로 결제내역조회
    // 포트원 결제 번호로 Payment 정보 조회
    Optional<Payment> findByImpUid(String impUid);

    Optional<Payment> findByMerchantUid(String impUid);

    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.merchantUid = :merchantUid")
    boolean existsByMerchantUid(@Param("merchantUid") String merchantUid);
}
