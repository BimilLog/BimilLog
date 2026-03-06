package jaeik.bimillog.domain.post.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

/**
 * <h2>사용자가 마이페이지에서 보는 글 객체</h2>
 */
@Getter
@AllArgsConstructor
public class MemberActivityPost {
    private Page<PostSimpleDetail> writePosts;
    private Page<PostSimpleDetail> likedPosts;
}
