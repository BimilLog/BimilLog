package jaeik.bimillog.domain.post.util;

import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostUtil {
    private final PostToMemberAdapter postToMemberAdapter;
    private final PostQueryRepository postQueryRepository;
    private final RedisPostHashAdapter redisPostHashAdapter;

    /**
     * <h3>게시글에서 블랙리스트 제거</h3>
     */
    public List<PostSimpleDetail> removePostsWithBlacklist(Long memberId, List<PostSimpleDetail> posts) {
        if (memberId == null || posts.isEmpty()) {
            return posts;
        }

        List<Long> blacklistIds = postToMemberAdapter.getInterActionBlacklist(memberId);
        Set<Long> blacklistSet = new HashSet<>(blacklistIds);
        return posts.stream().filter(post -> !blacklistSet.contains(post.getMemberId())).collect(Collectors.toList());
    }


    /**
     * 스케줄러 갱신과 비동기 삭제 간 경합으로 누락된 Hash를 DB에서 복구
     */
    public List<PostSimpleDetail> recoverMissingHashes(List<Long> orderedIds, List<PostSimpleDetail> cachedPosts) {
        if (cachedPosts.size() >= orderedIds.size()) {
            return cachedPosts;
        }

        Set<Long> cachedIds = cachedPosts.stream()
                .map(PostSimpleDetail::getId)
                .collect(Collectors.toSet());

        List<Long> missingIds = orderedIds.stream()
                .filter(id -> !cachedIds.contains(id))
                .toList();

        if (missingIds.isEmpty()) {
            return cachedPosts;
        }

        List<PostSimpleDetail> dbPosts = missingIds.stream()
                .map(id -> postQueryRepository.findPostDetail(id, null).orElse(null))
                .filter(Objects::nonNull)
                .map(PostDetail::toSimpleDetail)
                .toList();

        dbPosts.forEach(redisPostHashAdapter::createPostHash);

        List<PostSimpleDetail> result = new ArrayList<>(cachedPosts);
        result.addAll(dbPosts);
        return result;
    }

    /**
     * orderedIds 순서대로 정렬
     */
    public List<PostSimpleDetail> orderByIds(List<Long> orderedIds, List<PostSimpleDetail> posts) {
        Map<Long, PostSimpleDetail> postMap = posts.stream()
                .collect(Collectors.toMap(PostSimpleDetail::getId, p -> p, (a, b) -> a));

        return orderedIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public Page<PostSimpleDetail> paginate(List<PostSimpleDetail> posts, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), posts.size());

        if (start >= posts.size()) {
            return new PageImpl<>(List.of(), pageable, posts.size());
        }

        return new PageImpl<>(posts.subList(start, end), pageable, posts.size());
    }
}
