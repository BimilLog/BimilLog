package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.application.service.PostQueryService;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.PostSearchType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
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
     * <h3>전체 게시글 목록 페이지네이션 조회</h3>
     * <p>전체 게시글을 최신순으로 정렬하여 페이지 단위로 조회합니다.</p>
     * <p>공지사항은 제외하고 일반 게시글만 조회</p>
     * <p>{@link PostQueryService}에서 게시판 메인 목록 조회 시 호출됩니다.</p>
     *
     * @param pageable 페이지 정보 (크기, 정렬 기준 포함)
     * @return Page<PostSimpleDetail> 최신순으로 정렬된 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSimpleDetail> findByPage(Pageable pageable);

    /**
     * <h3>사용자 작성 게시글 목록 조회</h3>
     * <p>특정 사용자가 작성한 게시글 목록을 최신순으로 조회합니다.</p>
     * <p>사용자별 활동 내역 추적 및 개인 포트폴리오 구성 시 사용</p>
     * <p>{@link PostQueryService}에서 사용자 작성 게시글 내역 조회 시 호출됩니다.</p>
     *
     * @param memberId   조회할 사용자의 ID
     * @param pageable 페이지 정보 (크기, 번호, 정렬 포함)
     * @return 사용자가 작성한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSimpleDetail> findPostsByMemberId(Long memberId, Pageable pageable);

    /**
     * <h3>사용자 추천 게시글 목록 조회</h3>
     * <p>특정 사용자가 추천(좋아요)한 게시글 목록을 추천 시간 순으로 조회합니다.</p>
     * <p>PostLike 테이블과 Post 테이블을 JOIN하여 추천 시간 내림차순으로 조회</p>
     * <p>{@link PostQueryService}에서 사용자 추천 게시글 내역 조회 시 호출됩니다.</p>
     *
     * @param memberId   조회할 사용자의 ID
     * @param pageable 페이지 정보 (크기, 번호, 정렬 포함)
     * @return 사용자가 추천한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSimpleDetail> findLikedPostsByMemberId(Long memberId, Pageable pageable);

    /**
     * <h3>MySQL FULLTEXT 전문 검색</h3>
     * <p>MySQL FULLTEXT 인덱스를 사용하여 게시글을 검색합니다.</p>
     * <p>ngram parser를 활용한 한국어 검색 지원</p>
     * <p>{@link PostQueryService}에서 검색어가 3글자 이상일 때 우선적으로 호출됩니다.</p>
     *
     * @param type 검색 타입 (TITLE, TITLE_CONTENT)
     * @param query 검색어 (3글자 이상)
     * @param pageable 페이지 정보
     * @return Page<PostSimpleDetail> 검색 결과 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSimpleDetail> findByFullTextSearch(PostSearchType type, String query, Pageable pageable);

    /**
     * <h3>접두사 검색 (인덱스 활용)</h3>
     * <p>LIKE 'query%' 조건으로 검색하여 인덱스를 활용합니다.</p>
     * <p>주로 작성자 검색에서 4글자 이상일 때 사용</p>
     * <p>{@link PostQueryService}에서 WRITER 검색 시 호출됩니다.</p>
     *
     * @param type 검색 타입 (주로 WRITER)
     * @param query 검색어
     * @param pageable 페이지 정보
     * @return Page<PostSimpleDetail> 검색 결과 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSimpleDetail> findByPrefixMatch(PostSearchType type, String query, Pageable pageable);

    /**
     * <h3>부분 문자열 검색 (인덱스 미활용)</h3>
     * <p>LIKE '%query%' 조건으로 부분 검색을 수행합니다.</p>
     * <p>전문 검색 실패 시 폴백으로 사용</p>
     * <p>{@link PostQueryService}에서 다른 검색 방식이 적합하지 않을 때 호출됩니다.</p>
     *
     * @param type 검색 타입 (TITLE, WRITER, TITLE_CONTENT)
     * @param query 검색어
     * @param pageable 페이지 정보
     * @return Page<PostSimpleDetail> 검색 결과 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSimpleDetail> findByPartialMatch(PostSearchType type, String query, Pageable pageable);


    /**
     * <h3>게시글 통합 상세 정보 조회</h3>
     * <p>게시글과 관련된 모든 정보(좋아요 수, 댓글 수, 사용자별 좋아요 여부)를 JOIN으로 한 번에 조회합니다.</p>
     * <p>{@link PostQueryService}에서 게시글 상세 페이지 조회 시 호출됩니다.</p>
     *
     * @param postId 조회할 게시글 ID
     * @param memberId 현재 로그인한 사용자 ID (좋아요 여부 확인용, 비로그인 시 null)
     * @return 게시글 상세 정보 객체 (게시글이 존재하지 않으면 empty)
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<PostDetail> findPostDetailWithCounts(Long postId, Long memberId);

    /**
     * <h3>사용자가 작성한 postId 조회</h3>
     * <p>사용자가 작성한 글의 postId 목록을 조회합니다.</p>
     *
     * @param memberId 게시글을 조회할 사용자 ID
     * @return List<Long> 사용자의 게시글 ID 목록
     */
    List<Long> findPostIdsMemberId(Long memberId);

    /**
     * <h3>주간 인기 게시글 조회</h3>
     * <p>지난 7일간의 인기 게시글 목록을 데이터베이스에서 조회합니다.</p>
     * <p>PostCacheSyncService의 스케쥴러가 주간 인기글 캐시 갱신 시 호출됩니다.</p>
     * <p>추천 수 1개 이상인 게시글을 추천 수 내림차순으로 최대 5개까지 조회하여 Redis 캐시 업데이트용 데이터를 제공합니다.</p>
     *
     * @return 주간 인기 게시글 목록 (최대 5개)
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSimpleDetail> findWeeklyPopularPosts();

    /**
     * <h3>전설의 게시글 조회</h3>
     * <p>추천 수 20개 이상인 게시글 중 최고 추천 수를 기록한 게시글 목록을 데이터베이스에서 조회합니다.</p>
     * <p>PostCacheSyncService의 스케쥴러가 레전드 인기글 캐시 갱신 시 호출됩니다.</p>
     * <p>추천 수 20개 이상 조건을 만족하는 게시글을 추천 수 내림차순으로 최대 50개까지 조회하여 Redis 캐시 업데이트용 데이터를 제공합니다.</p>
     *
     * @return 전설의 게시글 목록 (최대 50개)
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSimpleDetail> findLegendaryPosts();

}
