package jaeik.bimillog.domain.admin.out;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.service.PostQueryService;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>관리자 글 조회 어댑터</h2>
 *
 * @author Jaeik
 * @version 2.4.0
 */
@Repository
@RequiredArgsConstructor
public class AdminToPostAdapter {
    private final PostQueryService postQueryService;

    /**
     * <h3>ID로 게시글 조회</h3>
     * <p>주어진 ID를 사용하여 게시글을 조회합니다.</p>
     *
     * @param id 조회할 게시글 ID
     * @return 조회된 게시글 엔티티
     */
    public Post findById(Long id) {
        return postQueryService.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }


    /**
     * <h3>PostId 목록으로 Post 리스트 반환</h3>
     */
    public List<Post> findAllByIds(List<Long> postIds) {
        return postQueryService.findAllByIds(postIds);
    }
}
