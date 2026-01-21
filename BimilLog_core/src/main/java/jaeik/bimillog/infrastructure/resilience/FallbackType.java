package jaeik.bimillog.infrastructure.resilience;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h2>DB Fallback 유형</h2>
 * <p>Redis 장애 시 DB 폴백 요청의 유형을 정의합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Getter
@RequiredArgsConstructor
public enum FallbackType {
    REALTIME("실시간 인기글"),
    WEEKLY("주간 인기글"),
    LEGEND("레전드 인기글"),
    NOTICE("공지사항"),
    DETAIL("인기글 상세");

    private final String description;
}
