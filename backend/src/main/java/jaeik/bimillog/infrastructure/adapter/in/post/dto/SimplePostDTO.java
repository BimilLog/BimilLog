package jaeik.bimillog.infrastructure.adapter.in.post.dto;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * <h2>간편 게시글 조회용 응답 DTO</h2>
 * <p>
 * 게시글 목록 조회 시 사용되는 간단한 정보를 담는 응답 DTO입니다.
 * </p>
 * <p>
 * PostQueryController의 게시글 목록 조회 API에서 응답 바디로 사용되며,
 * 게시글 ID, 제목, 내용, 조회수, 추천수, 댓글수, 공지 여부, 작성 시간, 작성자 정보 등을 포함합니다.
 * </p>
 * <p>
 * 클라이언트의 게시판 메인 페이지에서 GET /api/post/query 엔드포인트 호출 시 페이지 단위로 반환되며,
 * 게시글 상세 조회가 아닌 목록 표시에 필요한 핵심 정보만을 제공합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SimplePostDTO {
    private Long id;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private PostCacheFlag postCacheFlag;
    private Instant createdAt;
    private Long userId;
    private String userName;
    private Integer commentCount;
    private boolean isNotice;

}
