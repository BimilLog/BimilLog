package jaeik.growfarm.infrastructure.adapter.post.in.web.dto;

import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * <h2>게시글 상세 조회 응답 DTO</h2>
 * <p>
 * 게시글의 상세 정보를 담는 DTO로, 게시글 ID, 작성자 ID, 작성자 이름, 제목, 내용, 조회수, 추천 수,
 * 공지 여부, 작성 시간 등을 포함합니다.
 * </p>
 * <p>
 * 게시글의 추천 여부를 포함하여 사용자가 추천을 눌렀는지 여부를 나타냅니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
public class FullPostResDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Instant createdAt;
    private PostCacheFlag postCacheFlag;
    private boolean isNotice;
    private boolean isLiked;

    @Builder
    public FullPostResDTO(Long id, Long userId, String userName, String title, String content,
                          Integer viewCount, Integer likeCount, boolean isNotice, Instant createdAt, boolean isLiked, Integer commentCount, PostCacheFlag postCacheFlag) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.title = title;
        this.content = content;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.isNotice = isNotice;
        this.createdAt = createdAt;
        this.isLiked = isLiked;
        this.commentCount = commentCount;
        this.postCacheFlag = postCacheFlag;
    }

}
