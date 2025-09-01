package jaeik.bimillog.domain.post.entity;

import java.time.Instant;

/**
 * <h2>게시글 상세 정보 조회용 프로젝션</h2>
 * <p>
 * JOIN 쿼리를 통해 게시글, 좋아요 수, 댓글 수, 사용자 좋아요 여부를 한번에 조회하기 위한 프로젝션 인터페이스
 * </p>
 * <p>
 * 기존의 4개 개별 쿼리를 1개의 최적화된 JOIN 쿼리로 대체하여 성능을 개선합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostDetailProjection {

    /**
     * <h3>게시글 ID</h3>
     * @return 게시글 ID
     * @since 2.0.0
     */
    Long getId();

    /**
     * <h3>게시글 제목</h3>
     * @return 게시글 제목
     * @since 2.0.0
     */
    String getTitle();

    /**
     * <h3>게시글 내용</h3>
     * @return 게시글 내용
     * @since 2.0.0
     */
    String getContent();

    /**
     * <h3>조회수</h3>
     * @return 조회수
     * @since 2.0.0
     */
    Integer getViewCount();

    /**
     * <h3>생성일시</h3>
     * @return 게시글 생성일시
     * @since 2.0.0
     */
    Instant getCreatedAt();

    /**
     * <h3>작성자 ID</h3>
     * @return 작성자 사용자 ID
     * @since 2.0.0
     */
    Long getUserId();

    /**
     * <h3>작성자 이름</h3>
     * @return 작성자 사용자명
     * @since 2.0.0
     */
    String getUserName();

    /**
     * <h3>공지사항 여부</h3>
     * @return 공지사항이면 true, 일반 게시글이면 false
     * @since 2.0.0
     */
    Boolean getIsNotice();

    /**
     * <h3>캐시 플래그</h3>
     * @return 게시글의 캐시 플래그 (인기글 타입)
     * @since 2.0.0
     */
    PostCacheFlag getPostCacheFlag();

    /**
     * <h3>좋아요 개수</h3>
     * @return 게시글의 총 좋아요 개수
     * @since 2.0.0
     */
    Long getLikeCount();

    /**
     * <h3>댓글 개수</h3>
     * @return 게시글의 총 댓글 개수
     * @since 2.0.0
     */
    Integer getCommentCount();

    /**
     * <h3>사용자 좋아요 여부</h3>
     * <p>현재 사용자가 이 게시글에 좋아요를 눌렀는지 여부</p>
     * <p>비로그인 사용자의 경우 항상 false</p>
     * @return 사용자가 좋아요를 눌렀으면 true, 아니면 false
     * @since 2.0.0
     */
    Boolean getIsLiked();

    /**
     * <h3>PostDetail 엔티티로 변환</h3>
     * <p>프로젝션 데이터를 PostDetail 도메인 객체로 변환합니다.</p>
     *
     * @return PostDetail 도메인 객체
     * @since 2.0.0
     */
    default PostDetail toPostDetail() {
        return PostDetail.builder()
                .id(getId())
                .title(getTitle())
                .content(getContent())
                .viewCount(getViewCount())
                .createdAt(getCreatedAt())
                .userId(getUserId())
                .userName(getUserName())
                .isNotice(getIsNotice() != null ? getIsNotice() : false)
                .postCacheFlag(getPostCacheFlag())
                .likeCount(getLikeCount() != null ? getLikeCount().intValue() : 0)
                .commentCount(getCommentCount() != null ? getCommentCount() : 0)
                .isLiked(getIsLiked() != null ? getIsLiked() : false)
                .build();
    }
}