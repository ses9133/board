package org.example.demo_ssr_v1.reply;

import lombok.Data;
import org.example.demo_ssr_v1._core.utils.MyDateUtil;

public class ReplyResponse {
    @Data
    public static class ListDTO {
        private Long id;
        private String comment;
        private Long userId; // 댓글 작성자 ID
        private String username; // 댓글 작성자명 (평탄화)
        private String createdAt;
        private boolean isOwner; // 댓글 소유자 여부 확인 (세션 ID 값과 비교)

        public ListDTO(Reply reply, Long sessionUserId) {
            this.id = reply.getId();
            this.comment = reply.getComment();
            // Repository 에서 JOIN FETCH 로 이미 로딩된 User 임
            if(reply.getUser() != null) {
                this.userId = reply.getUser().getId();
                this.username = reply.getUser().getUsername();
            }
            if(reply.getCreatedAt() != null) {
                this.createdAt = MyDateUtil.format(reply.getCreatedAt());
            }
            this.isOwner = reply.isOwner(sessionUserId); // 댓글 소유자 확인
        }
    }
}
