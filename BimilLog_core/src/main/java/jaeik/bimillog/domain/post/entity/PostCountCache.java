package jaeik.bimillog.domain.post.entity;

/**
 * <h2>카운터 캐시 DTO</h2>
 * <p>{@code post:counters} Hash에서 HMGET으로 조회한 카운트 값을 담습니다.</p>
 * <p>{@link PostCacheEntry}와 결합하여 {@link PostSimpleDetail}을 생성합니다.</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
public record PostCountCache(int viewCount, int likeCount, int commentCount) {

    public static final PostCountCache ZERO = new PostCountCache(0, 0, 0);
}
