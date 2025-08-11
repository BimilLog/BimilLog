package jaeik.growfarm.dto.post;

import jaeik.growfarm.domain.post.domain.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
public class FullPostResDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String title;
    private String content;
    private int views;
    private int likes;
    private boolean isNotice;
    private Instant createdAt;
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
