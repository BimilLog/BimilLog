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
 *   <li><b>2촌:</b> 기본 50점 + commonScore (최대 20점) + 상호작용 점수 (최대 10점)</li>
 *   <li><b>3촌:</b> 기본 20점 + commonScore (최대 5점) + 상호작용 점수 (최대 10점)</li>
 *   <li><b>기타(0촌):</b> 상호작용 점수만 적용 (최대 10점)</li>
 * </ul>
 *
 * <h3>commonScore 누적 방식:</h3>
 * <ul>
 *   <li><b>2촌:</b> 공통친구 발견 시 +2</li>
 *   <li><b>3촌 첫 생성:</b> 부모 2촌의 commonScore × 0.25</li>
 *   <li><b>3촌 추가 발견:</b> +0.5</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Getter
@Setter
@Builder
public class RecommendCandidate {
    // 추천 후보자 회원 ID
    private Long memberId;

    // 촌수 (2: 친구의 친구, 3: 친구의 친구의 친구, 0: 기타)
    private int depth;

    // 공통친구 ID 집합 (나와 후보자가 공통으로 아는 친구, 2촌용)
    @Builder.Default
    private Set<Long> commonFriends = new HashSet<>();

    // 공통 점수 (2촌: +2, 3촌 첫 생성: 부모×0.25, 3촌 추가: +0.5)
    @Builder.Default
    private double commonScore = 0.0;

    // 상호작용 점수 (롤링페이퍼, 좋아요 등 과거 활동 기반)
    @Builder.Default
    private double interactionScore = 0.0;

    // 화면 표시용: 함께 아는 친구(대표 1명) ID
    private Long acquaintanceId;

    // 화면 표시용: 함께 아는 친구가 2명 이상인지 여부
    private boolean manyAcquaintance;

    public static RecommendCandidate initialCandidate (Long memberId, int depth, double parentScore) {
        RecommendCandidate recommendCandidate = RecommendCandidate.builder()
                .memberId(memberId)
                .depth(depth)
                .commonFriends(new HashSet<>())
                .build();

        if (depth == 3) {
            recommendCandidate.commonScore = Math.min(parentScore * 0.25, 5);
        }
        return recommendCandidate;
    }

    public void addCommonFriendAndScore(Long friendId) {
        if (depth == 2) {
            commonFriends.add(friendId);
            if (acquaintanceId == null) {
                acquaintanceId = friendId;
            }

            if (commonFriends.size() >= 2) {
                this.manyAcquaintance = true;
            }

            if (commonScore < 20) {
                commonScore += 2;
            }
        } else if (depth == 3 && commonScore < 5) {
            commonScore += 0.5;
        }
    }


    /**
     * <h3>총점을 계산합니다.</h3>
     * <p>
     * 촌수에 따른 기본 점수, 공통 점수, 상호작용 점수를 합산합니다.
     * </p>
     *
     * <h4>점수 구성:</h4>
     * <ul>
     *   <li><b>기본 점수:</b> 2촌=50, 3촌=20, 기타=0</li>
     *   <li><b>공통 점수:</b> commonScore (2촌 최대 20, 3촌 최대 5 - 증가 시점에 제한)</li>
     *   <li><b>상호작용 점수:</b> min(interactionScore, 10)</li>
     * </ul>
     *
     * @return 총점
     */
    public double calculateTotalScore() {
        double base = (depth == 2) ? 50 : (depth == 3) ? 20 : 0;
        double interaction = Math.min(interactionScore, 10.0);

        return base + commonScore + interaction;
    }
}