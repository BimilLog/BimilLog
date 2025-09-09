package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * <h2>PostQueryPort</h2>
 * <p>
 * 게시글 조회 기능을 담당하는 아웃바운드 포트입니다.
 * 헥사고날 아키텍처에서 게시글 읽기 관련 외부 의존성을 추상화하여 도메인 로직의 순수성을 보장합니다.
 * CQRS 패턴에 따른 조회 전용 포트로 읽기 작업에 특화되어 있습니다.
 * </p>
 * <p>
 * 이 포트는 다양한 게시글 조회 기능을 제공합니다:
 * - 개별 게시글 조회: ID를 통한 특정 게시글 검색
 * - 게시글 목록 조회: 페이지네이션을 통한 효율적 목록 조회
 * - 검색 기능: 제목/내용/작성자 기반 게시글 검색
 * - 사용자별 활동: 작성한 게시글과 추천한 게시글 조회
 * - 통합 상세 조회: JOIN을 통한 게시글 상세 정보 일괄 조회
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 포트가 필요한 이유:
 * 1. 사용자 경험 개선 - 다양한 조회 옵션과 빠른 검색 제공
 * 2. 성능 최적화 - 페이지네이션과 JOIN을 통한 효율적 데이터 조회
 * 3. 개인화 서비스 - 사용자별 게시글 활동 내역 제공
 * 4. 커뮤니티 운영 - 검색과 분류를 통한 콘텐츠 관리
 * </p>
 * <p>
 * PostQueryController에서 게시글 목록 조회 API 제공 시 사용됩니다.
 * PostService에서 게시글 상세 정보 조회 시 사용됩니다.
 * UserService에서 사용자 활동 내역 조회 시 사용됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostQueryPort {

    /**
     * <h3>게시글 ID로 단일 게시글 조회</h3>
     * <p>특정 ID에 해당하는 게시글 엔티티를 조회합니다.</p>
     * <p>게시글 수정, 삭제, 상세 조회 등의 기본 CRUD 작업에서 사용됩니다.</p>
     * <p>PostService에서 게시글 수정/삭제 권한 확인 시 호출됩니다.</p>
     * <p>PostQueryController에서 개별 게시글 상세 정보 조회 시 호출됩니다.</p>
     * <p>CommentService에서 댓글 작성 전 게시글 존재성 검증 시 호출됩니다.</p>
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
     * <p>커뮤니티 메인 페이지에서 모든 게시글을 표시할 때 사용됩니다.</p>
     * <p>PostQueryController에서 게시글 목록 API 제공 시 호출됩니다.</p>
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
     * <p>MySQL의 ngram parser를 활용한 전문 검색으로 한국어 검색을 지원합니다.</p>
     * <p>검색 결과는 최신순으로 정렬되어 페이지네이션으로 제공됩니다.</p>
     * <p>PostQueryController에서 게시글 검색 API 제공 시 호출됩니다.</p>
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
     * <p>특정 사용자가 작성한 게시글 목록을 최신순으로 페이지네이션하여 조회합니다.</p>
     * <p>PostQueryController에서 사용자 프로필 페이지의 '작성한 글' 탭 API 제공 시 호출됩니다.</p>
     * <p>사용자별 활동 내역을 추적하고 개인 포트폴리오를 구성하는 데 사용됩니다.</p>
     * <p>해당 사용자 ID로 작성된 모든 게시글을 작성 시간 내림차순으로 정렬하여 반환합니다.</p>
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
     * <p>특정 사용자가 추천(좋아요)한 게시글 목록을 추천 시간 순으로 페이지네이션하여 조회합니다.</p>
     * <p>PostQueryController에서 사용자 프로필 페이지의 '좋아한 글' 탭 API 제공 시 호출됩니다.</p>
     * <p>사용자의 관심사와 취향을 파악할 수 있는 데이터를 제공하여 개인화 서비스에 활용됩니다.</p>
     * <p>PostLike 테이블과 Post 테이블을 JOIN하여 해당 사용자가 추천한 게시글들을 추천 시간 내림차순으로 조회합니다.</p>
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
     * <p>게시글과 관련된 모든 정보(좋아요 수, 댓글 수, 사용자별 좋아요 여부)를 JOIN을 통해 한 번의 쿼리로 조회합니다.</p>
     * <p>PostQueryController에서 게시글 상세 페이지 API 제공 시 호출됩니다.</p>
     * <p>기존의 개별 쿼리들(게시글 조회 + 좋아요 수 + 댓글 수 + 좋아요 여부)을 1개 JOIN 쿼리로 최적화하여 성능을 대폭 개선합니다.</p>
     * <p>PostDetail 객체로 게시글 엔티티와 집계 정보, 사용자별 상호작용 정보를 함께 제공합니다.</p>
     *
     * @param postId 조회할 게시글 ID
     * @param userId 현재 로그인한 사용자 ID (좋아요 여부 확인용, 비로그인 시 null)
     * @return 게시글 상세 정보 객체 (게시글이 존재하지 않으면 empty)
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<PostDetail> findPostDetailWithCounts(Long postId, Long userId);

}
