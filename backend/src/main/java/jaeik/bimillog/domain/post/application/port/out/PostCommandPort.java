package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.Post;

/**
 * <h2>PostCommandPort</h2>
 * <p>
 * Post 도메인에서 게시글 데이터의 생성, 수정, 삭제 작업을 처리하는 아웃바운드 포트입니다.
 * 헥사고날 아키텍처에서 Post 도메인과 영속성 계층을 분리하는 추상화된 인터페이스 역할을 합니다.
 * </p>
 * <p>PostCommandService에서 CQRS Command 패턴에 따른 모든 쓰기 작업 시 호출됩니다.</p>
 * <p>게시글 생성, 수정, 삭제와 조회수 증가 등 게시글 상태 변경을 담당합니다.</p>
 * <p>트랜잭션 경계 내에서 데이터 일관성을 보장하며, 비즈니스 규칙에 따른 검증은 도메인 서비스에서 수행됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCommandPort {

    /**
     * <h3>게시글 저장 및 수정</h3>
     * <p>게시글 엔티티를 데이터베이스에 저장하거나 수정합니다.</p>
     * <p>PostCommandService의 createPost, updatePost 메서드에서 호출됩니다.</p>
     * <p>신규 게시글 생성 시 ID가 자동 생성되고, 기존 게시글 수정 시 변경된 필드만 업데이트됩니다.</p>
     * <p>JPA의 save 메서드를 통해 엔티티 상태에 따른 INSERT 또는 UPDATE 작업이 수행됩니다.</p>
     *
     * @param post 저장하거나 수정할 게시글 엔티티
     * @return 저장된 게시글 엔티티 (ID 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    Post save(Post post);

    /**
     * <h3>게시글 삭제</h3>
     * <p>게시글 엔티티를 데이터베이스에서 완전히 삭제합니다.</p>
     * <p>PostCommandService의 deletePost 메서드에서 권한 검증 후 호출됩니다.</p>
     * <p>연관된 댓글, 좋아요 데이터는 CASCADE 설정에 따라 자동으로 함께 삭제되며, 캐시 무효화는 별도 이벤트로 처리됩니다.</p>
     * <p>물리적 삭제를 수행하므로 복구가 불가능하며, 삭제 전 충분한 검증이 선행되어야 합니다.</p>
     *
     * @param post 삭제할 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void delete(Post post);

    /**
     * <h3>게시글 조회수 증가</h3>
     * <p>특정 게시글의 조회수를 1 증가시킵니다.</p>
     * <p>PostQueryService의 게시글 조회 메서드들에서 비동기적으로 호출됩니다.</p>
     * <p>조회수는 게시글 열람 시마다 자동으로 증가되며, 중복 조회에 대한 제한은 별도 로직으로 처리됩니다.</p>
     * <p>UPDATE 쿼리를 통해 해당 ID의 게시글 views 필드를 증가시키는 원자적 연산을 수행합니다.</p>
     *
     * @param postId 조회수를 증가시킬 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void incrementViewByPostId(Long postId);
}