package jaeik.bimillog.domain.friend.entity;

import jaeik.bimillog.domain.friend.algorithm.BreadthFirstSearch;
import jaeik.bimillog.domain.friend.service.FriendRecommendService;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>친구 관계 도메인 객체</h2>
 * <p>특정 회원의 1촌/2촌/3촌 관계 정보를 담는 도메인 객체입니다.</p>
 * <p>친구 추천 알고리즘에서 사용되며, 각 촌수별 ID와 연결 관계 정보를 포함합니다.</p>
 *
 * @author Jaeik
 * @version 2.2.0
 */
@Getter
@Setter
public class FriendRelation {
    /**
     * 본인 회원 ID
     */
    private Long memberId;

    /**
     * 1촌 친구 ID 집합
     */
    private Set<Long> firstDegreeIds;

    /**
     * 2촌 후보자 정보 목록
     */
    private List<CandidateInfo> secondDegreeCandidates;

    /**
     * 3촌 후보자 정보 목록
     */
    private List<CandidateInfo> thirdDegreeCandidates;

    /**
     * <h3>친구 추천 후보자 정보</h3>
     * <p>BFS 탐색으로 발견된 각 후보자의 연결 관계 및 점수 정보</p>
     *
     * <h4>주요 필드 설명</h4>
     * <ul>
     *   <li><b>bridgeFriendIds</b>: 본인과 후보자를 연결하는 중개 친구들
     *       <ul>
     *         <li>2촌: A → B → C 관계에서 B (1촌 친구)</li>
     *         <li>3촌: A → B → C → D 관계에서 C (2촌 친구)</li>
     *       </ul>
     *   </li>
     *   <li><b>commonFriendIds</b>: 실제 공통 친구로 확정된 ID들 (점수 계산용)</li>
     *   <li><b>interactionScore</b>: Redis에서 조회한 상호작용 점수 (0.0~10.0)</li>
     * </ul>
     *
     * @see FriendRecommendService
     * @see BreadthFirstSearch
     */
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CandidateInfo {
        /**
         * 후보자 회원 ID
         */
        private Long candidateId;

        /**
         * 다리 역할을 하는 중개자 친구 ID 집합
         * <p>2촌의 경우: 본인과 후보자를 연결하는 1촌 친구들 (예: A → B → C에서 B)</p>
         * <p>3촌의 경우: 본인과 후보자를 연결하는 2촌 친구들 (예: A → B → C → D에서 C)</p>
         */
        private Set<Long> bridgeFriendIds;

        /**
         * 상호작용 점수
         */
        private Double interactionScore;

        /**
         * 공통 친구 ID 집합
         */
        private Set<Long> commonFriendIds;

        /**
         * 기본 정보로 후보자 생성
         */
        public static CandidateInfo of(Long candidateId, Set<Long> bridgeFriendIds) {
            return CandidateInfo.builder()
                    .candidateId(candidateId)
                    .bridgeFriendIds(bridgeFriendIds)
                    .interactionScore(0.0)
                    .commonFriendIds(new HashSet<>())
                    .build();
        }
    }

    /**
     * 기본 생성자
     */
    public FriendRelation(Long memberId, Set<Long> firstDegreeIds) {
        this.memberId = memberId;
        this.firstDegreeIds = firstDegreeIds;
        this.secondDegreeCandidates = new ArrayList<>();
        this.thirdDegreeCandidates = new ArrayList<>();
    }

    /**
     * <h3>2촌 후보자 추가</h3>
     */
    public void addSecondDegreeCandidate(CandidateInfo candidate) {
        this.secondDegreeCandidates.add(candidate);
    }

    /**
     * <h3>3촌 후보자 추가</h3>
     */
    public void addThirdDegreeCandidate(CandidateInfo candidate) {
        this.thirdDegreeCandidates.add(candidate);
    }

    /**
     * <h3>특정 후보자의 상호작용 점수 설정</h3>
     */
    public void setInteractionScore(Long candidateId, Double score) {
        // 2촌 후보자 중 찾기
        secondDegreeCandidates.stream()
                .filter(c -> c.getCandidateId().equals(candidateId))
                .findFirst()
                .ifPresent(c -> c.setInteractionScore(score));

        // 3촌 후보자 중 찾기
        thirdDegreeCandidates.stream()
                .filter(c -> c.getCandidateId().equals(candidateId))
                .findFirst()
                .ifPresent(c -> c.setInteractionScore(score));
    }

    /**
     * <h3>모든 후보자 ID 집합 반환</h3>
     */
    public Set<Long> getAllCandidateIds() {
        Set<Long> allIds = new HashSet<>();
        secondDegreeCandidates.forEach(c -> allIds.add(c.getCandidateId()));
        thirdDegreeCandidates.forEach(c -> allIds.add(c.getCandidateId()));
        return allIds;
    }

    /**
     * <h3>2촌 후보자 ID 집합 반환</h3>
     */
    public Set<Long> getSecondDegreeIds() {
        return secondDegreeCandidates.stream()
                .map(CandidateInfo::getCandidateId)
                .collect(Collectors.toSet());
    }

    /**
     * <h3>공통 친구 정보 초기화</h3>
     * <p>각 후보자의 bridgeFriendIds를 commonFriendIds로 설정합니다.</p>
     * <p>2촌의 경우 bridgeFriendIds가 그대로 공통 친구 목록이 됩니다.</p>
     */
    public void initializeCommonFriends() {
        // 2촌: bridgeFriendIds가 이미 공통 친구 목록
        for (CandidateInfo candidate : secondDegreeCandidates) {
            candidate.setCommonFriendIds(new HashSet<>(candidate.getBridgeFriendIds()));
        }

        // 3촌: 빈 집합으로 초기화 (현재 로직에서는 가상 ID 사용)
        for (CandidateInfo candidate : thirdDegreeCandidates) {
            candidate.setCommonFriendIds(new HashSet<>());
        }
    }
}
