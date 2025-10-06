package jaeik.bimillog.domain.post.entity;

import com.querydsl.core.annotations.QueryProjection;
import jaeik.bimillog.domain.post.application.service.PostQueryService;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * <h2>게시글 상세 정보 값 객체</h2>
 * <p>게시글 상세 조회 결과를 담는 immutable 도메인 값 객체입니다.</p>
 * <p>QueryDSL Projection과 레디스 캐시를 지원합니다.</p>
 * <p>FullPostResDTO의 도메인 전용 대체 객체로 사용됩니다.</p>
 *
 * @param id 게시글 ID
 * @param title 게시글 제목
 * @param content 게시글 내용
 * @param viewCount 조회수
 * @param likeCount 추천수
 * @param createdAt 작성일시
 * @param memberId 작성자 ID
 * @param memberName 작성자 이름
 * @param commentCount 댓글 수
 * @param isLiked 사용자 추천 여부 (로그인 사용자만)
 * @author Jaeik
 * @version 2.0.0
 */
public record PostDetail(
        Long id,
        String title,
        String content,
        Integer viewCount,
        Integer likeCount,
        Instant createdAt,
        Long memberId,
        String memberName,
        Integer commentCount,
        boolean isLiked
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Builder
    @QueryProjection
    public PostDetail {
    }

    /**
     * <h3>게시글 상세 정보 생성</h3>
     * <p>게시글 엔티티와 메타 정보로부터 상세 정보를 생성합니다.</p>
     * <p>로그인 사용자를 위한 추천 상태 포함 버전입니다.</p>
     * <p>{@link PostQueryService}에서 게시글 상세 조회 시 호출됩니다.</p>
     *
     * @param post 게시글 엔티티
     * @param likeCount 추천수
     * @param commentCount 댓글수
     * @param isLiked 사용자 추천 여부
     * @return PostDetail 값 객체
     * @since 2.0.0
     * @author Jaeik
     */
    public static PostDetail of(Post post, Integer likeCount, Integer commentCount, boolean isLiked) {
        return PostDetail.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViews())
                .likeCount(likeCount)
                .createdAt(post.getCreatedAt())
                .memberId(post.getMember().getId())
                .memberName(post.getMember().getMemberName())
                .commentCount(commentCount)
                .isLiked(isLiked)
                .build();
    }

    /**
     * <h3>추천 여부 없는 상세 정보 생성</h3>
     * <p>비로그인 사용자를 위한 상세 정보를 생성합니다.</p>
     * <p>isLiked 값이 false로 고정됩니다.</p>
     * <p>{@link PostQueryService}에서 비로그인 사용자의 게시글 상세 조회 시 호출됩니다.</p>
     *
     * @param post 게시글 엔티티
     * @param likeCount 추천수
     * @param commentCount 댓글수
     * @return PostDetail 값 객체 (isLiked = false)
     * @since 2.0.0
     * @author Jaeik
     */
    public static PostDetail of(Post post, Integer likeCount, Integer commentCount) {
        return of(post, likeCount, commentCount, false);
    }

    /**
     * <h3>기본 댓글 수로 상세 정보 생성</h3>
     * <p>댓글 수 0으로 기본 상세 정보를 생성합니다.</p>
     * <p>주로 캐시된 게시글 데이터에서 사용됩니다.</p>
     *
     * @param post 게시글 엔티티
     * @param likeCount 추천수
     * @return PostDetail 값 객체 (commentCount = 0, isLiked = false)
     * @since 2.0.0
     * @author Jaeik
     */
    public static PostDetail of(Post post, Integer likeCount) {
        return of(post, likeCount, 0, false);
    }

    /**
     * <h3>추천 여부를 변경한 새로운 PostDetail 생성</h3>
     * <p>캐시된 PostDetail의 isLiked 필드만 변경하여 새로운 immutable 객체를 생성합니다.</p>
     * <p>필요한 필드만 변경하여 캐시 효율성을 높입니다.</p>
     * <p>{@link PostQueryService}에서 로그인 사용자의 추천 상태 맞춤형 조회 시 호출됩니다.</p>
     *
     * @param isLiked 사용자 추천 여부
     * @return PostDetail 새로운 PostDetail 객체
     * @since 2.0.0
     * @author Jaeik
     */
    public PostDetail withIsLiked(boolean isLiked) {
        return PostDetail.builder()
                .id(this.id)
                .title(this.title)
                .content(this.content)
                .viewCount(this.viewCount)
                .likeCount(this.likeCount)
                .createdAt(this.createdAt)
                .memberId(this.memberId)
                .memberName(this.memberName)
                .commentCount(this.commentCount)
                .isLiked(isLiked)
                .build();
    }

    /**
     * <h3>목록용 검색 결과로 변환</h3>
     * <p>PostDetail에서 PostSearchResult로 변환합니다.</p>
     * <p>isLiked 정보는 목록 화면에서 필요하지 않으므로 제외됩니다.</p>
     * <p>{@link PostQueryService}에서 게시글 목록 조회 시 호출됩니다.</p>
     *
     * @return PostSearchResult 목록용 검색 결과
     * @since 2.0.0
     * @author Jaeik
     */
    public PostSearchResult toSearchResult() {
        return PostSearchResult.ofPostDetail(this);
    }

}