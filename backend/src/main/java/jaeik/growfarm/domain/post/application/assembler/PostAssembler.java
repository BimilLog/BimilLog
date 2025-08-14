
package jaeik.growfarm.domain.post.application.assembler;

import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.dto.post.FullPostResDTO;
import org.springframework.stereotype.Component;

/**
 * <h2>게시글 어셈블러</h2>
 * <p>게시글 엔티티를 다양한 DTO로 변환하는 역할을 담당합니다.</p>
 * <p>주로 FullPostResDTO 변환 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class PostAssembler {

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
    public FullPostResDTO toFullPostResDTO(Post post, long likeCount, boolean isLiked) {
        return FullPostResDTO.builder()
                .id(post.getId())
                .userId(post.getUser() != null ? post.getUser().getId() : null)
                .userName(post.getUser() != null ? post.getUser().getUserName() : "익명")
                .title(post.getTitle())
                .content(post.getContent())
                .views(post.getViews())
                .likes((int) likeCount)
                .isNotice(post.isNotice())
                .createdAt(post.getCreatedAt())
                .isLiked(isLiked)
                .build();
    }
}
