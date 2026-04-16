package jaeik.bimillog.infrastructure.redis.member;

import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import org.springframework.data.domain.Page;

/**
 * <h2>멤버 캐시 조회 결과</h2>
 * <p>PER(Probabilistic Early Recomputation) Lua 스크립트 반환값을 Java로 표현</p>
 * <ul>
 *   <li>HIT: 캐시 히트 - 데이터를 그대로 반환</li>
 *   <li>EARLY_REFRESH: PER 트리거 - 락 없이 DB 조회 후 갱신 필요</li>
 *   <li>MISS: 캐시 미스 - 싱글플라이트 흐름으로 처리</li>
 * </ul>
 */
public record MemberCacheResult(Type type, Page<SimpleMemberDTO> data) {

    public enum Type {
        HIT,
        EARLY_REFRESH,
        MISS
    }

    public static MemberCacheResult hit(Page<SimpleMemberDTO> data) {
        return new MemberCacheResult(Type.HIT, data);
    }

    public static MemberCacheResult earlyRefresh() {
        return new MemberCacheResult(Type.EARLY_REFRESH, Page.empty());
    }

    public static MemberCacheResult miss() {
        return new MemberCacheResult(Type.MISS, Page.empty());
    }
}
