package org.example.demo_ssr_v1.board;

import lombok.RequiredArgsConstructor;
import org.example.demo_ssr_v1._core.errors.exception.Exception403;
import org.example.demo_ssr_v1._core.errors.exception.Exception404;
import org.example.demo_ssr_v1.purchase.PurchaseService;
import org.example.demo_ssr_v1.reply.ReplyRepository;
import org.example.demo_ssr_v1.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {
    private final BoardRepository boardRepository;
    private final ReplyRepository replyRepository;
    private final PurchaseService purchaseService;

//    // 읽기 전용 트랜잭션 - 성능 최적화
//    public List<BoardResponse.ListDTO> 게시글목록조회() {
//        // List<Board> --> List<BoardResponse.ListDTO>
//        List<Board> boardList = boardRepository.findAllWithUserByOrderByCreatedAtDesc();
//
//        // 1. 반복문 활용 방법
////        List<BoardResponse.ListDTO> dtoList = new ArrayList<>();
////        for(Board board: boardList) {
////            BoardResponse.ListDTO dto = new BoardResponse.ListDTO(board);
////            dtoList.add(dto);
////        }
////        return dtoList;
//
//        // 2. 람다식 활용 -> 참조메서드
//        return boardList.stream()
//                .map(BoardResponse.ListDTO::new)
//                .toList();
//    }
    public BoardResponse.PageDTO 게시글목록조회(int page, int size, String keyword) {
        // page 는 0 부터 시작
        // 상한선 제한
        // size 는 기본값 5, 최소 1, 최대 50으로 제한
        int validPage = Math.max(0, page); // 페이지 번호가 음수가 되는 것을 막음
        int validSize = Math.max(1, Math.min(50, size));

        // 정렬 기준
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(validPage, validSize, sort);

        Page<Board> boardPage;

        if(keyword != null && !keyword.trim().isEmpty()) {
            boardPage = boardRepository.findByTitleContainingOrContentContaining(keyword.trim(), pageable);
        } else {
            // 검색어 없을 때 사용
            boardPage = boardRepository.findAllWithUserByOrderByCreatedAtDesc(pageable);
        }

        return new BoardResponse.PageDTO(boardPage);
    }

    public BoardResponse.DetailDTO 게시글상세조회(Long boardId, Long userId) {
        Board board = boardRepository.findByIdWithUser(boardId)
                .orElseThrow(() -> new Exception404("해당 게시글을 찾을 수 없습니다."));

            // board 유료글인지 무료글인지

            // 구매 여부 확인 (로그인 사용자가 있는 경우만 확인 가능)
            boolean isPurchased = false;
            if(userId != null) {
                isPurchased = purchaseService.구매여부확인(userId, boardId);
            }
            return new BoardResponse.DetailDTO(board, isPurchased);

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
        replyRepository.deleteByBoardId(boardId); // 제약 오류 발생해서 reply 먼저 삭제해야함
        boardRepository.deleteById(boardId);
    }
}
