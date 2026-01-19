package jaeik.bimillog.domain.post.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <h2>캐시된 게시글 래퍼</h2>
 * <p>PostSimpleDetail을 감싸며 만료 시간 정보를 포함합니다.</p>
 * <p>PER(Probabilistic Early Refresh)을 위한 확률적 갱신 판단 로직을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Getter
@NoArgsConstructor
public class CachedPost implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * PER의 expiry gap (초 단위)
     * <p>TTL 마지막 60초 동안 확률적으로 캐시를 갱신합니다.</p>
     */
    private static final int EXPIRY_GAP_SECONDS = 60;

    /**
     * 캐시된 게시글 데이터
     */
    private PostSimpleDetail data;

    /**
     * 캐시 만료 예정 시간
     */
    private Instant expiresAt;

    /**
     * <h3>CachedPost 생성자</h3>
     *
     * @param data 게시글 데이터
     * @param ttl  캐시 유지 시간
     */
    public CachedPost(PostSimpleDetail data, Duration ttl) {
        this.data = data;
        this.expiresAt = Instant.now().plus(ttl);
    }

    /**
     * <h3>남은 TTL 계산</h3>
     *
     * @return 남은 시간 (초 단위), 음수일 경우 이미 만료됨
     */
    public long remainingSeconds() {
        return Duration.between(Instant.now(), expiresAt).toSeconds();
    }

    /**
     * <h3>PER 기반 갱신 필요 여부 판단</h3>
     * <p>TTL 마지막 60초 동안 확률적으로 true를 반환합니다.</p>
     * <p>남은 시간이 적을수록 갱신 확률이 높아집니다.</p>
     *
     * @return 갱신이 필요하면 true
     */
    public boolean shouldRefresh() {
        long remaining = remainingSeconds();
        if (remaining <= 0) {
            return true; // 이미 만료됨
        }
        if (remaining < EXPIRY_GAP_SECONDS) {
            // 남은 시간이 적을수록 갱신 확률 증가
            double randomFactor = ThreadLocalRandom.current().nextDouble();
            return remaining - (randomFactor * EXPIRY_GAP_SECONDS) <= 0;
        }
        return false;
    }

    /**
     * <h3>게시글 ID 조회</h3>
     *
     * @return 게시글 ID
     */
    public Long getPostId() {
        return data != null ? data.getId() : null;
    }
}
