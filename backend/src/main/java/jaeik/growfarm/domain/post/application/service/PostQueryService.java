package jaeik.growfarm.domain.post.application.service;

import jaeik.growfarm.domain.post.application.assembler.PostAssembler;
import jaeik.growfarm.domain.post.application.port.in.PostQueryUseCase;
import jaeik.growfarm.domain.post.application.port.out.CountPostLikePort;
import jaeik.growfarm.domain.post.application.port.out.ExistPostLikePort;
import jaeik.growfarm.domain.post.application.port.out.LoadPostCachePort;
import jaeik.growfarm.domain.post.application.port.out.LoadPostPort;
import jaeik.growfarm.domain.post.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.post.application.port.out.ManagePostCachePort;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    private final CountPostLikePort countPostLikePort;
    private final ExistPostLikePort existPostLikePort;
    private final LoadUserPort loadUserPort;
    private final LoadPostCachePort loadPostCachePort;
    private final ManagePostCachePort managePostCachePort;
    private final PostCacheManageService postCacheManageService;
    private final PostAssembler postAssembler;


    @Override
    public Page<SimplePostResDTO> getBoard(Pageable pageable) {
        return loadPostPort.findByPage(pageable);
    }

    @Override
    public FullPostResDTO getPost(Long postId, Long userId) {
        Post post = loadPostPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 조회수 증가 로직은 PostController로 이동 (Command와 Query 분리)
        // loadPostPort.incrementView(post);

        long likeCount = countPostLikePort.countByPost(post);
        boolean isLiked = false;
        if (userId != null) {
            User user = loadUserPort.getReferenceById(userId);
            isLiked = existPostLikePort.existsByUserAndPost(user, post);
        }

        return postAssembler.toFullPostResDTO(post, likeCount, isLiked);
    }

    @Override
    public Page<SimplePostResDTO> searchPost(String type, String query, Pageable pageable) {
        return loadPostPort.findBySearch(type, query, pageable);
    }

    @Override
    public List<SimplePostResDTO> getPopularPosts(PostCacheFlag type) {
        if (loadPostCachePort.hasPopularPostsCache(type)) {
            switch (type) {
                case REALTIME -> postCacheManageService.updateRealtimePopularPosts();
                case WEEKLY -> postCacheManageService.updateWeeklyPopularPosts();
                case LEGEND -> postCacheManageService.updateLegendaryPosts();
                default -> throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
        return loadPostCachePort.getCachedPopularPosts(type);
    }

    @Override
    public List<SimplePostResDTO> getNoticePosts() {
        if (loadPostCachePort.hasPopularPostsCache(PostCacheFlag.NOTICE)) {
            // 공지사항은 스케줄링이 없으므로, 필요 시 즉시 DB에서 조회하여 캐싱
            List<SimplePostResDTO> noticePosts = loadPostPort.findNoticePosts();
            managePostCachePort.cachePosts(PostCacheFlag.NOTICE, noticePosts);
        }
        return loadPostCachePort.getCachedPopularPosts(PostCacheFlag.NOTICE);
    }

    @Override
    public Optional<Post> findById(Long postId) {
        return loadPostPort.findById(postId);
    }
}
