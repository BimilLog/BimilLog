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
public interface PostCacheCommandPort {

    /**
     * <h3>인기글 목록 및 상세 데이터 일괄 캐싱</h3>
     * <p>인기글 목록과 각 게시글의 상세 정보를 Redis에 동시에 저장합니다.</p>
     * <p>PostDetail 데이터에서 PostSearchResult를 추출하여 목록용 캐시를 생성하고,</p>
     * <p>각 PostDetail을 개별 상세 캐시로 저장하여 두 레벨의 캐싱 전략을 제공합니다.</p>
     * <p>PostScheduler에서 주기적인 인기글 데이터 업데이트 시 호출됩니다.</p>
     * <p>PostCacheService에서 인기글 데이터 동기화 시 호출됩니다.</p>
     *
     * @param type 캐시할 인기글 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     * @param fullPosts 인기글 목록과 상세 데이터가 포함된 PostDetail 목록
     * @author Jaeik
     * @since 2.0.0
     */
    void cachePostsWithDetails(PostCacheFlag type, List<PostDetail> fullPosts);

    /**
     * <h3>캐시 데이터 선택적 삭제</h3>
     * <p>인기글 캐시 데이터를 선택적으로 삭제하며, 두 가지 모드를 지원합니다.</p>
     * <p>전체 캐시 삭제: type 지정 시 해당 인기글 리스트와 관련 상세 캐시 전체 제거</p>
     * <p>개별 캐시 삭제: type=null 시 특정 게시글의 캐시만 선택적 제거</p>
     * <p>PostScheduler에서 인기글 데이터 갱신 시 기존 데이터 삭제용으로 호출됩니다.</p>
     * <p>PostService에서 게시글 삭제 시 관련 캐시 데이터 정리를 위해 호출됩니다.</p>
     *
     * @param type 삭제할 캐시 인기글 유형 (null: 개별 삭제 모드, 지정: 전체 삭제 모드)
     * @param postId 삭제대상 게시글 ID (type=null일 때만 사용)
     * @param targetTypes 개별 삭제 시 대상 캐시 유형들 (비어있으면 모든 인기글 캐시 검사)
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteCache(PostCacheFlag type, Long postId, PostCacheFlag... targetTypes);

    /**
     * <h3>게시글 인기 레벨 플래그 일괄 설정</h3>
     * <p>지정된 게시글 ID 목록에 특정 인기 레벨 플래그를 대량 일괄 설정합니다.</p>
     * <p>게시글 엔티티의 popularFlag 필드를 업데이트하여 인기글 분류 상태를 마킹합니다.</p>
     * <p>PostScheduler에서 인기글 계산 완료 후 인기 레벨 플래그 일괄 설정 시 호출됩니다.</p>
     * <p>단일 게시글보다 대량 처리가 필요한 경우에 성능을 위해 사용됩니다.</p>
     *
     * @param postIds 인기 플래그를 적용할 게시글 ID 목록
     * @param postCacheFlag 설정할 인기 레벨 플래그 (예: REALTIME, WEEKLY, LEGEND)
     * @author Jaeik
     * @since 2.0.0
     */
    void applyPopularFlag(List<Long> postIds, PostCacheFlag postCacheFlag);

    /**
     * <h3>인기 레벨 플래그 전체 초기화</h3>
     * <p>지정된 인기 레벨에 해당하는 모든 게시글의 플래그를 일괄 초기화합니다.</p>
     * <p>기존 인기글 선정을 전체적으로 제거하여 새로운 인기글 계산 결과를 반영할 준비를 합니다.</p>
     * <p>PostScheduler에서 인기글 계산 시작 전 기존 플래그 정리를 위해 호출됩니다.</p>
     * <p>인기글 계산 로직의 반복적 실행에서 데이터 일관성을 보장하기 위해 사용됩니다.</p>
     *
     * @param postCacheFlag 초기화할 인기 레벨 플래그
     * @author Jaeik
     * @since 2.0.0
     */
    void resetPopularFlag(PostCacheFlag postCacheFlag);

}
