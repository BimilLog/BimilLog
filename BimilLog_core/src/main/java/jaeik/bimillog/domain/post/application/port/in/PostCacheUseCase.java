package jaeik.bimillog.domain.post.application.port.in;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.in.post.web.PostCacheController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * <h2>게시글 캐시 유스케이스</h2>
 * <p>Post 도메인의 캐시 데이터 조회 및 동기화 작업을 담당하는 유스케이스입니다.</p>
 * <p>실시간, 주간, 레전드 인기글 조회</p>
 * <p>공지사항 조회 및 상태 변경에 따른 캐시 동기화</p>
 * <p>Redis와 DB 데이터의 일관성 유지</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCacheUseCase {

    /**
     * <h3>실시간 인기글 조회</h3>
     * <p>실시간 인기 게시글 목록을 조회합니다.</p>
     * <p>Redis Sorted Set에서 postId 목록 조회 후 캐시 어사이드 패턴으로 상세 정보 획득</p>
     * <p>{@link PostCacheController}에서 GET /api/post/popular/realtime 요청 처리 시 호출됩니다.</p>
     *
     * @return List<PostSimpleDetail> 실시간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSimpleDetail> getRealtimePosts();

    /**
     * <h3>주간 인기글 조회</h3>
     * <p>주간 인기 게시글 목록을 조회합니다.</p>
     * <p>Redis 캐시에서 미리 계산된 주간 인기글 데이터 조회</p>
     * <p>{@link PostCacheController}에서 GET /api/post/popular/weekly 요청 처리 시 호출됩니다.</p>
     *
     * @return List<PostSimpleDetail> 주간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSimpleDetail> getWeeklyPosts();

    /**
     * <h3>레전드 인기글 목록 페이지네이션 조회</h3>
     * <p>역대 최고 인기글로 선정된 레전드 게시글 목록을 조회합니다.</p>
     * <p>Redis List 구조로 페이지네이션을 처리합니다.</p>
     * <p>{@link PostCacheController}에서 GET /api/post/legend 요청 처리 시 호출됩니다.</p>
     *
     * @param type 조회할 캐시 유형 (PostCacheFlag.LEGEND 고정값)
     * @param pageable 페이지 정보 (페이지 번호, 크기)
     * @return Page<PostSimpleDetail> 레전드 인기 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSimpleDetail> getPopularPostLegend(PostCacheFlag type, Pageable pageable);

    /**
     * <h3>공지사항 목록 전체 조회</h3>
     * <p>관리자가 지정한 공지사항 게시글 목록을 조회합니다.</p>
     * <p>Redis 캐시에서 공지사항 전체 목록을 한 번에 반환</p>
     * <p>{@link PostCacheController}에서 GET /api/post/notice 요청 처리 시 호출됩니다.</p>
     *
     * @return List<PostSimpleDetail> 캐시된 공지사항 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSimpleDetail> getNoticePosts();
}