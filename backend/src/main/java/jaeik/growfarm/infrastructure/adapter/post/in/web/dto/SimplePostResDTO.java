package jaeik.growfarm.infrastructure.adapter.post.in.web.dto;

import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.domain.post.entity.PostSearchResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * <h2>간편 게시글 조회용 DTO</h2>
 * <p>게시글의 간단한 정보를 담는 DTO로, 게시글 ID, 제목, 내용, 조회수, 추천 수, 공지 여부,
 * 작성 시간, 작성자 ID, 작성자 이름 등을 포함합니다.</p>
 * <p>게시판에서 목록을 이루는 용도</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SimplePostResDTO {
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

    /**
     * <h3>도메인 PostSearchResult로부터 DTO 생성</h3>
     * <p>헥사고날 아키텍처 패턴: 도메인 순수 객체를 인프라 DTO로 변환</p>
     *
     * @param searchResult 도메인 검색 결과 객체
     * @return SimplePostResDTO 인프라 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    public static SimplePostResDTO from(PostSearchResult searchResult) {
        return SimplePostResDTO.builder()
                .id(searchResult.getId())
                .title(searchResult.getTitle())
                .content(searchResult.getContent())
                .viewCount(searchResult.getViewCount())
                .likeCount(searchResult.getLikeCount())
                .postCacheFlag(searchResult.getPostCacheFlag())
                .createdAt(searchResult.getCreatedAt())
                .userId(searchResult.getUserId())
                .userName(searchResult.getUserName())
                .commentCount(searchResult.getCommentCount())
                .isNotice(searchResult.isNotice())
                .build();
    }
}
