package jaeik.growfarm.repository.post;

import jaeik.growfarm.dto.post.PostDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>PostCustomRepositoryImpl</h2>
 * <p>
 * 게시글 관련 커스텀 쿼리 메소드 구현체
 * </p>
 *
 * @author jaeik
 * @version 1.0.21
 */
@Repository
@RequiredArgsConstructor
public class PostCustomRepositoryImpl implements PostCustomRepository, 
        PostQueryRepository, PostSearchRepository, PostPopularRepository, PostUserRepository, PostCacheRepository {

        private final PostCustomSearchRepositoryImpl postSearchRepository;
        private final PostCustomPopularRepositoryImpl popularPostRepository;
        private final PostCustomUserRepositoryImpl userManagementRepository;

        /**
         * <h3>게시글 목록 조회</h3>
         * <p>
         * 최신순으로 페이징하여 게시글 목록을 조회한다.
         * </p>
         * <p>
         * 게시글 당 댓글 수, 추천 수를 반환한다.
         * </p>
         *
         * @return 게시글 목록 페이지
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        @Transactional(readOnly = true)
        public Page<SimplePostDTO> findPostsWithCommentAndLikeCounts(Pageable pageable) {
                return postSearchRepository.findPostsWithCommentAndLikeCounts(pageable);
        }

        /**
         * <h3>게시글 검색</h3>
         * <p>
         * 검색어와 검색 유형에 따라 게시글을 검색한다.
         * </p>
         * <p>
         * 게시글 마다의 총 댓글 수, 총 추천 수를 반환한다.
         * </p>
         *
         * @param keyword    검색어
         * @param searchType 검색 유형 (TITLE, TITLE_CONTENT, AUTHOR 등)
         * @param pageable   페이지 정보
         * @return 검색된 게시글 페이지
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        @Transactional(readOnly = true)
        public Page<SimplePostDTO> searchPosts(String keyword, String searchType, Pageable pageable) {
                return postSearchRepository.searchPosts(keyword, searchType, pageable);
        }

        /**
         * <h3>게시글 상세 조회</h3>
         * <p>
         * 게시글 정보, 좋아요 수, 사용자 좋아요 여부를 조회한다.
         * </p>
         *
         * @param postId 게시글 ID
         * @param userId 사용자 ID (null 가능)
         * @return PostDTO 게시글 상세 정보
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        @Transactional(readOnly = true)
        public PostDTO findPostById(Long postId, Long userId) {
                return postSearchRepository.findPostById(postId, userId);
        }

        /**
         * <h3>실시간 인기글 선정</h3>
         * <p>
         * 1일 이내의 글 중 추천 수가 가장 높은 상위 5개를 실시간 인기글로 등록한다.
         * </p>
         *
         * @return 실시간 인기글 목록
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        @Transactional
        public List<SimplePostDTO> updateRealtimePopularPosts() {
                return popularPostRepository.updateRealtimePopularPosts();
        }

        /**
         * <h3>주간 인기글 선정</h3>
         * <p>
         * 7일 이내의 글 중 추천 수가 가장 높은 상위 5개를 주간 인기글로 등록한다.
         * </p>
         *
         * @return 주간 인기글 목록
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        @Transactional
        public List<SimplePostDTO> updateWeeklyPopularPosts() {
                return popularPostRepository.updateWeeklyPopularPosts();
        }

        /**
         * <h3>레전드 게시글 선정</h3>
         * <p>
         * 추천 수가 20개 이상인 모든 게시글을 레전드 게시글로 등록한다.
         * </p>
         *
         * @return 레전드 게시글 목록
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        @Transactional
        public List<SimplePostDTO> updateLegendPosts() {
                return popularPostRepository.updateLegendPosts();
        }

        /**
         * <h3>사용자 작성 글 목록 조회</h3>
         * <p>
         * 사용자 ID를 기준으로 해당 사용자가 작성한 글 목록을 조회한다.
         * </p>
         *
         * @param userId   사용자 ID
         * @param pageable 페이지 정보
         * @return 사용자가 작성한 글 목록
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        @Transactional(readOnly = true)
        public Page<SimplePostDTO> findPostsByUserId(Long userId, Pageable pageable) {
                return userManagementRepository.findPostsByUserId(userId, pageable);
        }

        /**
         * <h3>사용자가 추천한 글 목록 조회</h3>
         * <p>
         * 사용자 ID를 기준으로 해당 사용자가 추천한 글 목록을 조회한다.
         * </p>
         *
         * @param userId   사용자 ID
         * @param pageable 페이지 정보
         * @return 사용자가 추천한 글 목록
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        @Transactional(readOnly = true)
        public Page<SimplePostDTO> findLikedPostsByUserId(Long userId, Pageable pageable) {
                return userManagementRepository.findLikedPostsByUserId(userId, pageable);
        }

        /**
         * <h3>게시글 삭제 및 Redis 캐시 동기화</h3>
         * <p>
         * 게시글을 삭제하고, 해당 게시글이 인기글(실시간/주간/레전드)인 경우
         * Redis 캐시에서도 즉시 해당 인기글 목록을 삭제한다.
         * </p>
         *
         * @param postId 삭제할 게시글 ID
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        @Transactional
        public void deletePostWithCacheSync(Long postId) {
                userManagementRepository.deletePostWithCacheSync(postId);
        }
}
