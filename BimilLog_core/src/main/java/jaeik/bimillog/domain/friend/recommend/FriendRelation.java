package jaeik.bimillog.domain.friend.recommend;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>친구 관계 도메인 모델</h2>
 * <p>1촌, 2촌, 3촌 관계 정보를 캡슐화하고 관리합니다.</p>
 * <p>자신의 데이터를 기반으로 추천 후보자(RecommendCandidate) 목록을 생성하는 책임을 가집니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Getter
public class FriendRelation {
    private final Long memberId;
    private final Set<Long> firstDegreeIds;

    private final List<CandidateInfo> secondDegreeCandidates = new ArrayList<>();
    private final List<CandidateInfo> thirdDegreeCandidates = new ArrayList<>();

    // 조회 성능을 위해 ID만 별도 관리
    private final Set<Long> secondDegreeIds = new HashSet<>();

    public FriendRelation(Long memberId, Set<Long> firstDegreeIds) {
        this.memberId = memberId;
        this.firstDegreeIds = firstDegreeIds;
    }

    // --- 데이터 구성 메서드 ---

    public void addSecondDegreeCandidate(CandidateInfo info) {
        secondDegreeCandidates.add(info);
        secondDegreeIds.add(info.getCandidateId());
    }

    public void addThirdDegreeCandidate(CandidateInfo info) {
        thirdDegreeCandidates.add(info);
    }

    public Set<Long> getAllCandidateIds() {
        Set<Long> ids = new HashSet<>(secondDegreeIds);
        for (CandidateInfo info : thirdDegreeCandidates) {
            ids.add(info.getCandidateId());
        }
        return ids;
    }

    public void setInteractionScore(Long candidateId, Double score) {
        // 단순 리스트 순회 (데이터 규모가 크지 않다고 가정)
        // 성능 최적화가 필요하다면 Map<Long, CandidateInfo> 구조 도입 고려
        findCandidate(candidateId).ifPresent(c -> c.setInteractionScore(score));
    }

    public void initializeCommonFriends() {
        // 기존 서비스 로직 호환용 (필요시 내부 초기화 로직 구현)
    }

    // --- 핵심 비즈니스 로직: 후보자 생성 ---

    /**
     * 내부의 관계 데이터(2촌, 3촌)와 상호작용 점수를 바탕으로
     * 점수가 계산된 추천 후보자 리스트를 반환합니다.
     *
     * @param scorer 점수 계산 전략
     * @return 점수가 매겨진 후보자 리스트
     */
    public List<RecommendCandidate> toCandidates(FriendRecommendScorer scorer) {
        List<RecommendCandidate> result = new ArrayList<>();

        // 1. 2촌 후보 변환
        for (CandidateInfo info : secondDegreeCandidates) {
            RecommendCandidate candidate = createCandidate(info, FriendRecommendScorer.DEPTH_SECOND);
            // 2촌은 실제 공통 친구 ID를 가짐
            info.getConnectionIds().forEach(candidate::addCommonFriend);

            scorer.calculateAndSetScore(candidate);
            result.add(candidate);
        }

        // 2. 3촌 후보 변환 (필요한 경우만)
        if (!thirdDegreeCandidates.isEmpty()) {
            addThirdDegreeCandidates(result, scorer);
        }

        return result;
    }

    private void addThirdDegreeCandidates(List<RecommendCandidate> result, FriendRecommendScorer scorer) {
        // 3촌 점수 계산 최적화를 위해 2촌의 공통친구 정보를 Map으로 캐싱
        Map<Long, Set<Long>> secondDegreeCommonMap = secondDegreeCandidates.stream()
                .collect(Collectors.toMap(CandidateInfo::getCandidateId, CandidateInfo::getConnectionIds));

        for (CandidateInfo info : thirdDegreeCandidates) {
            // 3촌의 공통 친구 수 = (나와 연결된 2촌)들이 가진 (나와의 공통친구) 수의 합
            int virtualCommonCount = 0;
            for (Long bridgeId : info.getConnectionIds()) { // 3촌에게 connection은 '중간다리 2촌'
                Set<Long> secondsCommons = secondDegreeCommonMap.getOrDefault(bridgeId, Collections.emptySet());
                virtualCommonCount += secondsCommons.size();
            }

            // 점수 계산용 가상 친구 Set 생성
            Set<Long> virtualFriends = new HashSet<>();
            for (int i = 0; i < virtualCommonCount; i++) virtualFriends.add((long) i);

            RecommendCandidate candidate = createCandidate(info, FriendRecommendScorer.DEPTH_THIRD);
            candidate.setCommonFriends(virtualFriends);

            scorer.calculateAndSetScore(candidate);
            result.add(candidate);
        }
    }

    private RecommendCandidate createCandidate(CandidateInfo info, int depth) {
        return RecommendCandidate.builder()
                .memberId(info.getCandidateId())
                .depth(depth)
                .commonFriends(new HashSet<>()) // 초기화
                .interactionScore(info.getInteractionScore())
                .build();
    }

    private Optional<CandidateInfo> findCandidate(Long id) {
        // 2촌 먼저 검색
        for (CandidateInfo c : secondDegreeCandidates) {
            if (c.getCandidateId().equals(id)) return Optional.of(c);
        }
        // 3촌 검색
        for (CandidateInfo c : thirdDegreeCandidates) {
            if (c.getCandidateId().equals(id)) return Optional.of(c);
        }
        return Optional.empty();
    }

    // --- 내부 데이터 클래스 ---

    @Getter
    @RequiredArgsConstructor(staticName = "of")
    public static class CandidateInfo {
        private final Long candidateId;

        // 2촌인 경우: 나와의 공통 친구(1촌) ID 목록
        // 3촌인 경우: 나와 연결해주는 다리 친구(2촌) ID 목록
        private final Set<Long> connectionIds;

        @Setter
        private Double interactionScore = 0.0;

        // 의미 명확화를 위한 Alias
        public Set<Long> getCommonFriendIds() { return connectionIds; }
        public Set<Long> getBridgeFriendIds() { return connectionIds; }
    }
}