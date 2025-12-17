package org.example.demo_ssr_v1.reply;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.demo_ssr_v1._core.errors.exception.Exception400;
import org.example.demo_ssr_v1.board.Board;
import org.example.demo_ssr_v1.user.User;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

/**
 * 단방향
 */
@Data
@NoArgsConstructor
@Table(name = "reply_tb")
@Entity
public class Reply {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String comment;

    // 단방향 설계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    private Timestamp createdAt;

    @Builder
    public Reply(String comment, Board board, User user) {
        this.comment = comment;
        this.board = board;
        this.user = user;
    }

    // 소유자 여부 확인
    public boolean isOwner(Long userId) {
        if (userId == null || this.user == null) {
            return false;
        }
        Long replyUserId = this.user.getId();
        if(replyUserId == null) {
            return false;
        }

        return replyUserId.equals(userId);
    }

    // 댓글 내용 수정
    public void updateComment(String newComment) {
        if (newComment == null || newComment.trim().isEmpty()) {
            throw new Exception400("댓글 내용 입력은 필수입니다.");
        }
        if(newComment.length() > 500) {
            throw new Exception400("댓글 내용은 최대 500자까지 입니다.");
        }
        this.comment = newComment;
    }

}
