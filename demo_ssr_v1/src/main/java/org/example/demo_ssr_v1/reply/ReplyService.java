package org.example.demo_ssr_v1.reply;

import lombok.RequiredArgsConstructor;
import org.example.demo_ssr_v1._core.errors.exception.Exception403;
import org.example.demo_ssr_v1._core.errors.exception.Exception404;
import org.example.demo_ssr_v1.board.Board;
import org.example.demo_ssr_v1.board.BoardRepository;
import org.example.demo_ssr_v1.user.User;
import org.example.demo_ssr_v1.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReplyService {
    private final ReplyRepository replyRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    // 댓글 목록 조회
    /**
     * OSIV 대응하기 위해 DTO 설계, 계층간의 결합도를 줄이기 위해 DTO 설계
     * - JOIN FETCH 로 한 번에 User 를 들고 옴
     * */
    public List<ReplyResponse.ListDTO> 댓글목록조회(Long boardId, Long sessionUserId) {
        // 1. 조회
        // 2. 인가 처리 X
        // 3. List<Reply> -> List<ReplyResponse.ListDTO> 데이터 변환
        List<Reply> replyList = replyRepository.findByBoardIdWithUser(boardId); // 영속 상태
        return replyList.stream()
                .map(reply -> new ReplyResponse.ListDTO(
                        reply, sessionUserId))
                .toList();
    }

    // 댓글 작성
    @Transactional
    public Reply 댓글작성(ReplyRequest.SaveDTO saveDTO, Long sessionUserId) {
        // 1. 게시글 존재 유무 확인
        // 2. 현재 로그인 여부 확인
        // 3. 인가 처리 필요 X
        // 4. 요청 DTO -> Reply 엔티티로 변환
        // 5. 저장 요청

        Board boardEntity = boardRepository.findById(saveDTO.getBoardId())
                .orElseThrow(() -> new Exception404("해당 게시글을 찾을 수 없습니다."));

        User userEntity = userRepository.findById(sessionUserId)
                .orElseThrow(() -> new Exception404("사용자를 찾을 수 없습니다."));

        Reply reply = saveDTO.toEntity(boardEntity, userEntity); // 비영속 상태
        return replyRepository.save(reply);
    }

    // 댓글 삭제
    @Transactional
    public Long 댓글삭제(Long replyId, Long userId) {
        // 1. 댓글 조회
        Reply replyEntity = replyRepository.findByIdWithUser(replyId)
                .orElseThrow(() -> new Exception404("해당 댓글을 찾을 수 없습니다."));
        // 2. 권한 체크
        if(!replyEntity.isOwner(userId)) {
            throw new Exception403("댓글 삭제 권한이 없습니다.");
        }

        Long boardId = replyEntity.getBoard().getId();

        // 3. 댓글 삭제
        replyRepository.delete(replyEntity);

        // 컨트롤러 단에서 리다이렉트 처리해서 다시 게시글 상세보기 호출하기 위함
        return boardId;
    }

    // 댓글 수정

}
