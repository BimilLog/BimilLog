package jaeik.bimillog.domain.friend.recommend;


import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * <h2>친구 추천 후보자 탐색기</h2>
 * <p>사용자의 친구 관계 상태(1촌 유무, 2촌 유무)에 따라<br>
 * 적절한 방식(BFS 또는 상호작용 점수)으로 추천 후보자 목록을 생성합니다.</p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FriendCandidateFinder {
    private final RedisFriendshipRepository redisFriendshipRepository;
    private final RedisInteractionScoreRepository redisInteractionScoreRepository;
    private final BreadthFirstSearch breadthFirstSearch;
    private final FriendRecommendScorer scorer;

    private static final int FIRST_FRIEND_COUNT = 200;
    private static final int RECOMMEND_LIMIT = 10;

    /**
     * 상황에 맞는 최적의 추천 후보자 리스트를 반환합니다.
     */
    public List<RecommendCandidate> findCandidates(Long memberId) {
        // 1. 1촌 친구 조회
        Set<Long> firstDegree = redisFriendshipRepository.getFriends(memberId, FIRST_FRIEND_COUNT);
        
        // [전략 1] 1촌이 아예 없으면 -> 상호작용 기반 추천
        if (firstDegree.isEmpty()) {
            return findFromInteractionOnly(memberId);
        }

        // 2. BFS로 2촌 탐색
        FriendRelation relation = breadthFirstSearch.findFriendRelation(memberId, firstDegree);
        
        // [전략 2] 2촌이 한 명도 없으면 -> 상호작용 기반 추천
        if (relation.getSecondDegreeIds().isEmpty()) {
            return findFromInteractionOnly(memberId);
        }

        // 3. 2촌이 부족하면 3촌까지 확장 (BFS 계속 진행)
        if (relation.getSecondDegreeIds().size() < RECOMMEND_LIMIT) {
            relation = breadthFirstSearch.addThirdDegreeRelation(relation);
        }

        // 4. 후보자들에게 상호작용 점수 주입
        injectInteractionScores(memberId, relation);

        // 5. 도메인 모델을 통해 후보자 리스트 생성 및 반환
        return relation.toCandidates(scorer);
    }

    /**
     * 상호작용 점수 테이블만 사용하여 후보자를 생성합니다. (Fallback)
     */
    private List<RecommendCandidate> findFromInteractionOnly(Long memberId) {
        log.info("추천 후보 부족으로 상호작용 점수 기반 탐색 수행: memberId={}", memberId);
        
        Map<Long, Double> interactionScores = redisInteractionScoreRepository.getAllInteractionScores(memberId);
        List<RecommendCandidate> candidates = new ArrayList<>();

        for (Map.Entry<Long, Double> entry : interactionScores.entrySet()) {
            if (entry.getKey().equals(memberId)) continue;

            RecommendCandidate candidate = RecommendCandidate.builder()
                    .memberId(entry.getKey())
                    .interactionScore(entry.getValue())
                    .build();

            scorer.calculateAndSetScore(candidate);
            candidates.add(candidate);
        }
        return candidates;
    }

    private void injectInteractionScores(Long memberId, FriendRelation relation) {
        Set<Long> allCandidateIds = relation.getAllCandidateIds();
        Map<Long, Double> scores = redisInteractionScoreRepository.getInteractionScoresBatch(memberId, allCandidateIds);

        for (Map.Entry<Long, Double> entry : scores.entrySet()) {
            relation.setInteractionScore(entry.getKey(), entry.getValue());
        }
    }
}