package jaeik.bimillog.domain.paper.entity;

import lombok.Builder;
import java.time.Instant;

/**
 * <h3>메시지 상세 정보 값 객체</h3>
 * <p>
 * 내 롤링페이퍼 메시지 조회 결과를 담는 도메인 순수 값 객체
 * MessageDTO의 도메인 전용 대체
 * </p>
 *
 * @param id 메시지 ID
 * @param userId 사용자 ID
 * @param decoType 데코레이션 타입
 * @param anonymity 익명 이름
 * @param content 메시지 내용
 * @param width 메시지 너비
 * @param height 메시지 높이
 * @param createdAt 작성일시
 * @author Jaeik
 * @since 2.0.0
 */
public record MessageDetail(
        Long id,
        Long userId,
        DecoType decoType,
        String anonymity,
        String content,
        int width,
        int height,
        Instant createdAt
) {

    @Builder
    public MessageDetail {
    }

    /**
     * <h3>메시지 상세 정보 생성</h3>
     * <p>메시지 엔티티로부터 상세 정보를 생성합니다.</p>
     *
     * @param message 메시지 엔티티
     * @return MessageDetail 값 객체
     */
    public static MessageDetail from(Message message) {
        return MessageDetail.builder()
                .id(message.getId())
                .userId(message.getUserId())
                .decoType(message.getDecoType())
                .anonymity(message.getAnonymity())
                .content(message.getContent())
                .width(message.getWidth())
                .height(message.getHeight())
                .createdAt(message.getCreatedAt())
                .build();
    }
}