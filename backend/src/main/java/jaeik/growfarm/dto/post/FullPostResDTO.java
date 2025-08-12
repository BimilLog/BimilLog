package jaeik.growfarm.dto.post;

import jaeik.growfarm.domain.post.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
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
    private final boolean isLiked;

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

    public static FullPostResDTO from(Post post, boolean isLiked, int likeCount) {
        return FullPostResDTO.builder()
                .id(post.getId())
                .userId(post.getUser() != null ? post.getUser().getId() : null)
                .userName(post.getUser() != null ? post.getUser().getUserName() : "익명")
                .title(post.getTitle())
                .content(post.getContent())
                .views(post.getViews())
                .likes(likeCount)
                .isNotice(post.isNotice())
                .createdAt(post.getCreatedAt())
                .isLiked(isLiked)
                .build();
    }
}
