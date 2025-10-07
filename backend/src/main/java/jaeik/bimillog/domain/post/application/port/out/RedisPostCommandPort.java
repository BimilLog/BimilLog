package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;

import java.util.List;

/**
 * <h2>게시글 캐시 명령 포트</h2>
 * <p>Post 도메인의 Redis 캐시 데이터 생성, 수정, 삭제 작업을 담당하는 포트입니다.</p>
 * <p>인기글 캐시 데이터 생성 및 업데이트</p>
 * <p>게시글 상태 변경에 따른 캐시 동기화</p>
 * <p>인기 플래그 설정 및 배치 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface RedisPostCommandPort {

    /**
     * <h3>인기글 postId 영구 저장</h3>
     * <p>인기글 postId 목록만 Redis List에 영구 또는 긴 TTL로 저장합니다.</p>
     * <p>목록 캐시 TTL 만료 시 복구용으로 사용됩니다.</p>
     * <p>PostScheduledService에서 주기적인 인기글 데이터 업데이트 시 호출됩니다.</p>
     * <p>PostAdminService에서 공지사항 설정/해제 시 호출됩니다.</p>
     *
     * @param type 캐시할 인기글 유형 (WEEKLY, LEGEND, NOTICE)
     * @param postIds 캐시할 게시글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    void cachePostIdsOnly(PostCacheFlag type, List<Long> postIds);

    /**
     * <h3>인기글 postId 목록 캐싱</h3>
     * <p>인기글 postId 목록을 Redis List에 저장합니다 (TTL 5분)</p>
     * <p>PostScheduledService에서 주기적인 인기글 데이터 업데이트 시 호출됩니다.</p>
     *
     * @param type 캐시할 인기글 유형 (WEEKLY, LEGEND, NOTICE)
     * @param postIds 캐시할 게시글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    void cachePostIds(PostCacheFlag type, List<Long> postIds);

    /**
     * <h3>캐시 데이터 선택적 삭제</h3>
     * <p>인기글 캐시 데이터를 선택적으로 삭제합니다.</p>
     * <p>전체 캐시 삭제: type 지정 시 해당 인기글 리스트와 관련 상세 캐시 전체 제거</p>
     * <p>개별 캐시 삭제: type=null 시 특정 게시글의 캐시만 선택적 제거</p>
     * <p>PostScheduler에서 인기글 데이터 갱신 시 기존 데이터 삭제용으로 호출됩니다.</p>
     *
     * @param type 삭제할 캐시 인기글 유형 (null: 개별 삭제 모드, 지정: 전체 삭제 모드)
     * @param postId 삭제대상 게시글 ID (type=null일 때만 사용)
     * @param targetTypes 개별 삭제 시 대상 캐시 유형들 (비어있으면 모든 인기글 캐시 검사)
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteCache(PostCacheFlag type, Long postId, PostCacheFlag... targetTypes);

    /**
     * <h3>단일 게시글 상세 정보 캐싱</h3>
     * <p>게시글 상세 정보를 Redis 캐시에 저장합니다.</p>
     * <p>PostQueryService에서 캐시 어사이드 패턴으로 DB 조회 후 캐시 저장 시 호출됩니다.</p>
     *
     * @param postDetail 캐시할 게시글 상세 정보
     * @author Jaeik
     * @since 2.0.0
     */
    void cachePostDetail(PostDetail postDetail);

    /**
     * <h3>단일 게시글 캐시 무효화</h3>
     * <p>특정 게시글의 캐시 데이터를 Redis에서 삭제합니다.</p>
     * <p>PostCommandService에서 라이트 어라운드 패턴으로 게시글 수정/삭제 시 호출됩니다.</p>
     *
     * @param postId 캐시를 삭제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteSinglePostCache(Long postId);

    /**
     * <h3>실시간 인기글 점수 증가</h3>
     * <p>Redis Sorted Set에서 특정 게시글의 점수를 증가시킵니다.</p>
     * <p>이벤트 리스너에서 조회/댓글/추천 이벤트 발생 시 호출됩니다.</p>
     *
     * @param postId 점수를 증가시킬 게시글 ID
     * @param score 증가시킬 점수 (조회: 2점, 댓글: 3점, 추천: 4점)
     * @author Jaeik
     * @since 2.0.0
     */
    void incrementRealtimePopularScore(Long postId, double score);

    /**
     * <h3>실시간 인기글 전체 점수 지수감쇠 적용</h3>
     * <p>Redis Sorted Set의 모든 게시글 점수에 0.9를 곱하고, 임계값(1점) 이하의 게시글을 제거합니다.</p>
     * <p>PostScheduledService 스케줄러에서 5분마다 호출됩니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    void applyRealtimePopularScoreDecay();

    /**
     * <h3>postIds 저장소에 단일 게시글 추가</h3>
     * <p>postIds 영구 저장소의 앞에 게시글 ID를 추가합니다 (LPUSH).</p>
     * <p>공지사항 설정 시 호출됩니다.</p>
     *
     * @param type 캐시 유형 (NOTICE만 사용)
     * @param postId 추가할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void addPostIdToStorage(PostCacheFlag type, Long postId);

    /**
     * <h3>postIds 저장소에서 단일 게시글 제거</h3>
     * <p>postIds 영구 저장소에서 게시글 ID를 제거합니다 (LREM).</p>
     * <p>공지사항 해제 시 호출됩니다.</p>
     *
     * @param type 캐시 유형 (NOTICE만 사용)
     * @param postId 제거할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void removePostIdFromStorage(PostCacheFlag type, Long postId);

}
