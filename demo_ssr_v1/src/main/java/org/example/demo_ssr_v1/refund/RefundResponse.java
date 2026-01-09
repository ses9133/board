package org.example.demo_ssr_v1.refund;

import lombok.Data;
import org.example.demo_ssr_v1._core.utils.MyDateUtil;

public class RefundResponse {
    @Data
    public static class ListDTO {
        private Long id;
        private Long paymentId;
        private Integer amount;
        private String reason;
        private String rejectReason;
        private String statusDisplay; // 화면 표시용 (대기중, 승인, 거절)

        // 상태별 플래그 변수 사용 (화면 표시용)
        private boolean isPending; // 대기중
        private boolean isApproved; // 승인 완료
        private boolean isRejected; // 거절

        public ListDTO(RefundRequest refund) {
            this.id = refund.getId();
            this.paymentId = refund.getPayment().getId();
            this.amount = refund.getPayment().getAmount();
            this.reason = refund.getReason();
            this.rejectReason = refund.getRejectReason() == null ? "" : refund.getRejectReason();

            switch (refund.getStatus()) {
                case PENDING -> this.statusDisplay = "대기중";
                case APPROVED -> this.statusDisplay = "승인됨";
                case REJECT -> this.statusDisplay = "거절됨";
            }
            this.isPending = refund.getStatus() == RefundStatus.PENDING;
            this.isApproved = refund.getStatus() == RefundStatus.APPROVED;
            this.isRejected = refund.getStatus() == RefundStatus.REJECT;
        }
    }

    @Data
    public static class AdminListDTO {
        private Long id;
        private String username;
        private Long paymentId;
        private String impUid; // 포트원으로 환불 승인 요청할 때 필요
        private String merchantUid;
        private Integer amount;
        private String requestedAt;
        private RefundStatus status;
        private String statusDisplay;
        private String reason;
        private String rejectReason;

        public AdminListDTO(RefundRequest refundRequest) {
            this.id = refundRequest.getId();
            this.username = refundRequest.getUser().getUsername();
            this.paymentId = refundRequest.getPayment().getId();
            this.impUid = refundRequest.getPayment().getImpUid();
            this.merchantUid = refundRequest.getPayment().getMerchantUid();
            this.amount = refundRequest.getPayment().getAmount();
            if(refundRequest.getCreatedAt() != null) {
                this.requestedAt = MyDateUtil.format(refundRequest.getCreatedAt());
            }
            this.status = refundRequest.getStatus();
            switch (refundRequest.getStatus()) {
                case PENDING -> this.statusDisplay = "대기중";
                case APPROVED -> this.statusDisplay = "승인됨";
                case REJECT -> this.statusDisplay = "거절됨";
            }
            this.reason = refundRequest.getReason();
            this.rejectReason = refundRequest.getRejectReason();
        }
    }
}
