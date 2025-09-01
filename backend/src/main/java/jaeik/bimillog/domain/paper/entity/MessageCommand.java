package jaeik.bimillog.domain.paper.entity;

import lombok.Builder;

/**
 * <h3>메시지 명령 값 객체</h3>
 * <p>
 * 메시지 생성/수정/삭제시 사용하는 도메인 순수 값 객체
 * MessageDTO의 도메인 전용 대체 (Command용)
 * </p>
 *
 * @param id 메시지 ID (수정/삭제시 사용)
 * @param userId 사용자 ID
 * @param decoType 데코레이션 타입
 * @param anonymity 익명 이름 (최대 8자)
 * @param content 메시지 내용 (최대 255자)
 * @param width 메시지 너비
 * @param height 메시지 높이
 * @author Jaeik
 * @since 2.0.0
 */
public record MessageCommand(
        Long id,
        Long userId,
        DecoType decoType,
        String anonymity,
        String content,
        int width,
        int height
) {

    @Builder
    public MessageCommand {
        // 도메인 비즈니스 규칙 검증
        if (anonymity != null && anonymity.length() > 8) {
            throw new IllegalArgumentException("익명 이름은 최대 8글자까지 입력 가능합니다.");
        }
        if (content != null && content.length() > 255) {
            throw new IllegalArgumentException("내용은 최대 255자까지 입력 가능합니다.");
        }
    }

    /**
     * <h3>메시지 작성용 명령 생성</h3>
     * <p>새 메시지 작성시 사용하는 명령을 생성합니다.</p>
     *
     * @param userId 사용자 ID
     * @param decoType 데코레이션 타입
     * @param anonymity 익명 이름
     * @param content 메시지 내용
     * @param width 메시지 너비
     * @param height 메시지 높이
     * @return MessageCommand 값 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public static MessageCommand ofCreate(Long userId, DecoType decoType, String anonymity, 
                                         String content, int width, int height) {
        return MessageCommand.builder()
                .userId(userId)
                .decoType(decoType)
                .anonymity(anonymity)
                .content(content)
                .width(width)
                .height(height)
                .build();
    }

    /**
     * <h3>메시지 삭제용 명령 생성</h3>
     * <p>메시지 삭제시 사용하는 명령을 생성합니다.</p>
     *
     * @param id 메시지 ID
     * @param userId 사용자 ID
     * @return MessageCommand 값 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public static MessageCommand ofDelete(Long id, Long userId) {
        return MessageCommand.builder()
                .id(id)
                .userId(userId)
                .build();
    }
}