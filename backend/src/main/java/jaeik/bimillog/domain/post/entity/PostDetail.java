package jaeik.bimillog.domain.post.entity;

import com.querydsl.core.annotations.QueryProjection;
import jaeik.bimillog.domain.post.application.service.PostQueryService;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * <h2>게시글 상세 정보 값 객체</h2>
 * <p>게시글 상세 조회 결과를 담는 도메인 값 객체입니다.</p>
 * <p>QueryDSL Projection과 레디스 캐시를 지원합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@NoArgsConstructor
public class PostDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private Instant createdAt;
    private Long memberId;
    private String memberName;
    private Integer commentCount;
    private boolean isLiked;

    /**
     * <h3>생성자 - QueryDSL Projection용</h3>
     * <p>QueryDSL @QueryProjection을 위한 전용 생성자입니다.</p>
     * <p>PostQueryAdapter에서 게시글 상세 조회 시 QueryDSL을 통해 호출됩니다.</p>
     *
     * @since 2.0.0
     * @author Jaeik
     */
    @Builder
    @QueryProjection
    public PostDetail(Long id, String title, String content, Integer viewCount,
                     Integer likeCount, Instant createdAt, Long memberId,
                     String memberName, Integer commentCount, boolean isLiked) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.memberId = memberId;
        this.memberName = memberName;
        this.commentCount = commentCount;
        this.isLiked = isLiked;
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
     * <p>PostDetail에서 PostSimpleDetail로 변환합니다.</p>
     * <p>isLiked 정보는 목록 화면에서 필요하지 않으므로 제외됩니다.</p>
     * <p>{@link PostQueryService}에서 게시글 목록 조회 시 호출됩니다.</p>
     *
     * @return PostSimpleDetail 목록용 검색 결과
     * @since 2.0.0
     * @author Jaeik
     */
    public PostSimpleDetail toSimpleDetail() {
        return PostSimpleDetail.builder()
                .id(this.id)
                .title(this.title)
                .viewCount(this.viewCount)
                .likeCount(this.likeCount)
                .createdAt(this.createdAt)
                .memberId(this.memberId)
                .memberName(this.memberName)
                .commentCount(this.commentCount)
                .build();
    }

}