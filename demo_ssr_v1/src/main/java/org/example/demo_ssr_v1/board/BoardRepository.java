package org.example.demo_ssr_v1.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {
    // 자동 제공 메서드 (별도 구현 없이 사용 가능)
    // - save(T entity): (Insert 또는 Update)
    // - findById(ID id): id 로 엔티티 조회(Optional<T>)
    // - findAll()
    // - deleteById(Id id): ID로 엔티티 삭제
    // - count(): 전체 개수 조회
    // - existsById(Id id): ID 존재 여부 확인

    // 전체 조회
    // SELECT * FROM board_tb ORDER BY created_at DESC;
    // List<Board> findAllByOrderByCreatedAtDesc();

    // LAZY 로딩이라서 한번에 username 을 가져와야함
//    @Query("""
//            SELECT b FROM Board b
//            JOIN FETCH b.user
//            ORDER BY b.createdAt DESC
//""")
//    List<Board> findAllWithUserByOrderByCreatedAtDesc();

    // 게시글 전체 조회(페이징 처리) - 인수값은 우리가 생성한 Pageable 객체를 넣고
    // - 리턴타입은 List 대신 Page 객체로 반환
    /**
     * SELECT 절에 DISTINCT 를 사용하면 정확한 count를 가져올 수 있음
     * JOIN FETCH 때문에 하이버네이트가 쿼리를 이상하게 작성하는 것을 막는 처리
     * countQuery -전체 게시글에 개수를 빠르게 가져오기 위해 사용한다. 성능 문제
     * @param pageable
     * @return
     */
    @Query(
            value = """
        SELECT DISTINCT b FROM Board b
        JOIN FETCH b.user
        ORDER BY b.createdAt DESC
    """,
            countQuery = """
        SELECT COUNT(b) FROM Board b
    """
    )
    Page<Board> findAllWithUserByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 게시글 검색(제목 또는 내용, 페이징 포함)
     * @param keyword
     * @param pageable
     * @return
     */
    @Query(
            value = """
        SELECT DISTINCT b FROM Board b
        WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY b.createdAt DESC
    """,
            countQuery = """
        SELECT COUNT(DISTINCT b) FROM Board b
        WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """
    )
    Page<Board> findByTitleContainingOrContentContaining(@Param("keyword") String keyword, Pageable pageable);


    // 게시글 ID로 조회(작성자 정보 포함)
    @Query("""
            SELECT b FROM Board b
            JOIN FETCH b.user
            WHERE b.id = :id
""")
    Optional<Board> findByIdWithUser(@Param("id") Long id);
}
