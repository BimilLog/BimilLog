package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.in.PostCacheUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostCacheCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostCacheQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostCacheSyncPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>게시글 캐시 동기화 서비스</h2>
 * <p>게시글 관련 캐시 데이터를 주기적으로 업데이트하고 이벤트에 따라 캐시를 무효화하는 서비스 클래스입니다.</p>
 * <p>주로 인기 게시글 및 공지사항 캐시를 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheSyncService implements PostCacheUseCase {

    private final PostCacheCommandPort postCacheCommandPort;
    private final PostCacheQueryPort postCacheQueryPort;
    private final PostCacheSyncPort postCacheSyncPort;
    private final PostQueryPort postQueryPort;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>실시간 인기 게시글 업데이트</h3>
     * <p>30분마다 실시간 인기 게시글을 업데이트하고 캐시합니다.</p>
     * <p>이전 플래그를 초기화하고 새로운 인기 게시글에 플래그를 적용합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedRate = 60000 * 30) // 30분마다
    @Transactional
    public void updateRealtimePopularPosts() {
        postCacheCommandPort.resetPopularFlag(PostCacheFlag.REALTIME);
        List<PostSearchResult> posts = postCacheSyncPort.findRealtimePopularPosts();
        if (!posts.isEmpty()) {
            List<Long> postIds = posts.stream().map(PostSearchResult::getId).collect(Collectors.toList());
            postCacheCommandPort.applyPopularFlag(postIds, PostCacheFlag.REALTIME);
            
            // 상세 정보 조회 후 목록 + 상세 캐시를 한번에 처리
            List<PostDetail> fullPosts = posts.stream()
                    .map(post -> postCacheSyncPort.findPostDetail(post.getId()))
                    .filter(fullPost -> fullPost != null)
                    .collect(Collectors.toList());
            
            postCacheCommandPort.cachePostsWithDetails(PostCacheFlag.REALTIME, fullPosts);
        }
    }

    /**
     * <h3>주간 인기 게시글 업데이트</h3>
     * <p>1일마다 주간 인기 게시글을 업데이트하고 캐시하며, 관련 사용자에게 알림 이벤트를 발행합니다.</p>
     * <p>이전 플래그를 초기화하고 새로운 인기 게시글에 플래그를 적용합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedRate = 60000 * 1440) // 1일마다
    @Transactional
    public void updateWeeklyPopularPosts() {
        postCacheCommandPort.resetPopularFlag(PostCacheFlag.WEEKLY);
        List<PostSearchResult> posts = postCacheSyncPort.findWeeklyPopularPosts();
        if (!posts.isEmpty()) {
            List<Long> postIds = posts.stream().map(PostSearchResult::getId).collect(Collectors.toList());
            postCacheCommandPort.applyPopularFlag(postIds, PostCacheFlag.WEEKLY);
            
            // 상세 정보 조회 후 목록 + 상세 캐시를 한번에 처리
            List<PostDetail> fullPosts = posts.stream()
                    .map(post -> postCacheSyncPort.findPostDetail(post.getId()))
                    .filter(fullPost -> fullPost != null)
                    .collect(Collectors.toList());
            
            postCacheCommandPort.cachePostsWithDetails(PostCacheFlag.WEEKLY, fullPosts);
            
            // 사용자에게 알림 이벤트 발행
            posts.forEach(post -> {
                if (post.getUserId() != null) {
                    eventPublisher.publishEvent(new PostFeaturedEvent(
                            this,
                            post.getUserId(),
                            "주간 인기 게시글로 선정되었어요!",
                            post.getId(),
                            "주간 인기 게시글 선정",
                            "회원님의 게시글 \'" + post.getTitle() + "\'이 주간 인기 게시글로 선정되었습니다."
                    ));
                }
            });
        }
    }
    
    /**
     * <h3>전설의 게시글 업데이트</h3>
     * <p>1일마다 전설의 게시글을 업데이트하고 캐시하며, 관련 사용자에게 알림 이벤트를 발행합니다.</p>
     * <p>이전 플래그를 초기화하고 새로운 전설의 게시글에 플래그를 적용합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Scheduled(fixedRate = 60000 * 1440) // 1일마다
    @Transactional
    public void updateLegendaryPosts() {
        postCacheCommandPort.resetPopularFlag(PostCacheFlag.LEGEND);
        List<PostSearchResult> posts = postCacheSyncPort.findLegendaryPosts();
        if (!posts.isEmpty()) {
            List<Long> postIds = posts.stream().map(PostSearchResult::getId).collect(Collectors.toList());
            postCacheCommandPort.applyPopularFlag(postIds, PostCacheFlag.LEGEND);
            
            // 상세 정보 조회 후 목록 + 상세 캐시를 한번에 처리
            List<PostDetail> fullPosts = posts.stream()
                    .map(post -> postCacheSyncPort.findPostDetail(post.getId()))
                    .filter(fullPost -> fullPost != null)
                    .collect(Collectors.toList());
            
            postCacheCommandPort.cachePostsWithDetails(PostCacheFlag.LEGEND, fullPosts);
            
            // 사용자에게 알림 이벤트 발행
            posts.forEach(post -> {
                if (post.getUserId() != null) {
                    eventPublisher.publishEvent(new PostFeaturedEvent(
                            this,
                            post.getUserId(),
                            "명예의 전당에 등극했어요!",
                            post.getId(),
                            "명예의 전당 등극",
                            "회원님의 게시글 \'" + post.getTitle() + "\'이 명예의 전당에 등극했습니다."
                    ));
                }
            });
        }
    }

    /**
     * <h3>공지 캐시 삭제</h3>
     * <p>공지사항 관련 캐시를 삭제합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteNoticeCache() {
        postCacheCommandPort.deletePopularPostsCache(PostCacheFlag.NOTICE);
    }

    @Override
    public void addSingleNoticeToCache(Long postId) {
        // 캐시가 없으면 전체 공지 캐시를 재생성
        if (!postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.NOTICE)) {
            List<PostSearchResult> allNotices = postCacheQueryPort.findAllNotices();
            if (!allNotices.isEmpty()) {
                // PostSearchResult -> PostDetail 변환 후 통합 캐시 메서드 사용
                List<PostDetail> allNoticeDetails = allNotices.stream()
                        .map(notice -> postCacheSyncPort.findPostDetail(notice.getId()))
                        .filter(detail -> detail != null)
                        .collect(Collectors.toList());
                
                if (!allNoticeDetails.isEmpty()) {
                    postCacheCommandPort.cachePostsWithDetails(PostCacheFlag.NOTICE, allNoticeDetails);
                }
            }
            return;
        }

        // 이미 캐시에 있는지 확인
        if (postCacheQueryPort.existsInNoticeCache(postId)) {
            return;
        }

        // 전체 공지 목록을 조회해서 목록+상세 캐시 재생성
        List<PostSearchResult> allNotices = postCacheQueryPort.findAllNotices();
        if (!allNotices.isEmpty()) {
            List<PostDetail> allNoticeDetails = allNotices.stream()
                    .map(notice -> postCacheSyncPort.findPostDetail(notice.getId()))
                    .filter(detail -> detail != null)
                    .collect(Collectors.toList());
            
            if (!allNoticeDetails.isEmpty()) {
                postCacheCommandPort.cachePostsWithDetails(PostCacheFlag.NOTICE, allNoticeDetails);
            }
        }
    }

    @Override
    public void removeSingleNoticeFromCache(Long postId) {
        // 캐시가 있는 경우에만 전체 공지 목록 재생성
        if (postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.NOTICE)) {
            List<PostSearchResult> allNotices = postCacheQueryPort.findAllNotices();
            if (!allNotices.isEmpty()) {
                List<PostDetail> allNoticeDetails = allNotices.stream()
                        .map(notice -> postCacheSyncPort.findPostDetail(notice.getId()))
                        .filter(detail -> detail != null)
                        .collect(Collectors.toList());
                
                if (!allNoticeDetails.isEmpty()) {
                    postCacheCommandPort.cachePostsWithDetails(PostCacheFlag.NOTICE, allNoticeDetails);
                } else {
                    // 공지가 없으면 캐시 삭제
                    postCacheCommandPort.deletePopularPostsCache(PostCacheFlag.NOTICE);
                }
            } else {
                // 공지가 없으면 캐시 삭제
                postCacheCommandPort.deletePopularPostsCache(PostCacheFlag.NOTICE);
            }
        }
    }
}
