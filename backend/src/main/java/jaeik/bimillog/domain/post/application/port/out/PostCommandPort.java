package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.Post;

/**
 * <h2>글 관리 포트</h2>
 * <p>
 *     게시글 데이터의 생성/수정/삭제를 담당하는 통합 Port 인터페이스입니다.
 * </p>
 *
 * @author jaeik
 * @version 2.0.0
 */
public interface PostCommandPort {

    /**
     * <h3>게시글 저장 / 수정</h3>
     * <p>
     *     게시글 엔티티를 데이터베이스에 저장합니다.
     *     신규 게시글 생성 및 기존 게시글 수정 모두 처리합니다.
     * </p>
     * @param post 저장할 게시글 엔티티
     * @return 저장된 게시글 엔티티
     */
    Post save(Post post);

    /**
     * <h3>게시글 삭제</h3>
     * <p>
     *     게시글 엔티티를 데이터베이스에서 삭제합니다.
     * </p>
     * @param post 삭제할 게시글 엔티티
     */
    void delete(Post post);

    /**
     * <h3>조회수 증가</h3>
     * <p>
     *     게시글의 조회수를 1 증가시키고 데이터베이스에 저장합니다.
     *     Command 작업이므로 PostCommandPort에 위치합니다.
     * </p>
     * @param post 조회수를 증가시킬 게시글 엔티티
     */
    void incrementView(Post post);
}