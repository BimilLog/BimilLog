package jaeik.bimillog.domain.post.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

/**
 * 사용자가 마이페이지에서 보는 글 객체
 * 댓글의 경우는 SimpleCommentInfo클래스가 마이페이지에만 쓰여서 내부 클래스로 두었지만
 * 글의 경우는 PostSimpleDetail이 마이페이지 말고도 다른 관심사에도 쓰여 응집도 강화를 하지않고 별도의 클래스로 두었음
 */
@Getter
@AllArgsConstructor
public class MemberActivityPost {
    private Page<PostSimpleDetail> writePosts;
    private Page<PostSimpleDetail> likedPosts;
}
