package org.example.demo_ssr_v1.purchase;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.demo_ssr_v1.board.Board;
import org.example.demo_ssr_v1.user.User;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.sql.Timestamp;

@Entity
@Table(
        name = "purchase_tb",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_board", columnNames = {"user_id", "board_id"})
        }
)
@Data
@NoArgsConstructor
public class Purchase {
    // 1. User
    // 2. Board
    // 3. 홍길동 1번 게시글 구매한 기록
    // 4. 중복 구매 방지
    // 5. 구매시 지불한 포인트
    // 6. 구매 시간

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 단방향 설계 Purchase : User = N : 1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 단방항 설계  Purchase : Board = N : 1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    private Integer price;

    @CreationTimestamp
    private Timestamp createdAt;

    @Builder
    public Purchase(User user, Board board, Integer price) {
        this.user = user;
        this.board = board;
        this.price = price;
    }
}
