package jaeik.bimillog.domain.post.entity;

import com.querydsl.core.annotations.QueryProjection;

import java.time.Instant;

/**
 * <h2>게시글 상세 정보 조회용 프로젝션 Record</h2>
 * <p>
 * JOIN 쿼리를 통해 게시글, 좋아요 수, 댓글 수, 사용자 좋아요 여부를 한번에 조회하기 위한 프로젝션 구현체
 * </p>
 * <p>
 * QueryDSL의 @QueryProjection을 사용하여 타입 안전하고 성능 최적화된 프로젝션을 제공합니다.
 * </p>
 * <p>
 * 기존의 4개 개별 쿼리를 1개의 최적화된 JOIN 쿼리로 대체하여 성능을 개선합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public record PostDetailProjectionRecord(
        Long id,
        String title,
        String content,
        Integer viewCount,
        Instant createdAt,
        Long userId,
        String userName,
        Boolean isNotice,
        PostCacheFlag postCacheFlag,
        Long likeCount,
        Integer commentCount,
        Boolean isLiked
) {

    /**
     * <h3>QueryDSL 프로젝션 생성자</h3>
     * <p>@QueryProjection을 통해 QueryDSL이 타입 안전한 프로젝션을 생성할 수 있도록 합니다.</p>
     *
     * @param id 게시글 ID
     * @param title 게시글 제목
     * @param content 게시글 내용
     * @param viewCount 조회수
     * @param createdAt 생성일시
     * @param userId 작성자 ID
     * @param userName 작성자 이름
     * @param isNotice 공지사항 여부
     * @param postCacheFlag 캐시 플래그
     * @param likeCount 좋아요 개수
     * @param commentCount 댓글 개수
     * @param isLiked 사용자 좋아요 여부
     */
    @QueryProjection
    public PostDetailProjectionRecord {
    }

    /**
     * <h3>PostDetail 엔티티로 변환</h3>
     * <p>프로젝션 데이터를 PostDetail 도메인 객체로 변환합니다.</p>
     *
     * @return PostDetail 도메인 객체
     * @since 2.0.0
     */
    public PostDetail toPostDetail() {
        return PostDetail.builder()
                .id(id())
                .title(title())
                .content(content())
                .viewCount(viewCount())
                .createdAt(createdAt())
                .userId(userId())
                .userName(userName())
                .isNotice(isNotice() != null ? isNotice() : false)
                .postCacheFlag(postCacheFlag())
                .likeCount(likeCount() != null ? likeCount().intValue() : 0)
                .commentCount(commentCount() != null ? commentCount() : 0)
                .isLiked(isLiked() != null ? isLiked() : false)
                .build();
    }
}