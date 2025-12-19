package org.example.demo_ssr_v1.board;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.demo_ssr_v1.reply.ReplyResponse;
import org.example.demo_ssr_v1.reply.ReplyService;
import org.example.demo_ssr_v1.user.User;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller// IoC
@RequiredArgsConstructor // DI
public class BoardController {

    private final BoardService boardService;
    private final ReplyService replyService;

    /**
     * 게시글 수정 화면 요청
     * @param id
     * @param model
     * @param session
     * @return
     */
    @GetMapping("/board/{id}/update")
    public String updateForm(@PathVariable Long id, Model model, HttpSession session) {
        // 1. 인증 검사 (O)
        User sessionUser = (User) session.getAttribute("sessionUser"); // sessionUser -> 상수만드는게 좋음
//        if(sessionUser == null) {
//            throw new Exception401("로그인먼저 해주세요");
//        }

        BoardResponse.UpdateFormDTO dto = boardService.게시글수정화면(id, sessionUser.getId());
        model.addAttribute("board", dto);

        return "board/update-form";
    }

    /**
     * 게시글 수정 요청 기능
     * @param id
     * @param updateDTO
     * @param session
     * @return
     */
    @PostMapping("/board/{id}/update")
    public String updateProc(@PathVariable Long id, BoardRequest.UpdateDTO updateDTO, HttpSession session) {

        // 1. 인증 처리
        User sessionUser = (User) session.getAttribute("sessionUser");
//        if(sessionUser == null) {
//            throw new Exception401("로그인이 필요합니다.");
//        }

        // 2. 인가 처리
        //  조회
        updateDTO.validate();
        boardService.게시글수정(updateDTO, id, sessionUser.getId());
        return "redirect:/board/list";
    }

    /** TODO - 삭제 예정
     * 게시글 목록 화면 요청
     * @param model
     * @return
     */
//    @GetMapping({"/board/list", "/"})
//    public String boardList(Model model) {
//        List<BoardResponse.ListDTO> boardList = boardService.게시글목록조회();
//        model.addAttribute("boardList", boardList);
//
//        return "board/list";
//    }

    /**
     * 게시글 목록 페이징 처리 기능 추가
     * @param model
     * @return
     * // 예시: /board/list?page=1&size=5
     */
    @GetMapping({"/board/list", "/"})
    //@ResponseBody // 뷰 리졸브(X) 데이터 반환
    public String boardList(Model model,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "3") int size,
                                 @RequestParam(required = false) String keyword) {

        // 1. 페이지 번호 변환: 사용자는 1부터 시작하는 페이지 번호를 사용하지만
        //  Spring의 Pageable 은 0 부터 시작하므로 1을 빼서 변환해야함
        int pageIndex = Math.max(0, page - 1);

//        return boardService.게시글목록조회(pageIndex, size);
        BoardResponse.PageDTO boardPage = boardService.게시글목록조회(pageIndex, size, keyword);
        model.addAttribute("keyword", keyword);
        model.addAttribute("boardPage", boardPage);

        return "board/list";
    }

    @GetMapping("/board/save")
    public String saveForm() {
        return "board/save-form";
    }

    /**
     * 게시글 작성 요청 기능
     * @param saveDTO
     * @param session
     * @return
     */
    @PostMapping("/board/save")
    public String saveProc(BoardRequest.SaveDTO saveDTO, HttpSession session) {
        // 1. 인증 처리 확인
        User sessionUser = (User) session.getAttribute("sessionUser");
//        if(sessionUser == null) {
//            throw new Exception401("로그인이 필요합니다.");
//        } -- > 인터셉터
        // 2. 유효성 검사 (형식) - controller, 논리적 검사 - service
        boardService.게시글작성(saveDTO, sessionUser);
        return "redirect:/";
    }

    /**
     * 게시글 삭제 요청 기능
     * @param id
     * @param session
     * @return
     */
    @PostMapping("/board/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        // 1. 인증 처리
        User sessionUser = (User) session.getAttribute("sessionUser");
        boardService.게시글삭제(id, sessionUser.getId());
        return "redirect:/";
    }

    /**
     * 게시글 상세 보기 화면 요청
     * @param id
     * @param model
     * @return
     */
    @GetMapping("/board/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        BoardResponse.DetailDTO board = boardService.게시글상세조회(id);

        // 세션에 로그인 사용자 정보 조회(없을 수 도 있음)
        User sessionUser = (User) session.getAttribute("sessionUser");
        boolean isOwner = false;

        if(sessionUser != null && board.getUserId() !=null) {
            isOwner = board.getUserId().equals(sessionUser.getId());
        }

        // 댓글 목록 조회 (추가)
        // 로그인 안 한 상태에서 댓글 목록 요청시에 sessionUserId 는 null 값임
        Long sessionUserId = sessionUser != null ? sessionUser.getId() : null;
        List<ReplyResponse.ListDTO> replyList = replyService.댓글목록조회(id, sessionUserId);

        model.addAttribute("replyList", replyList);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("board", board);

        return "board/detail";
    }

}
