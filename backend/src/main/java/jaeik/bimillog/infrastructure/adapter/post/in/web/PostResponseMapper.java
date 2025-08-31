package jaeik.bimillog.infrastructure.adapter.post.in.web;

import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.infrastructure.adapter.post.in.web.dto.FullPostResDTO;
import jaeik.bimillog.infrastructure.adapter.post.in.web.dto.SimplePostResDTO;
import org.springframework.stereotype.Component;

/**
 * <h3>Post 응답 매퍼</h3>
 * <p>도메인 객체를 응답 DTO로 변환하는 매퍼 클래스</p>
 * <p>Controller의 변환 로직을 분리하여 단일 책임 원칙을 준수</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Component
public class PostResponseMapper {

    /**
     * <h3>PostSearchResult를 SimplePostResDTO로 변환</h3>
     *
     * @param postSearchResult 변환할 도메인 객체
     * @return SimplePostResDTO 응답 DTO
     * @author jaeik
     * @since 2.0.0
     */
    public SimplePostResDTO convertToSimplePostResDTO(PostSearchResult postSearchResult) {
        return SimplePostResDTO.builder()
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
     * @return FullPostResDTO 응답 DTO
     * @author jaeik
     * @since 2.0.0
     */
    public FullPostResDTO convertToFullPostResDTO(PostDetail postDetail) {
        return FullPostResDTO.builder()
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