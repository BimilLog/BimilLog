package jaeik.bimillog.domain.global.application.port.out;

import jaeik.bimillog.domain.post.application.service.PostQueryService;
import jaeik.bimillog.domain.post.entity.Post;

public interface GlobalPostQueryPort {

    /**
     * <h3>게시글 ID로 단일 게시글 조회</h3>
     * <p>특정 ID에 해당하는 게시글 엔티티를 조회합니다.</p>
     * <p>게시글 수정, 삭제, 상세 조회 등의 기본 CRUD 작업 시 사용</p>
     * <p>{@link PostQueryService}에서 게시글 존재성 검증 및 권한 확인 시 호출됩니다.</p>
     *
     * @param id 조회할 게시글 ID
     * @return Post 조회된 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    Post findById(Long id);
}
