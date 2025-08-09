package jaeik.growfarm.dto.post;

import jaeik.growfarm.entity.post.PostCacheFlag;
import jaeik.growfarm.entity.post.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * <h2>게시글 상세 정보 DTO</h2>
 * <p>
 * 게시글 상세 보기용 데이터 전송 객체
 * </p>
 * 
 * @version  2.0.0
 * @author Jaeik
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FullPostResDTO extends BasePostDisplayDTO{

    protected String content;

    protected boolean userLike;

    /**
     * <h3>새 게시글을 PostDTO로 변환</h3>
     * <p>
     * 게시글 상세 보기 시 사용하는 변환 로직이다.
     * </p>
     *
     * @param post 변환할 Post 엔티티
     * @return 변환된 PostDTO 객체
     * @since 2.0.0
     * @author Jaeik
     */
    public static FullPostResDTO newPost(Post post) {
        return FullPostResDTO.builder()
                .postId(post.getId())
                .userId((post.getUser() != null) ? post.getUser().getId() : null)
                .userName((post.getUser() != null) ? post.getUser().getUserName() : "익명")
                .title(post.getTitle())
                .content(post.getContent())
                .views(0)
                .likes(0)
                .isNotice(false)
                .postCacheFlag(null)
                .createdAt(post.getCreatedAt())
                .userLike(false)
                .build();
    }

    /**
     * <h3>기존 게시글을 PostDTO로 변환</h3>
     * <p>
     * Post 엔티티를 기반으로 PostDTO를 생성한다.
     * </p>
     *
     * @since 2.0.0
     * @author Jaeik
     */
    public static FullPostResDTO existedPost(Long postId, Long userId, String userName, String title, String content,
                                             int views, int likes, boolean isNotice, PostCacheFlag postCacheFlag, Instant createdAt, boolean userLike) {

        String nickName = userName != null ? userName : "익명";

        return FullPostResDTO.builder()
                .postId(postId)
                .userId(userId)
                .userName(nickName)
                .title(title)
                .content(content)
                .views(views)
                .likes(likes)
                .isNotice(isNotice)
                .postCacheFlag(postCacheFlag)
                .createdAt(createdAt)
                .userLike(userLike)
                .build();
    }
}
