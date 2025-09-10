package jaeik.bimillog.domain.post.application.port.in;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jaeik.bimillog.infrastructure.adapter.post.in.web.PostQueryController;

import java.util.List;
import java.util.Map;

/**
 * <h2>게시글 조회 유스케이스</h2>
 * <p>게시글 도메인의 조회 작업을 담당하는 유스케이스입니다.</p>
 * <p>게시글 목록 조회, 상세 조회, 검색</p>
 * <p>인기글 조회, 사용자별 활동 내역 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostQueryUseCase {

    /**
     * <h3>게시판 메인 목록 조회</h3>
     * <p>전체 게시글을 최신순으로 정렬하여 페이지 단위로 조회합니다.</p>
     * <p>공지사항은 제외하고 일반 게시글만 조회</p>
     * <p>{@link PostQueryController}에서 GET /api/post 요청 처리 시 호출됩니다.</p>
     *
     * @param pageable 페이지 정보 (크기, 정렬 기준 포함)
     * @return Page<PostSearchResult> 최신순으로 정렬된 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> getBoard(Pageable pageable);

    /**
     * <h3>게시글 상세 페이지 조회</h3>
     * <p>특정 게시글의 상세 내용과 댓글수, 추천수, 사용자 추천여부를 통합 조회합니다.</p>
     * <p>JOIN 쿼리로 게시글 상세 페이지에 필요한 모든 정보를 한 번에 조회</p>
     * <p>{@link PostQueryController}에서 GET /api/post/{postId} 요청 처리 시 호출됩니다.</p>
     *
     * @param postId 조회할 게시글의 식별자 ID
     * @param userId 현재 로그인한 사용자 ID (추천여부 확인용, null 가능)
     * @return PostDetail 게시글 상세 정보와 연관 데이터
     * @author Jaeik
     * @since 2.0.0
     */
    PostDetail getPost(Long postId, Long userId);

    /**
     * <h3>게시글 전문 검색</h3>
     * <p>검색 타입과 검색어를 기반으로 게시글을 검색합니다.</p>
     * <p>MySQL ngram parser를 활용한 전문 검색으로 한국어 검색 지원</p>
     * <p>{@link PostQueryController}에서 GET /api/post/search 요청 처리 시 호출됩니다.</p>
     *
     * @param type 검색 대상 유형 (title: 제목, content: 내용, author: 작성자)
     * @param query 검색할 키워드 (한국어 단어 지원)
     * @param pageable 페이지 정보 (결과 개수 제한 및 정렬)
     * @return Page<PostSearchResult> 검색 조건에 맞는 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> searchPost(String type, String query, Pageable pageable);

    /**
     * <h3>실시간 및 주간 인기글 동시 조회</h3>
     * <p>실시간과 주간 인기 게시글을 한 번에 조회합니다.</p>
     * <p>Redis 캐시에서 미리 계산된 인기글 데이터 조회</p>
     * <p>{@link PostQueryController}에서 GET /api/post/popular 요청 처리 시 호출됩니다.</p>
     *
     * @return Map<String, List<PostSearchResult>> 인기글 맵 ("realtime": 실시간, "weekly": 주간)
     * @author Jaeik
     * @since 2.0.0
     */
    Map<String, List<PostSearchResult>> getRealtimeAndWeeklyPosts();

    /**
     * <h3>레전드 인기글 목록 페이지네이션 조회</h3>
     * <p>역대 최고 인기글로 선정된 레전드 게시글 목록을 조회합니다.</p>
     * <p>Redis List 구조를 활용하여 페이지네이션 처리</p>
     * <p>{@link PostQueryController}에서 GET /api/post/legend 요청 처리 시 호출됩니다.</p>
     *
     * @param type 조회할 캐시 유형 (PostCacheFlag.LEGEND 고정값)
     * @param pageable 페이지 정보 (페이지 번호, 크기)
     * @return Page<PostSearchResult> 레전드 인기 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> getPopularPostLegend(PostCacheFlag type, Pageable pageable);

    /**
     * <h3>공지사항 목록 전체 조회</h3>
     * <p>관리자가 지정한 공지사항 게시글 목록을 조회합니다.</p>
     * <p>Redis 캐시에서 공지사항 전체 목록을 한 번에 반환</p>
     * <p>{@link PostQueryController}에서 GET /api/post/notice 요청 처리 시 호출됩니다.</p>
     *
     * @return List<PostSearchResult> 캐시된 공지사항 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSearchResult> getNoticePosts();

    /**
     * <h3>크로스 도메인 게시글 엔티티 조회</h3>
     * <p>다른 도메인에서 Post 엔티티가 필요할 때 사용하는 단순 조회 메서드입니다.</p>
     * <p>댓글 작성 시 게시글 존재성 검증, 알림 발송 시 게시글 정보 획득</p>
     * <p>Comment 도메인, Notification 도메인에서 도메인 간 조회 시 호출됩니다.</p>
     *
     * @param postId 조회할 게시글의 식별자 ID
     * @return Post 게시글 엔티티 (예외 발생 시 처리)
     * @author Jaeik
     * @since 2.0.0
     */
    Post findById(Long postId);

    /**
     * <h3>사용자 작성 게시글 내역 조회</h3>
     * <p>특정 사용자가 작성한 모든 게시글 목록을 최신순으로 조회합니다.</p>
     * <p>사용자 프로필 페이지에서 작성 활동 내역 확인 시 사용</p>
     * <p>User 도메인에서 사용자 활동 내역 조회 시 호출됩니다.</p>
     *
     * @param userId 작성글을 조회할 사용자의 식별자 ID
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬)
     * @return Page<PostSearchResult> 사용자가 작성한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> getUserPosts(Long userId, Pageable pageable);

    /**
     * <h3>사용자 추천 게시글 내역 조회</h3>
     * <p>특정 사용자가 추천한 모든 게시글 목록을 최신순으로 조회합니다.</p>
     * <p>사용자 프로필 페이지에서 추천한 게시글 내역 확인 시 사용</p>
     * <p>User 도메인에서 사용자 활동 내역 조회 시 호출됩니다.</p>
     *
     * @param userId 추천글을 조회할 사용자의 식별자 ID
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬)
     * @return Page<PostSearchResult> 사용자가 추천한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> getUserLikedPosts(Long userId, Pageable pageable);
}
