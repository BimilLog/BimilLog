package jaeik.bimillog.infrastructure.adapter.in.post.web;

import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.infrastructure.adapter.in.post.dto.FullPostDTO;
import jaeik.bimillog.infrastructure.adapter.in.post.dto.SimplePostDTO;
import org.springframework.stereotype.Component;

/**
 * <h2>PostResponseMapper</h2>
 * <p>
 * Post 도메인 객체를 웹 응답 DTO로 변환하는 매퍼 컴포넌트입니다.
 * </p>
 * <p>
 * 헥사고날 아키텍처에서 도메인과 웹 레이어 간의 데이터 변환 책임을 분리하고,
 * Controller의 변환 로직을 독립적인 컴포넌트로 추출하여 단일 책임 원칙을 준수합니다.
 * </p>
 * <p>
 * PostQueryController와 PostCacheController에서 도메인 객체를 클라이언트 응답 형태로 변환할 때 호출되어,
 * 내부 도메인 구조를 노출하지 않고 API 응답 스펙에 맞는 DTO를 생성합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class PostResponseMapper {

    /**
     * <h3>PostSearchResult를 SimplePostResDTO로 변환</h3>
     *
     * @param postSearchResult 변환할 도메인 객체
     * @return SimplePostDTO 응답 DTO
     * @author jaeik
     * @since 2.0.0
     */
    public SimplePostDTO convertToSimplePostResDTO(PostSearchResult postSearchResult) {
        return SimplePostDTO.builder()
                .id(postSearchResult.getId())
                .title(postSearchResult.getTitle())
                .content(postSearchResult.getContent())
                .viewCount(postSearchResult.getViewCount())
                .likeCount(postSearchResult.getLikeCount())
                .postCacheFlag(postSearchResult.getPostCacheFlag())
                .createdAt(postSearchResult.getCreatedAt())
                .userId(postSearchResult.getUserId())
                .userName(postSearchResult.getUserName())
                .commentCount(postSearchResult.getCommentCount())
                .isNotice(postSearchResult.isNotice())
                .build();
    }

    /**
     * <h3>PostDetail을 FullPostResDTO로 변환</h3>
     *
     * @param postDetail 변환할 도메인 객체
     * @return FullPostDTO 응답 DTO
     * @author jaeik
     * @since 2.0.0
     */
    public FullPostDTO convertToFullPostResDTO(PostDetail postDetail) {
        return FullPostDTO.builder()
                .id(postDetail.id())
                .title(postDetail.title())
                .content(postDetail.content())
                .viewCount(postDetail.viewCount())
                .likeCount(postDetail.likeCount())
                .postCacheFlag(postDetail.postCacheFlag())
                .createdAt(postDetail.createdAt())
                .userId(postDetail.userId())
                .userName(postDetail.userName())
                .commentCount(postDetail.commentCount())
                .isNotice(postDetail.isNotice())
                .isLiked(postDetail.isLiked())
                .build();
    }
}