package jaeik.bimillog.domain.friend.entity;

import lombok.*;

import java.util.*;
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
     * <h3>친구 후보자 정보 내부 클래스</h3>
     * <p>각 후보자의 연결 정보, 상호작용 점수, 공통 친구 정보를 담습니다.</p>
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
         * 연결된 친구들 ID 집합
         * <p>2촌의 경우: 연결된 1촌 친구들</p>
         * <p>3촌의 경우: 연결된 2촌 친구들</p>
         */
        private Set<Long> connectedFriendIds;

        /**
         * 상호작용 점수
         */
        private Double interactionScore;

        /**
         * 공통 친구 ID 집합
         */
        private Set<Long> commonFriendIds;

        /**
         * 연결된 친구 없이 기본 정보만으로 생성
         */
        public static CandidateInfo of(Long candidateId, Set<Long> connectedFriendIds) {
            return CandidateInfo.builder()
                    .candidateId(candidateId)
                    .connectedFriendIds(connectedFriendIds)
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
}
