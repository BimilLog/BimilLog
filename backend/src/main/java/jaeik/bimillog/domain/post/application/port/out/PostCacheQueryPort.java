package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * <h2>PostCacheQueryPort</h2>
 * <p>
 * Post 도메인에서 Redis 캐시 데이터의 조회 작업을 처리하는 아웃바운드 포트입니다.
 * 헥사고날 아키텍처에서 Redis 캐시 시스템과의 의존성을 추상화하여 도메인의 순수성을 보장합니다.
 * CQRS 패턴의 Query 측면을 담당하여 캐시 데이터의 읽기 전용 작업에 특화되어 있습니다.
 * </p>
 * <p>
 * 이 포트는 다양한 인기글 캐시 데이터의 조회 기능을 제공합니다:
 * - 인기글 목록 조회: 실시간, 주간, 레전드, 공지사항 목록 캐시 조회
 * - 게시글 상세 캐시 조회: 캐시된 게시글 상세 정보 반환
 * - 페이지네이션 지원: 레전드 게시글의 효율적 페이지 단위 조회
 * - 캐시 상태 확인: 인기글 캐시 존재 여부 확인
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 포트가 필요한 이유:
 * 1. 성능 최적화 - DB 직접 조회 대신 빠른 Redis 캐시 활용
 * 2. 사용자 경험 - 인기글 목록의 즉시 로딩으로 UX 향상
 * 3. 서버 부하 감소 - 반복적인 인기글 조회에서 데이터베이스 부하 경감
 * 4. 실시간성 지원 - 실시간 인기글 데이터의 즉시 제공
 * </p>
 * <p>
 * PostQueryService에서 인기글 목록 조회 시 호출됩니다.
 * PostQueryController에서 인기글 조회 API 요청 처리 시 호출됩니다.
 * PostService에서 게시글 상세 조회 시 캐시 활용 여부 판단을 위해 호출됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCacheQueryPort {

    /**
     * <h3>인기글 목록 캐시 조회</h3>
     * <p>지정된 인기글 유형의 목록 캐시 데이터를 Redis에서 조회합니다.</p>
     * <p>스케줄러에서 미리 계산하여 저장한 인기글 리스트를 빠르게 반환합니다.</p>
     * <p>PostQueryService에서 인기글 목록 요청 시 캐시 우선 조회를 위해 호출됩니다.</p>
     * <p>웹 페이지에서 인기글 섹션에 표시될 데이터를 제공합니다.</p>
     *
     * @param type 조회할 인기글 캐시 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     * @return List<PostSearchResult> Redis에서 조회한 인기글 목록 (캐시 없으면 빈 리스트)
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSearchResult> getCachedPostList(PostCacheFlag type);

    /**
     * <h3>레전드 인기글 페이지네이션 조회</h3>
     * <p>레전드 인기글 목록을 Redis List 구조를 활용하여 효율적으로 페이지 단위로 조회합니다.</p>
     * <p>LRANGE 명령을 사용하여 대량의 레전드 게시글도 메모리 효율적으로 처리합니다.</p>
     * <p>PostQueryService에서 레전드 게시글 목록 요청 시 호출됩니다.</p>
     * <p>레전드 게시글 전용 페이지에서 페이지 단위 내비게이션에 사용됩니다.</p>
     *
     * @param pageable 페이지 정보 (페이지 번호와 크기)
     * @return Page<PostSearchResult> 페이지네이션된 레전드 인기글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> getCachedPostListPaged(Pageable pageable);

    /**
     * <h3>게시글 상세 정보 캐시 조회</h3>
     * <p>특정 게시글의 상세 정보를 Redis 캐시에서 조회하며, 캐시에 존재하지 않으면 null을 반환합니다.</p>
     * <p>캐시된 데이터가 존재하면 해당 게시글은 인기글로 간주되어 추가적인 성능 이점을 얻습니다.</p>
     * <p>PostService에서 게시글 상세 조회 시 캐시 활용 여부 판단을 위해 호출됩니다.</p>
     * <p>인기글의 경우 DB 조회 대신 캐시 데이터를 직접 반환하여 성능을 향상시킵니다.</p>
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
     * <p>캐시 데이터 조회 전에 존재 여부를 미리 확인하여 불필요한 네트워크 호출을 방지합니다.</p>
     * <p>PostQueryService에서 인기글 목록 조회 시 캐시 우선 전략 적용 여부 판단에 사용됩니다.</p>
     * <p>캐시 데이터가 없는 경우 DB에서 실시간 인기글 계산으로 폴백하도록 유도합니다.</p>
     *
     * @param type 확인할 인기글 캐시 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     * @return boolean 캐시 데이터 존재 여부 (true: 존재, false: 빈 또는 없음)
     * @author Jaeik
     * @since 2.0.0
     */
    boolean hasPopularPostsCache(PostCacheFlag type);

}
