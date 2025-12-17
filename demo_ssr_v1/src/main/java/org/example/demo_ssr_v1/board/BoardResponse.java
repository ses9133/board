package org.example.demo_ssr_v1.board;

import lombok.Data;
import org.example.demo_ssr_v1._core.utils.MyDateUtil;


public class BoardResponse {
    @Data
    public static class ListDTO {
        private Long id;
        private String title;
        private String username;  // 작성자명 (평탄화) {{board.username}} 으로 사용 가능
        private String createdAt;

        public ListDTO(Board board) {
            this.id = board.getId();
            this.title = board.getTitle();
            // 쿼리 --> JOIN FETCH 로 가져오면 문제 없음
            if(board.getUser() != null) {
                this.username = board.getUser().getUsername();
            }
            if(board.getCreatedAt() != null) {
                this.createdAt = MyDateUtil.format(board.getCreatedAt());
            }
        }
    }

    @Data
    public static class DetailDTO {
        private Long id;
        private String title;
        private String content;
        private Long userId;
        private String username;
        private String createdAt;

        public DetailDTO(Board board) {
            this.id = board.getId();
            this.title = board.getTitle();
            this.content = board.getContent();
            // JOIN FETCH 활용 (한번에 JOIN 해서 Repository 에서 가져올 예정)
            if(board.getUser() != null) {
                this.userId = board.getUser().getId();
                this.username = board.getUser().getUsername();
            }
            if(board.getCreatedAt() != null) {
                this.createdAt = MyDateUtil.format(board.getCreatedAt());
            }
        }
    }
    /**
     * 수정화면 응답 DTO
     * */
    @Data
    public static class UpdateFormDTO {
        private Long id;
        private String title;
        private String content;

        public UpdateFormDTO(Board board) {
            this.id = board.getId();
            this.title = board.getTitle();
            this.content = board.getContent();
        }
    }
}
