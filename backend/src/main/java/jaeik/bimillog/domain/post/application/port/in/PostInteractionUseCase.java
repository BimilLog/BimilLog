package jaeik.bimillog.domain.post.application.port.in;

/**
 * <h2>게시글 상호작용 유스케이스</h2>
 * <p>게시글의 추천 및 조회수와 관련된 비즈니스 로직을 처리하는 유스케이스 인터페이스</p>
 * <p>헥사고널 아키텍처 원칙을 준수하여 HTTP 의존성을 제거했습니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostInteractionUseCase {

    /**
     * <h3>게시글 추천</h3>
     * <p>게시글을 추천하거나 추천 취소합니다.</p>
     * <p>이미 추천한 게시글인 경우 추천을 취소하고, 추천하지 않은 게시글인 경우 추천을 추가합니다.</p>
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param postId 추천할 게시글 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void likePost(Long userId, Long postId);

    /**
     * <h3>조회수 증가</h3>
     * <p>게시글의 조회수를 1 증가시킵니다.</p>
     * <p>게시글 조회 이벤트 처리 시 호출되는 메서드입니다.</p>
     *
     * @param postId 조회수를 증가시킬 게시글 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void incrementViewCount(Long postId);

}