package jaeik.bimillog.domain.post.entity;

/**
 * <h2>인기 게시글 정보</h2>
 * <p>주간 인기글 및 전설의 게시글 조회 시 사용되는 최소 정보 DTO입니다.</p>
 * <p>Redis 캐싱 및 알림 전송에 필요한 핵심 데이터만 포함합니다.</p>
 *
 * @param postId   게시글 ID
 * @param memberId 작성자 ID (알림 수신자)
 * @param title    게시글 제목 (알림 메시지용)
 * @author Jaeik
 * @since 2.0.0
 */
public record PopularPostInfo(
        Long postId,
        Long memberId,
        String title
) {
}
