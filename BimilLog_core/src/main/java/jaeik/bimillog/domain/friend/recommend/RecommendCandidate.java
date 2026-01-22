package jaeik.bimillog.domain.friend.recommend;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * <h2>친구 추천 후보자 정보를 담는 클래스</h2>
 * <p>
 * BFS 탐색 과정에서 발견된 추천 후보자의 정보와 점수를 관리합니다.
 * 2촌, 3촌 여부에 따라 다른 점수 계산 로직을 적용합니다.
 * </p>
 *
 * <h3>점수 계산 공식:</h3>
 * <ul>
 *   <li><b>2촌:</b> 기본 50점 + 공통친구 수 × 2 (최대 20점) + 상호작용 점수 (최대 10점)</li>
 *   <li><b>3촌:</b> 기본 20점 + 가상점수 (최대 5점) + 상호작용 점수 (최대 10점)</li>
 *   <li><b>기타(0촌):</b> 상호작용 점수만 적용 (최대 10점)</li>
 * </ul>
 *
 * @version 2.6.0
 * @author Jaeik
 */
@Getter
@Setter
@Builder
public class RecommendCandidate {
    // 추천 후보자 회원 ID
    private Long memberId;

    // 촌수 (2: 친구의 친구, 3: 친구의 친구의 친구, 0: 기타)
    private int depth;

    // 2촌용 공통친구 ID 집합 (나와 후보자가 공통으로 아는 친구)
    @Builder.Default
    private Set<Long> commonFriends = new HashSet<>();

    // 3촌용 공통친구 점수 (연결 고리가 되는 2촌의 공통친구 수 기반)
    @Builder.Default
    private double mutualThreeDegreeScore = 0.0;

    // 상호작용 점수 (롤링페이퍼, 좋아요 등 과거 활동 기반)
    @Builder.Default
    private double interactionScore = 0.0;

    // 화면 표시용: 함께 아는 친구(대표 1명) ID
    private Long acquaintanceId;

    // 화면 표시용: 함께 아는 친구가 2명 이상인지 여부
    private boolean manyAcquaintance;

    /**
     * 팩토리 메서드: 주어진 회원 ID와 촌수로 추천 후보자를 생성합니다.
     *
     * @param memberId 추천 후보자 회원 ID
     * @param depth    촌수 (2, 3, 또는 0)
     * @return 새로운 RecommendCandidate 인스턴스
     */
    public static RecommendCandidate of(Long memberId, int depth) {
        return RecommendCandidate.builder()
                .memberId(memberId)
                .depth(depth)
                .commonFriends(new HashSet<>())
                .build();
    }

    /**
     * <h3>공통 친구를 추가합니다 (2촌 전용).</h3>
     * <p>
     * 첫 번째로 추가되는 친구는 화면 표시용 대표 친구(acquaintanceId)로 설정됩니다.
     * 공통 친구가 2명 이상이면 manyAcquaintance 플래그가 설정됩니다.
     * </p>
     *
     * @param friendId 공통 친구 회원 ID
     */
    public void addCommonFriend(Long friendId) {
        this.commonFriends.add(friendId);
        if (this.acquaintanceId == null) this.acquaintanceId = friendId; // 첫 친구를 대표로
        if (this.commonFriends.size() >= 2) this.manyAcquaintance = true;
    }

    /**
     * <h3>가상 점수를 추가합니다 (3촌 전용).</h3>
     * <p>
     * 연결 고리가 되는 2촌의 공통친구 수에 0.5 가중치를 적용하여 가산합니다.
     * </p>
     *
     * @param score 연결 고리 2촌의 공통친구 수
     */
    public void addVirtualScore(int score) {
        this.mutualThreeDegreeScore += (score * 0.5);
    }

    /**
     * <h3>총점을 계산합니다.</h3>
     * <p>
     * 촌수에 따른 기본 점수, 공통친구/가상 점수, 상호작용 점수를 합산합니다.
     * </p>
     *
     * <h4>점수 구성:</h4>
     * <ul>
     *   <li><b>기본 점수:</b> 2촌=50, 3촌=20, 기타=0</li>
     *   <li><b>공통친구 점수 (2촌):</b> min(공통친구 수, 10) × 2</li>
     *   <li><b>가상 점수 (3촌):</b> min(virtualScore, 5)</li>
     *   <li><b>상호작용 점수:</b> min(interactionScore, 10)</li>
     * </ul>
     *
     * @return 총점 (0 ~ 80점 범위)
     */
    public double calculateTotalScore() {
        double base = (depth == 2) ? 50 : (depth == 3) ? 20 : 0;
        double commonScore = (depth == 2)
                ? Math.min(commonFriends.size(), 10) * 2
                : Math.min(mutualThreeDegreeScore, 5);
        double interaction = Math.min(interactionScore, 10.0);

        return base + commonScore + interaction;
    }
}