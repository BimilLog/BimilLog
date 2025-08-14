package jaeik.growfarm.dto.post;

import jaeik.growfarm.domain.post.entity.Post;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class FullPostResDTO {
    private final Long id;
    private final Long userId;
    private final String userName;
    private final String title;
    private final String content;
    private final int views;
    private final int likes;
    private final boolean isNotice;
    private final Instant createdAt;
    private boolean isLiked;

    @Builder
    public FullPostResDTO(Long id, Long userId, String userName, String title, String content,
                          int views, int likes, boolean isNotice, Instant createdAt, boolean isLiked) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.title = title;
        this.content = content;
        this.views = views;
        this.likes = likes;
        this.isNotice = isNotice;
        this.createdAt = createdAt;
        this.isLiked = isLiked;
    }

    /**
     * <h3>FullPostResDTO로 변환</h3>
     * <p>Post 엔티티, 추천 수, 추천 여부를 FullPostResDTO로 변환합니다.</p>
     *
     * @param post      게시글 엔티티
     * @param likeCount 추천 수
     * @param isLiked   사용자가 추천를 눌렀는지 여부
     * @return FullPostResDTO 변환된 게시글 응답 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    public static FullPostResDTO from(Post post, long likeCount, boolean isLiked) {
        return FullPostResDTO.builder()
                .id(post.getId())
                .userId(post.getUser() != null ? post.getUser().getId() : null)
                .userName(post.getUser() != null ? post.getUser().getUserName() : "익명")
                .title(post.getTitle())
                .content(post.getContent())
                .views(post.getViews())
                .likes((int) likeCount)
                .isNotice(post.isNotice())
                .createdAt(post.getCreatedAt())
                .isLiked(isLiked)
                .build();
    }
}
