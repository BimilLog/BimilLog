package jaeik.bimillog.domain.paper.entity;

import lombok.Builder;

import java.time.Instant;

/**
 * <h2>메시지 상세 정보 값 객체</h2>
 * <p>
 * 내 롤링페이퍼 메시지 조회 결과를 담는 도메인 순수 값 객체
 * </p>
 * <p>PaperQueryService에서 내 롤링페이퍼 조회 시 반환 데이터 구조를 제공</p>
 * <p>메시지의 모든 정보(내용, 익명 이름 등)를 포함하여 소유자에게 제공</p>
 *
 * @param id 메시지 ID
 * @param userId 사용자 ID
 * @param decoType 데코레이션 타입
 * @param anonymity 익명 이름
 * @param content 메시지 내용
 * @param x 메시지 x 좌표
 * @param y 메시지 y 좌표
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
        int x,
        int y,
        Instant createdAt
) {

    @Builder
    public MessageDetail {
    }

    /**
     * <h3>메시지 상세 정보 생성</h3>
     * <p>메시지 엔티티로부터 상세 정보를 생성합니다.</p>
     * <p>PaperQueryService에서 내 롤링페이퍼 조회 시 Message 엔티티 변환을 위해 호출되는 메서드</p>
     *
     * @param message 메시지 엔티티
     * @return MessageDetail 값 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public static MessageDetail from(Message message) {
        return MessageDetail.builder()
                .id(message.getId())
                .userId(message.getUserId())
                .decoType(message.getDecoType())
                .anonymity(message.getAnonymity())
                .content(message.getContent())
                .x(message.getX())
                .y(message.getY())
                .createdAt(message.getCreatedAt())
                .build();
    }
}