package jaeik.bimillog.domain.paper.entity;

import lombok.Builder;

/**
 * <h3>방문 메시지 상세 정보 값 객체</h3>
 * <p>
 * 다른 사용자 롤링페이퍼 방문시 메시지 조회 결과를 담는 도메인 순수 값 객체
 * VisitMessageDTO의 도메인 전용 대체
 * </p>
 * <p>
 * 방문자에게는 민감한 정보(익명 이름, 내용, 작성일시 등)를 노출하지 않음
 * </p>
 *
 * @param id 메시지 ID
 * @param userId 사용자 ID (작성자가 본인인지 확인용)
 * @param decoType 데코레이션 타입
 * @param width 메시지 너비
 * @param height 메시지 높이
 * @author Jaeik
 * @since 2.0.0
 */
public record VisitMessageDetail(
        Long id,
        Long userId,
        DecoType decoType,
        int width,
        int height
) {

    @Builder
    public VisitMessageDetail {
    }

    /**
     * <h3>방문 메시지 상세 정보 생성</h3>
     * <p>메시지 엔티티로부터 방문용 상세 정보를 생성합니다.</p>
     *
     * @param message 메시지 엔티티
     * @return VisitMessageDetail 값 객체
     */
    public static VisitMessageDetail from(Message message) {
        return VisitMessageDetail.builder()
                .id(message.getId())
                .userId(message.getUserId())
                .decoType(message.getDecoType())
                .width(message.getWidth())
                .height(message.getHeight())
                .build();
    }
}