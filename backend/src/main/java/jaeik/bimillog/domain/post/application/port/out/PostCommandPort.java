package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.application.service.PostCommandService;
import jaeik.bimillog.domain.post.entity.Post;

/**
 * <h2>게시글 명령 포트</h2>
 * <p>게시글 도메인의 명령 작업을 담당하는 포트입니다.</p>
 * <p>게시글 저장, 수정, 삭제</p>
 * <p>조회수 증가 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCommandPort {

    /**
     * <h3>게시글 생성</h3>
     * <p>새로운 게시글 엔티티를 데이터베이스에 생성합니다.</p>
     * <p>신규 생성 시에만 사용하며 ID가 자동 생성됩니다</p>
     * <p>{@link PostCommandService}에서 게시글 작성 시 호출됩니다.</p>
     *
     * @param post 생성할 게시글 엔티티
     * @return 생성된 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    Post create(Post post);

    /**
     * <h3>게시글 삭제</h3>
     * <p>게시글을 데이터베이스에서 완전히 삭제합니다.</p>
     * <p>연관된 댓글, 좋아요 데이터는 CASCADE로 함께 삭제</p>
     * <p>{@link PostCommandService}에서 권한 검증 후 호출됩니다.</p>
     *
     * @param post 삭제할 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void delete(Post post);

    /**
     * <h3>조회수 증가</h3>
     * <p>특정 게시글의 조회수를 1 증가시킵니다.</p>
     * <p>UPDATE 쿼리로 해당 게시글의 views 필드를 증가</p>
     * <p>{@link PostCommandService}에서 게시글 조회 시 호출됩니다.</p>
     *
     * @param postId 조회수 증가할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void incrementViewByPostId(Long postId);
}