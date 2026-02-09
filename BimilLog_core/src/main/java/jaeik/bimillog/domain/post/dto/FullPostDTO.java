package jaeik.bimillog.domain.post.dto;

import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import lombok.*;

import java.time.Instant;

/**
 * <h2>게시글 상세 조회 응답 DTO</h2>
 * <p>게시글 상세 조회 시 사용되는 완전한 정보를 담는 글 응답 DTO입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FullPostDTO {
    private Long id;
    private Long memberId;
    private String memberName;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Instant createdAt;
    private boolean liked;
    private PostCacheFlag featuredType;

    public static FullPostDTO convertToFullPostResDTO(PostDetail postDetail) {
        return FullPostDTO.builder()
                .id(postDetail.getId())
                .title(postDetail.getTitle())
                .content(postDetail.getContent())
                .viewCount(postDetail.getViewCount())
                .likeCount(postDetail.getLikeCount())
                .createdAt(postDetail.getCreatedAt())
                .memberId(postDetail.getMemberId())
                .memberName(postDetail.getMemberName())
                .commentCount(postDetail.getCommentCount())
                .liked(postDetail.isLiked())
                .featuredType(postDetail.getFeaturedType())
                .build();
    }
}
