package jaeik.bimillog.domain.post.event;

import java.util.List;

/**
 * <h2>실시간 인기글 캐시 리빌드 이벤트</h2>
 * <p>ZSet과 JSON LIST의 ID 순서가 불일치할 때 비동기 갱신을 트리거하기 위한 이벤트</p>
 *
 * @param postIds 새로운 실시간 인기글 ID 목록 (ZSet 기준)
 * @author Jaeik
 * @version 2.8.0
 */
public record RealtimeCacheRebuildEvent(List<Long> postIds) {}
