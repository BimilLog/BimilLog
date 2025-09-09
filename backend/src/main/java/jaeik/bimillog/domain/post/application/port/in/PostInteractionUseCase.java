package jaeik.bimillog.domain.post.application.port.in;

/**
 * <h2>PostInteractionUseCase</h2>
 * <p>
 * Post 도메인의 사용자 상호작용 비즈니스 로직을 처리하는 인바운드 포트입니다.
 * 헥사고날 아키텍처에서 게시글에 대한 사용자의 반응과 참여 활동을 관리하는 역할을 합니다.
 * </p>
 * <p>
 * 이 유스케이스는 게시글과 사용자 간의 상호작용 기능을 제공합니다:
 * - 게시글 추천: 사용자의 게시글 추천/추천취소 처리
 * - 조회수 관리: 게시글 열람 시 조회수 증가 처리
 * - 사용자 참여 추적: 인기글 선정과 사용자 반응 데이터 축적
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 포트가 필요한 이유:
 * 1. 사용자 참여 유도 - 추천 기능으로 커뮤니티 활성화 도모
 * 2. 인기 콘텐츠 식별 - 조회수와 추천수를 통한 인기글 발굴
 * 3. 데이터 정확성 - 중복 추천 방지와 정확한 조회수 카운트
 * 4. 성능 최적화 - 비동기 조회수 증가로 사용자 경험 향상
 * </p>
 * <p>
 * PostCommandController에서 게시글 추천 API 제공 시 호출됩니다.
 * PostViewEventHandler에서 게시글 조회 이벤트 처리 시 비동기로 호출됩니다.
 * </p>
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