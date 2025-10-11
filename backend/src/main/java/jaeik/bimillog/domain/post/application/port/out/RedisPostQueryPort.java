package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * <h2>게시글 캐시 조회 포트</h2>
 * <p>Post 도메인의 Redis 캐시 데이터 조회 작업을 담당하는 포트입니다.</p>
 * <p>인기글 목록 캐시 조회</p>
 * <p>게시글 상세 캐시 조회 및 페이지네이션</p>
 * <p>캐시 상태 확인 및 CQRS Query 측면 전담</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface RedisPostQueryPort {

    /**
     * <h3>인기글 목록 캐시 조회</h3>
     * <p>지정된 인기글 유형의 목록 캐시 데이터를 Redis에서 조회합니다.</p>
     * <p>PostQueryService에서 인기글 목록 요청 시 캐시 우선 조회를 위해 호출됩니다.</p>
     *
     * @param type 조회할 인기글 캐시 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     * @return List<PostSimpleDetail> Redis에서 조회한 인기글 목록 (캐시 없으면 빈 리스트)
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSimpleDetail> getCachedPostList(PostCacheFlag type);

    /**
     * <h3>레전드 인기글 페이지네이션 조회</h3>
     * <p>레전드 인기글 목록을 Redis List 구조를 활용하여 페이지 단위로 조회합니다.</p>
     * <p>PostQueryService에서 레전드 게시글 목록 요청 시 호출됩니다.</p>
     *
     * @param pageable 페이지 정보 (페이지 번호와 크기)
     * @return Page<PostSimpleDetail> 페이지네이션된 레전드 인기글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSimpleDetail> getCachedPostListPaged(Pageable pageable);

    /**
     * <h3>게시글 상세 정보 캐시 조회</h3>
     * <p>특정 게시글의 상세 정보를 Redis 캐시에서 조회합니다.</p>
     * <p>PostService에서 게시글 상세 조회 시 캐시 활용 여부 판단을 위해 호출됩니다.</p>
     *
     * @param postId 조회할 게시글의 식별자 ID
     * @return PostDetail 캐시된 게시글 상세 정보 (캐시 없으면 null)
     * @author Jaeik
     * @since 2.0.0
     */
    PostDetail getCachedPostIfExists(Long postId);

    /**
     * <h3>인기글 캐시 데이터 존재 여부 확인</h3>
     * <p>지정된 인기글 유형의 캐시 데이터가 Redis에 존재하는지 확인합니다.</p>
     * <p>PostQueryService에서 인기글 목록 조회 시 캐시 우선 전략 적용 여부 판단에 사용됩니다.</p>
     *
     * @param type 확인할 인기글 캐시 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     * @return boolean 캐시 데이터 존재 여부 (true: 존재, false: 빈 또는 없음)
     * @author Jaeik
     * @since 2.0.0
     */
    boolean hasPopularPostsCache(PostCacheFlag type);

    /**
     * <h3>실시간 인기글 postId 목록 조회</h3>
     * <p>Redis Sorted Set에서 점수가 높은 상위 5개의 게시글 ID를 조회합니다.</p>
     * <p>PostQueryService에서 실시간 인기글 목록 조회 시 호출됩니다.</p>
     *
     * @return List&lt;Long&gt; 상위 5개 게시글 ID 목록 (점수 내림차순)
     * @author Jaeik
     * @since 2.0.0
     */
    List<Long> getRealtimePopularPostIds();

    /**
     * <h3>postIds 영구 저장소에서 ID 목록 조회</h3>
     * <p>캐시 미스 발생 시 복구를 위해 영구 저장된 postId 목록을 조회합니다.</p>
     * <p>PostQueryService에서 목록 캐시 미스 시 DB 조회를 위한 ID 목록 획득에 사용됩니다.</p>
     *
     * @param type 조회할 인기글 캐시 유형 (WEEKLY, LEGEND, NOTICE)
     * @return List&lt;Long&gt; 저장된 게시글 ID 목록 (없으면 빈 리스트)
     * @author Jaeik
     * @since 2.0.0
     */
    List<Long> getStoredPostIds(PostCacheFlag type);

}
