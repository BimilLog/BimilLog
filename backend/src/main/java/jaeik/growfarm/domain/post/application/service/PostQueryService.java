package jaeik.growfarm.domain.post.application.service;

import jaeik.growfarm.domain.post.application.assembler.PostAssembler;
import jaeik.growfarm.domain.post.application.port.in.PostQueryUseCase;
import jaeik.growfarm.domain.post.application.port.out.*;
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

/**
 * <h2>PostQueryService</h2>
 * <p>
 *     PostQueryUseCase의 구현체입니다.
 *     게시글 조회 관련 비즈니스 로직을 처리합니다.
 * </p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostQueryService implements PostQueryUseCase {

    private final PostQueryPort postQueryPort;
    private final PostLikeQueryPort postLikeQueryPort;
    private final LoadUserPort loadUserPort;
    private final LoadPostCachePort loadPostCachePort;
    private final ManagePostCachePort managePostCachePort;
    private final PostCacheManageService postCacheManageService;
    private final PostAssembler postAssembler;


    /**
     * <h3>게시판 조회</h3>
     * <p>최신순으로 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimplePostResDTO> getBoard(Pageable pageable) {
        return postQueryPort.findByPage(pageable);
    }

    /**
     * <h3>게시글 상세 조회</h3>
     * <p>게시글 ID를 통해 게시글 상세 정보를 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @param userId 현재 로그인한 사용자 ID (Optional, 좋아요 여부 확인용)
     * @return 게시글 상세 정보 DTO
     * @throws CustomException 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public FullPostResDTO getPost(Long postId, Long userId) {
        Post post = postQueryPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        long likeCount = postLikeQueryPort.countByPost(post);
        boolean isLiked = false;
        if (userId != null) {
            User user = loadUserPort.getReferenceById(userId);
            isLiked = postLikeQueryPort.existsByUserAndPost(user, post);
        }

        return postAssembler.toFullPostResDTO(post, likeCount, isLiked);
    }

    /**
     * <h3>게시글 검색</h3>
     * <p>검색 유형과 검색어를 통해 게시글을 검색하고 최신순으로 페이지네이션합니다.</p>
     *
     * @param type     검색 유형 (예: title, content, writer)
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimplePostResDTO> searchPost(String type, String query, Pageable pageable) {
        return postQueryPort.findBySearch(type, query, pageable);
    }

    /**
     * <h3>인기 게시글 목록 조회</h3>
     * <p>캐시된 인기 게시글 목록(실시간, 주간, 레전드)을 조회합니다. 캐시가 없는 경우 업데이트 후 조회합니다.</p>
     *
     * @param type 조회할 인기 게시글 유형
     * @return 인기 게시글 목록
     * @throws CustomException 유효하지 않은 캐시 유형인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<SimplePostResDTO> getPopularPosts(PostCacheFlag type) {
        if (!loadPostCachePort.hasPopularPostsCache(type)) {
            switch (type) {
                case REALTIME -> postCacheManageService.updateRealtimePopularPosts();
                case WEEKLY -> postCacheManageService.updateWeeklyPopularPosts();
                case LEGEND -> postCacheManageService.updateLegendaryPosts();
                default -> throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
        return loadPostCachePort.getCachedPopularPosts(type);
    }

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>캐시된 공지사항 목록을 조회합니다. 캐시가 없는 경우 업데이트 후 조회합니다.</p>
     *
     * @return 공지사항 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<SimplePostResDTO> getNoticePosts() {
        if (!loadPostCachePort.hasPopularPostsCache(PostCacheFlag.NOTICE)) {
            List<SimplePostResDTO> noticePosts = postQueryPort.findNoticePosts();
            managePostCachePort.cachePosts(PostCacheFlag.NOTICE, noticePosts);
        }
        return loadPostCachePort.getCachedPopularPosts(PostCacheFlag.NOTICE);
    }

    /**
     * <h3>게시글 ID로 조회 (내부 도메인용)</h3>
     * <p>다른 도메인에서 게시글 엔티티가 필요한 경우 사용하는 메소드입니다.</p>
     *
     * @param postId 게시글 ID
     * @return 게시글 엔티티 (Optional)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public java.util.Optional<Post> findById(Long postId) {
        return postQueryPort.findById(postId);
    }

    /**
     * <h3>사용자 작성 게시글 목록 조회 (도메인 간 연동용)</h3>
     * <p>특정 사용자가 작성한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimplePostResDTO> getUserPosts(Long userId, Pageable pageable) {
        return postQueryPort.findPostsByUserId(userId, pageable);
    }

    /**
     * <h3>사용자 좋아요한 게시글 목록 조회 (도메인 간 연동용)</h3>
     * <p>특정 사용자가 좋아요한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 좋아요한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimplePostResDTO> getUserLikedPosts(Long userId, Pageable pageable) {
        return postQueryPort.findLikedPostsByUserId(userId, pageable);
    }
}
