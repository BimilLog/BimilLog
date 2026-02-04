package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostReadModelQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisFirstPagePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.FIRST_PAGE_SIZE;

/**
 * <h2>첫 페이지 캐시 서비스</h2>
 * <p>게시판 첫 페이지 캐시 조회 및 분기 로직을 담당합니다.</p>
 *
 * <h3>캐시 미스 정책:</h3>
 * <ul>
 *     <li>정상 상황: @Scheduled(fixedRate)가 서버 시작 즉시 실행되어 캐시 미스 없음</li>
 *     <li>Redis 장애: DB 폴백 조회</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FirstPageCacheService {

    private final RedisFirstPagePostAdapter redisFirstPagePostAdapter;
    private final PostReadModelQueryRepository postReadModelQueryRepository;
    private final PostToMemberAdapter postToMemberAdapter;

    /**
     * <h3>첫 페이지 조회</h3>
     * <p>캐시에서 게시글 목록을 조회하고, 회원인 경우 블랙리스트 필터링을 적용합니다.</p>
     *
     * @param memberId 회원 ID (null이면 비회원)
     * @return 게시글 목록 (블랙리스트 필터링 적용됨)
     */
    public List<PostSimpleDetail> getFirstPage(Long memberId) {
        // 1. 캐시 조회
        List<PostSimpleDetail> posts = redisFirstPagePostAdapter.getFirstPage();

        // 2. 캐시 미스 시 DB 조회 (Redis 장애 대비 - 정상 시 발생 안 함)
        if (posts.isEmpty()) {
            log.warn("[FIRST_PAGE_CACHE] 캐시 미스 - DB 폴백");
            posts = postReadModelQueryRepository.findBoardPostsByCursor(null, FIRST_PAGE_SIZE);

            // findBoardPostsByCursor는 size+1 반환하므로 자르기
            if (posts.size() > FIRST_PAGE_SIZE) {
                posts = posts.subList(0, FIRST_PAGE_SIZE);
            }
        }

        // 3. 블랙리스트 필터링 (회원만)
        if (memberId != null) {
            posts = removePostsWithBlacklist(memberId, posts);
        }

        return posts;
    }

    /**
     * <h3>첫 페이지 요청 여부 판단</h3>
     * <p>커서가 없고 요청 크기가 20 이하일 때 첫 페이지로 간주</p>
     *
     * @param cursor 커서 (마지막 조회 게시글 ID)
     * @param size   요청 크기
     * @return 첫 페이지 요청이면 true
     */
    public boolean isFirstPage(Long cursor, int size) {
        return cursor == null && size <= FIRST_PAGE_SIZE;
    }

    /**
     * <h3>블랙리스트 필터링</h3>
     * <p>회원의 블랙리스트에 포함된 작성자의 게시글을 제외합니다.</p>
     */
    private List<PostSimpleDetail> removePostsWithBlacklist(Long memberId, List<PostSimpleDetail> posts) {
        if (memberId == null || posts.isEmpty()) {
            return posts;
        }

        List<Long> blacklistIds = postToMemberAdapter.getInterActionBlacklist(memberId);
        Set<Long> blacklistSet = new HashSet<>(blacklistIds);

        return posts.stream()
                .filter(post -> !blacklistSet.contains(post.getMemberId()))
                .collect(Collectors.toList());
    }
}
