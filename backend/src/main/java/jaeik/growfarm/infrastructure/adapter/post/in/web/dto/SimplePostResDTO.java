package jaeik.growfarm.infrastructure.adapter.post.in.web.dto;

import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SimplePostResDTO {
    private Long id;
    private String title;
    private String content;
    private int views;
    private int likes;
    private boolean isNotice;
    private PostCacheFlag postCacheFlag;
    private Instant createdAt;
    private Long userId;
    private String userName;
    // ✅ COMPLETED: CommentReadRepository.findCommentCountsByPostIds() 배치 조회로 연결됨
    // N+1 문제 해결을 위해 PostQueryAdapter에서 배치 조회 후 설정됨
    private int commentCount;
    private int likeCount;
}
