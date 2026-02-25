package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.repository.FriendAdminQueryRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jaeik.bimillog.domain.friend.listener.FriendInteractionListener.INTERACTION_SCORE_DEFAULT;

/**
 * <h2>친구 도메인 Redis 복구 어드민 서비스</h2>
 * <p>Redis 데이터 유실 시 DB 데이터를 기반으로 친구 관계 및 상호작용 점수를 재구축합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendAdminService {

    private static final double INTERACTION_SCORE_LIMIT = 9.5;

    private final FriendAdminQueryRepository friendAdminQueryRepository;
    private final RedisFriendshipRepository redisFriendshipRepository;
    private final RedisInteractionScoreRepository redisInteractionScoreRepository;

    public record InteractionData(Long memberId, Long targetId, Long count) {
    }


    /**
     * <h3>친구 관계 Redis 전체 재구축</h3>
     * <p>DB의 friendship 테이블을 기반으로 Redis friend:* Set을 재구축합니다.</p>
     *
     * @return 처리 결과 메시지
     */
    public String rebuildFriendshipRedis() {
        List<long[]> pairs = friendAdminQueryRepository.getAllFriendshipPairs();
        redisFriendshipRepository.rebuildAll(pairs);
        return String.format("친구 관계 Redis 재구축 완료. 처리된 친구 쌍: %d개", pairs.size());
    }

    /**
     * <h3>상호작용 점수 Redis 전체 재구축</h3>
     * <p>DB의 post_like, comment, comment_like 집계를 기반으로 Redis interaction:* ZSet을 재구축합니다.</p>
     * <p>점수 계산: min(totalCount * 0.5, 9.5)</p>
     *
     * @return 처리 결과 메시지
     */
    public String rebuildInteractionScoreRedis() {
        // 2. 여러 저장소 결과를 하나의 Stream으로 합칩니다.
        List<InteractionData> allInteractions = Stream.of(
                friendAdminQueryRepository.getPostLikeInteractions(),
                friendAdminQueryRepository.getCommentInteractions(),
                friendAdminQueryRepository.getCommentLikeInteractions()
        ).flatMap(List::stream).toList();

        Map<Long, Map<Long, Double>> scoreMap = allInteractions.stream()
                .collect(Collectors.groupingBy(
                        InteractionData::memberId,
                        Collectors.groupingBy(
                                InteractionData::targetId,
                                Collectors.collectingAndThen(
                                        // targetId 별로 count를 모두 합산한 뒤
                                        Collectors.summingLong(InteractionData::count),
                                        // 합산된 totalCount를 기반으로 점수 계산 (min 처리)
                                        totalCount -> Math.min(totalCount * INTERACTION_SCORE_DEFAULT, INTERACTION_SCORE_LIMIT)
                                )
                        )
                ));

        redisInteractionScoreRepository.rebuildAll(scoreMap);

        long totalMembers = scoreMap.size();
        long totalEntries = scoreMap.values().stream().mapToLong(Map::size).sum();

        return String.format("상호작용 점수 Redis 재구축 완료. 대상 회원: %d명, 총 점수 항목: %d개", totalMembers, totalEntries);
    }
}

