
package jaeik.growfarm.domain.post.application.assembler;

import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.dto.post.FullPostResDTO;
import org.springframework.stereotype.Component;

@Component
public class PostAssembler {

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
