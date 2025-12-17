package org.example.demo_ssr_v1.board;

import lombok.Data;
import org.example.demo_ssr_v1._core.utils.MyDateUtil;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;


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

    @Data
    public static class PageDTO {
        private List<ListDTO> content;
        private int number; // 현재 페이지 번호 (0)
        private int size; // 한 페이지의 크기
        private int totalPages; // 전체 페이지 수
        private Long totalElements; // 전체 게시글 수
        private boolean first; // 첫 페이지 여부
        private boolean last; // 마지막 페이지 여부
        private boolean hasNext; // 다음 페이지 존재 여부
        private boolean hasPrevious; // 이전 페이지 존재 여부

        private Integer previousPageNumber; // 이전 페이지 번호 (없으면 null)
        private Integer nextPageNumber; // 다음 페이지 번호 (없으면 null)

        private List<PageLink> pageLinks; // 페이지 번호 링크 목록

        public PageDTO(Page<Board> page) {
            page.getContent();
            // page.getContent -> List<Board>
            // 게시글 리스트 DTO --> BoardResponse.ListDTO
            this.content = page.getContent().stream()
                    .map(ListDTO::new)
                    .toList();
            this.number = page.getNumber();
            this.size = page.getSize();
            this.totalPages = page.getTotalPages();
            this.totalElements = page.getTotalElements();
            this.first  = page.isFirst();
            this.last = page.isLast();
            this.hasNext = page.hasNext();
            this.hasPrevious = page.hasPrevious();

            // 현재 페이지 인덱스 0부터 시작함
            // 화면 -> N + 1
            // 이전 페이지는 N
            this.previousPageNumber = page.hasPrevious() ? page.getNumber() : null;
            this.nextPageNumber = page.hasNext() ? page.getNumber() + 2 : null;

            // pageLinks
            this.pageLinks = generatePageLinks(page);
        }

        private List<PageLink> generatePageLinks(Page<Board> page) {
            List<PageLink> links = new ArrayList<>();
            int currentPage = page.getNumber() + 1; // page.getNumber() - 0부터 시작하기 때문
            int totalPages = page.getTotalPages();

            // 3 4 [5] 6 7
            int startPage = Math.max(1, currentPage - 2);
            int endPage = Math.min(totalPages, currentPage + 2);

            for(int i = startPage; i <= endPage; i++) {
                PageLink link = new PageLink();
                link.setDisplayNumber(i);
                link.setActive(i == currentPage);
                links.add(link);
            }
            return links;
        }
    }

    // 페이지 링크 클래스 설계
    @Data
    public static class PageLink {
        private int displayNumber; // 표시할 페이지 번호
        private boolean active;
    }
}
