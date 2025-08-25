package jaeik.growfarm.infrastructure.adapter.post.in.web.dto;

import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.domain.post.entity.PostDetail;
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
    public static FullPostResDTO from(Post post, Integer likeCount, boolean isLiked) {
        return FullPostResDTO.builder()
                .id(post.getId())
                .userId(post.getUser() != null ? post.getUser().getId() : null)
                .userName(post.getUser() != null ? post.getUser().getUserName() : "익명")
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViews())
                .likeCount(likeCount)
                .isNotice(post.isNotice())
                .createdAt(post.getCreatedAt())
                .isLiked(isLiked)
                .commentCount(0) // 기본값으로 0 설정
                .build();
    }

    /**
     * <h3>도메인 PostDetail로부터 DTO 생성</h3>
     * <p>헥사고날 아키텍처 패턴: 도메인 순수 객체를 인프라 DTO로 변환</p>
     *
     * @param postDetail 도메인 게시글 상세 객체
     * @return FullPostResDTO 인프라 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    public static FullPostResDTO from(PostDetail postDetail) {
        return FullPostResDTO.builder()
                .id(postDetail.id())
                .userId(postDetail.userId())
                .userName(postDetail.userName())
                .title(postDetail.title())
                .content(postDetail.content())
                .viewCount(postDetail.viewCount())
                .likeCount(postDetail.likeCount())
                .isNotice(postDetail.isNotice())
                .createdAt(postDetail.createdAt())
                .isLiked(postDetail.isLiked())
                .commentCount(postDetail.commentCount())
                .build();
    }
}
