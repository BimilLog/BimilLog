package jaeik.growfarm.domain.post.application.service;

import jaeik.growfarm.domain.post.application.port.in.PostQueryUseCase;
import jaeik.growfarm.domain.post.application.port.out.LoadPostPort;
import jaeik.growfarm.domain.post.domain.Post;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>PostQueryService</h2>
 * <p>
 *     PostQueryUseCase의 구현체입니다.
 *     게시글 조회 관련 비즈니스 로직을 처리합니다.
 * </p>
 *
 * @author jaeik
 * @version 1.0
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostQueryService implements PostQueryUseCase {

    private final LoadPostPort loadPostPort;
    // private final LoadPostLikePort loadPostLikePort; // 좋아요 수 조회를 위해 필요

    @Override
    public Page<SimplePostResDTO> getBoard(Pageable pageable) {
        return loadPostPort.findByPage(pageable);
    }

    @Override
    public FullPostResDTO getPost(Long postId, User user) {
        Post post = loadPostPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 조회수 증가 로직은 웹 계층에서 처리 후, 이 서비스를 호출하기 전에 Port를 통해 처리되어야 함.
        // 예: controller -> incrementViewCountPort.increment(postId) -> getPost(postId, user)
        loadPostPort.incrementView(post);

        // 좋아요 수, 현재 사용자 좋아요 여부 등 추가 정보 조합
        // long likeCount = loadPostLikePort.countByPost(post);
        // boolean isLiked = user != null && existPostLikePort.existsByUserAndPost(user, post);

        return FullPostResDTO.builder()
                .id(post.getId())
                .userId(post.getUser() != null ? post.getUser().getId() : null)
                .userName(post.getUser() != null ? post.getUser().getUserName() : "익명")
                .title(post.getTitle())
                .content(post.getContent())
                .views(post.getViews())
                // .likes((int) likeCount)
                .isNotice(post.isNotice())
                .createdAt(post.getCreatedAt())
                // .isLiked(isLiked)
                .build();
    }

    @Override
    public Page<SimplePostResDTO> searchPost(String type, String query, Pageable pageable) {
        return loadPostPort.findBySearch(type, query, pageable);
    }
}
