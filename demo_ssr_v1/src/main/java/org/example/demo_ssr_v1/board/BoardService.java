package org.example.demo_ssr_v1.board;

import lombok.RequiredArgsConstructor;
import org.example.demo_ssr_v1._core.errors.exception.Exception403;
import org.example.demo_ssr_v1._core.errors.exception.Exception404;
import org.example.demo_ssr_v1.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {
    private final BoardRepository boardRepository;

    // 읽기 전용 트랜잭션 - 성능 최적화
    public List<BoardResponse.ListDTO> 게시글목록조회() {
        // List<Board> --> List<BoardResponse.ListDTO>
        List<Board> boardList = boardRepository.findAllWithUserByOrderByCreatedAtDesc();

        // 1. 반복문 활용 방법
//        List<BoardResponse.ListDTO> dtoList = new ArrayList<>();
//        for(Board board: boardList) {
//            BoardResponse.ListDTO dto = new BoardResponse.ListDTO(board);
//            dtoList.add(dto);
//        }
//        return dtoList;

        // 2. 람다식 활용 -> 참조메서드
        return boardList.stream()
                .map(BoardResponse.ListDTO::new)
                .toList();
    }

    public BoardResponse.DetailDTO 게시글상세조회(Long boardId) {
        Board board = boardRepository.findByIdWithUser(boardId)
                .orElseThrow(() -> new Exception404("해당 게시글을 찾을 수 없습니다."));

        return new BoardResponse.DetailDTO(board);
    }

    // 1. 트랜잭션 처리
    // 2. 리포지토리 저장
    @Transactional
    public Board 게시글작성(BoardRequest.SaveDTO saveDTO, User sessionUser) {
        Board board = saveDTO.toEntity(sessionUser); // 현재 board: 영속성 컨텍스트에 안올라간 상태
        // DTO 에서 직접 new 해서 생성한 Board 객체 일 뿐
        boardRepository.save(board);
        return board;
    }

    // 1. 게시글 조회
    // 2. 인가 처리
    public BoardResponse.UpdateFormDTO 게시글수정화면(Long boardId, Long sessionUserId) {
        Board boardEntity = boardRepository.findByIdWithUser(boardId)
                .orElseThrow(() -> new Exception404("해당 게시글을 찾을 수 없습니다."));

        if (!boardEntity.isOwner(sessionUserId)) {
            throw new Exception403("게시글 수정 권한이 없습니다.");
        }
        return new BoardResponse.UpdateFormDTO(boardEntity);
    }

    // 1. 트랜잭션 처리
    // 2. DB 에서 조회
    // 3. 인가 처리
    // 4. 조회된 게시글에 상태값 변경(더티 체킹)
    @Transactional // 1.
    public Board 게시글수정(BoardRequest.UpdateDTO updateDTO, Long boardId, Long sessionUserId) {
        // 2.
        Board boardEntity = boardRepository.findById(boardId)
                .orElseThrow(() -> new Exception404("해당 게시글을 찾을 수 없습니다."));

        // 3.
        if (!boardEntity.isOwner(sessionUserId)) {
            throw new Exception403("게시글 수정 권한이 없습니다.");
        }
        // 4.
        boardEntity.update(updateDTO);
        return boardEntity;
    }

    // 1. 트랜잭션 처리
    // 2. DB 에서 게시글 조회 (조회부터 해야 DB에 있는 Board에 user_id 값을 확인할 수 있음)
    // 3. 인가처리
    // 4. Repository 에게 삭제요청
    @Transactional
    public void 게시글삭제(Long boardId, Long sessionUserId) {
        Board boardEntity = boardRepository.findById(boardId)
                .orElseThrow(() -> new Exception404("해당 게시글을 찾을 수 없습니다."));

        if (!boardEntity.isOwner(sessionUserId)) {
            throw new Exception403("게시글 수정 권한이 없습니다.");
        }
        boardRepository.deleteById(boardId);
    }
}
