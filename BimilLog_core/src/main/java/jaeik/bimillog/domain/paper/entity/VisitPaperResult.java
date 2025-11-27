package jaeik.bimillog.domain.paper.entity;

import java.util.List;

/**
 * <h3>다른 사용자 롤링페이퍼 방문 조회 결과</h3>
 * <p>메시지 리스트와 소유자 ID를 포함하는 레코드</p>
 */
public record VisitPaperResult(List<VisitMessage> messages, Long ownerId) {}