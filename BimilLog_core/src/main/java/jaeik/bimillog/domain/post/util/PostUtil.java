package jaeik.bimillog.domain.post.util;

import jaeik.bimillog.domain.post.adapter.PostToCommentAdapter;
import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostUtil {

    private final PostToCommentAdapter postToCommentAdapter;
    private final PostToMemberAdapter postToMemberAdapter;

    /**
     * <h3>게시글 목록에 댓글 수 주입</h3>
     * <p>게시글 목록의 댓글 수를 배치로 조회하여 주입합니다.</p>
     * <p>좋아요 수는 PostQueryHelper에서 이미 처리되므로, 여기서는 댓글 수만 처리합니다.</p>
     *
     * @param posts 댓글 수를 채울 게시글 목록
     */
    public void enrichPostsCommentCount(List<PostSimpleDetail> posts) {
        List<Long> postIds = posts.stream().map(PostSimpleDetail::getId).toList();
        Map<Long, Integer> commentCounts = postToCommentAdapter.findCommentCountsByPostIds(postIds);

        posts.forEach(post -> {
            post.setCommentCount(commentCounts.getOrDefault(post.getId(), 0));
        });
    }

    /**
     * <h3>게시글에서 블랙리스트 제거</h3>
     */
    public List<PostSimpleDetail> removePostsWithBlacklist(Long memberId, List<PostSimpleDetail> posts) {
        if (memberId == null || posts.isEmpty()) {
            return posts;
        }

        List<Long> blacklistIds = postToMemberAdapter.getInterActionBlacklist(memberId);
        Set<Long> blacklistSet = new HashSet<>(blacklistIds);
        return posts.stream().filter(post -> !blacklistSet.contains(post.getMemberId())).collect(Collectors.toList());
    }


}
