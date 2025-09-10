package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.application.service.PostQueryService;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * <h2>게시글 조회 포트</h2>
 * <p>게시글 도메인의 조회 작업을 담당하는 포트입니다.</p>
 * <p>게시글 개별 조회, 목록 조회, 검색</p>
 * <p>사용자별 작성글, 추천글 조회</p>
 * <p>JOIN을 통한 게시글 상세 정보 통합 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostQueryPort {

    /**
     * <h3>게시글 ID로 단일 게시글 조회</h3>
     * <p>특정 ID에 해당하는 게시글 엔티티를 조회합니다.</p>
     * <p>게시글 수정, 삭제, 상세 조회 등의 기본 CRUD 작업 시 사용</p>
     * <p>{@link PostQueryService}에서 게시글 존재성 검증 및 권한 확인 시 호출됩니다.</p>
     *
     * @param id 조회할 게시글 ID
     * @return Post 조회된 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    Post findById(Long id);

    /**
     * <h3>전체 게시글 목록 페이지네이션 조회</h3>
     * <p>전체 게시글을 최신순으로 정렬하여 페이지 단위로 조회합니다.</p>
     * <p>공지사항은 제외하고 일반 게시글만 조회</p>
     * <p>{@link PostQueryService}에서 게시판 메인 목록 조회 시 호출됩니다.</p>
     *
     * @param pageable 페이지 정보 (크기, 정렬 기준 포함)
     * @return Page<PostSearchResult> 최신순으로 정렬된 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> findByPage(Pageable pageable);

    /**
     * <h3>조건별 게시글 검색</h3>
     * <p>검색 타입(제목, 내용, 작성자)과 검색어를 기반으로 게시글을 검색합니다.</p>
     * <p>MySQL ngram parser를 활용한 전문 검색으로 한국어 검색 지원</p>
     * <p>{@link PostQueryService}에서 게시글 전문 검색 처리 시 호출됩니다.</p>
     *
     * @param type 검색 타입 (title, content, author 등)
     * @param query 검색어 (한국어 키워드)
     * @param pageable 페이지 정보
     * @return Page<PostSearchResult> 검색 조건에 맞는 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> findBySearch(String type, String query, Pageable pageable);

    /**
     * <h3>사용자 작성 게시글 목록 조회</h3>
     * <p>특정 사용자가 작성한 게시글 목록을 최신순으로 조회합니다.</p>
     * <p>사용자별 활동 내역 추적 및 개인 포트폴리오 구성 시 사용</p>
     * <p>{@link PostQueryService}에서 사용자 작성 게시글 내역 조회 시 호출됩니다.</p>
     *
     * @param userId   조회할 사용자의 ID
     * @param pageable 페이지 정보 (크기, 번호, 정렬 포함)
     * @return 사용자가 작성한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> findPostsByUserId(Long userId, Pageable pageable);

    /**
     * <h3>사용자 추천 게시글 목록 조회</h3>
     * <p>특정 사용자가 추천(좋아요)한 게시글 목록을 추천 시간 순으로 조회합니다.</p>
     * <p>PostLike 테이블과 Post 테이블을 JOIN하여 추천 시간 내림차순으로 조회</p>
     * <p>{@link PostQueryService}에서 사용자 추천 게시글 내역 조회 시 호출됩니다.</p>
     *
     * @param userId   조회할 사용자의 ID
     * @param pageable 페이지 정보 (크기, 번호, 정렬 포함)
     * @return 사용자가 추천한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> findLikedPostsByUserId(Long userId, Pageable pageable);

    /**
     * <h3>게시글 통합 상세 정보 조회</h3>
     * <p>게시글과 관련된 모든 정보(좋아요 수, 댓글 수, 사용자별 좋아요 여부)를 JOIN으로 한 번에 조회합니다.</p>
     * <p>4개 개별 쿼리를 1개 JOIN 쿼리로 처리하여 데이터베이스 접근 회수 감소</p>
     * <p>{@link PostQueryService}에서 게시글 상세 페이지 조회 시 호출됩니다.</p>
     *
     * @param postId 조회할 게시글 ID
     * @param userId 현재 로그인한 사용자 ID (좋아요 여부 확인용, 비로그인 시 null)
     * @return 게시글 상세 정보 객체 (게시글이 존재하지 않으면 empty)
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<PostDetail> findPostDetailWithCounts(Long postId, Long userId);

}
