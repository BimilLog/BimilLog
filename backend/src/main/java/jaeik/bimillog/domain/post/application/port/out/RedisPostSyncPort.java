package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.PostSearchResult;

import java.util.List;

/**
 * <h2>게시글 캐시 동기화 포트</h2>
 * <p>
 * Post 도메인에서 캐시 데이터 동기화를 위한 DB 조회 작업을 담당하는 포트입니다.
 * </p>
 * <p>인기글 캐시 갱신용 DB 조회</p>
 * <p>실시간, 주간, 레전드 인기글 데이터 조회</p>
 * <p>게시글 상세 정보 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface RedisPostSyncPort {

    /**
     * <h3>실시간 인기 게시글 조회</h3>
     * <p>지난 1일간의 인기 게시글 목록을 데이터베이스에서 조회합니다.</p>
     * <p>PostCacheService의 스케쥴러가 실시간 인기글 캐시 갱신 시 호출됩니다.</p>
     * <p>추천 수 1개 이상인 게시글을 추천 수 내림차순으로 최대 5개까지 조회하여 Redis 캐시 업데이트용 데이터를 제공합니다.</p>
     *
     * @return 실시간 인기 게시글 목록 (최대 5개)
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSearchResult> findRealtimePopularPosts();

    /**
     * <h3>주간 인기 게시글 조회</h3>
     * <p>지난 7일간의 인기 게시글 목록을 데이터베이스에서 조회합니다.</p>
     * <p>PostCacheService의 스케쥴러가 주간 인기글 캐시 갱신 시 호출됩니다.</p>
     * <p>추천 수 1개 이상인 게시글을 추천 수 내림차순으로 최대 5개까지 조회하여 Redis 캐시 업데이트용 데이터를 제공합니다.</p>
     *
     * @return 주간 인기 게시글 목록 (최대 5개)
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSearchResult> findWeeklyPopularPosts();

    /**
     * <h3>전설의 게시글 조회</h3>
     * <p>추천 수 20개 이상인 게시글 중 최고 추천 수를 기록한 게시글 목록을 데이터베이스에서 조회합니다.</p>
     * <p>PostCacheService의 스케쥴러가 레전드 인기글 캐시 갱신 시 호출됩니다.</p>
     * <p>추천 수 20개 이상 조건을 만족하는 게시글을 추천 수 내림차순으로 최대 50개까지 조회하여 Redis 캐시 업데이트용 데이터를 제공합니다.</p>
     *
     * @return 전설의 게시글 목록 (최대 50개)
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSearchResult> findLegendaryPosts();
    

}
