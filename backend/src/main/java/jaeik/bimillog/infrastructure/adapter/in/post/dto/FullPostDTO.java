package jaeik.bimillog.infrastructure.adapter.in.post.dto;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import lombok.*;

import java.time.Instant;

/**
 * <h2>게시글 상세 조회 응답 DTO</h2>
 * <p>
 * 게시글 상세 조회 시 사용되는 완전한 정보를 담는 응답 DTO입니다.
 * </p>
 * <p>
 * PostQueryController의 게시글 상세 조회 API에서 응답 바디로 사용되며,
 * 게시글의 모든 상세 정보와 함께 사용자별 상호작용 상태(좋아요 여부)를 포함합니다.
 * </p>
 * <p>
 * 클라이언트의 게시글 상세 페이지에서 GET /api/post/query/{id} 엔드포인트 호출 시 반환되며,
 * 게시글 내용 전체, 작성자 정보, 통계 데이터, 사용자 상호작용 상태를 제공합니다.
 * </p>
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
    private PostCacheFlag postCacheFlag;
    private boolean isNotice;
    private boolean isLiked;
}
