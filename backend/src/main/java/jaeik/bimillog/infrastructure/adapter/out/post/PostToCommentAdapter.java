package jaeik.bimillog.infrastructure.adapter.out.post;

import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostToCommentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h2>PostToCommentAdapter</h2>
 * <p>
 * Post 도메인에서 Comment 도메인으로의 크로스 도메인 어댑터입니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class PostToCommentAdapter implements PostToCommentPort {

    private final CommentQueryUseCase commentQueryUseCase;


    /**
     * <h3>여러 게시글의 댓글 수 배치 조회</h3>
     * <p>사용자가 게시판 목록 페이지를 로드할 때 각 게시글 옆에 댓글 수를 표시하기 위해 호출됩니다.</p>
     * <p>예를 들어 "게시글 제목 [5]" 형태로 표시되어 사용자가 토론이 활발한 게시글을 빠르게 식별할 수 있도록 합니다.</p>
     * <p>단일 쿼리로 모든 게시글의 댓글 수를 조회하여 N+1 문제를 방지하고 게시판 로딩 성능을 보장합니다.</p>
     * <p>계층형 댓글 구조에서 삭제되지 않은 댓글만 카운팅하여 정확한 통계를 제공합니다.</p>
     * 
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
        return commentQueryUseCase.findCommentCountsByPostIds(postIds);
    }
}