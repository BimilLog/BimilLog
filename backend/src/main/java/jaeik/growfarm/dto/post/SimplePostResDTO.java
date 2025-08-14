package jaeik.growfarm.dto.post;

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
    // TODO: 현재 항상 0으로 설정됨 - CommentQueryPort.findCommentCountsByPostIds() 연결 필요
    // PostQueryService에서 게시글 목록 조회 시 댓글 수를 설정하는 로직 추가 필요
    private int commentCount;
    private int likeCount;
}
