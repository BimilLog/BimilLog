package jaeik.bimillog.domain.post.application.port.in;

/**
 * <h2>게시글 상호작용 유스케이스</h2>
 * <p>Post 도메인의 사용자 상호작용 작업을 담당하는 유스케이스입니다.</p>
 * <p>게시글 추천/추천취소 처리</p>
 * <p>조회수 증가 처리</p>
 * <p>중복 추천 방지 및 사용자 참여 추적</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostInteractionUseCase {

    /**
     * <h3>게시글 추천 상태 토글</h3>
     * <p>사용자의 게시글 추천 상태를 확인하여 추천/추천취소를 처리합니다.</p>
     * <p>기존에 추천한 게시글이면 추천을 취소하고, 추천하지 않은 게시글이면 새로 추천을 추가합니다.</p>
     * <p>PostCommandController에서 게시글 추천 토글 API 요청을 처리할 때 호출됩니다.</p>
     * <p>중복 추천 방지를 위해 기존 추천 여부를 체크하고 상태를 변경합니다.</p>
     *
     * @param userId 추천을 요청한 사용자의 ID
     * @param postId 추천 대상 게시글의 식별자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void likePost(Long userId, Long postId);

    /**
     * <h3>게시글 조회수 1 증가</h3>
     * <p>특정 게시글의 조회수를 1만큼 증가시켜 사용자 관심도를 반영합니다.</p>
     * <p>게시글 엔티티의 viewCount 필드를 원자적으로 업데이트하여 동시 접근에 대비합니다.</p>
     * <p>PostViewEventHandler에서 게시글 상세 조회 이벤트 수신 시 비동기로 호출됩니다.</p>
     * <p>조회수 증가는 사용자 경험에 영향을 주지 않도록 이벤트 방식으로 처리됩니다.</p>
     *
     * @param postId 조회수를 증가시킬 게시글의 식별자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void incrementViewCount(Long postId);

}