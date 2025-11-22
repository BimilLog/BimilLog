package jaeik.bimillog.domain.friend.service;

import com.querydsl.core.Tuple;
import jaeik.bimillog.domain.friend.entity.jpa.FriendRecommendation;
import jaeik.bimillog.domain.friend.entity.jpa.Friendship;
import jaeik.bimillog.domain.friend.repository.FriendRecommendationRepository;
import jaeik.bimillog.domain.friend.repository.FriendshipQueryRepository;
import jaeik.bimillog.domain.friend.repository.FriendshipRepository;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.builder.FriendTestDataBuilder;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FriendRecommendScheduledService 통합 테스트 (H2 Database)
 * <p>
 * 실제 데이터베이스를 사용하여 친구 추천 스케줄러의 전체 플로우를 검증합니다.
 * 현재 구현이 미완성이므로 일부 테스트는 @Disabled 처리되어 있습니다.
 * </p>
 *
 * <h3>테스트 시나리오:</h3>
 * <ul>
 *   <li>2촌 관계 조회 (현재 구현됨)</li>
 *   <li>복잡한 친구 네트워크 처리 (미구현 - @Disabled)</li>
 *   <li>점수 기반 추천 생성 (미구현 - @Disabled)</li>
 *   <li>데이터 무결성 검증 (미구현 - @Disabled)</li>
 * </ul>
 *
 * <h3>테스트 데이터 구조:</h3>
 * <pre>
 * Member Network:
 *
 *   Member1 --- Member2 --- Member4 --- Member7
 *      |           |
 *   Member3     Member5 --- Member6
 *      |
 *   Member8
 *
 * 예상 2촌 관계:
 * - Member1의 2촌: Member4, Member5
 * - Member2의 2촌: Member1, Member3, Member6
 * - Member3의 2촌: Member2, Member8
 * </pre>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@DisplayName("FriendRecommendScheduledService 통합 테스트")
@Tag("integration")
@ActiveProfiles("h2test")
@Import(H2TestConfiguration.class)
class FriendRecommendScheduledServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FriendRecommendScheduledService scheduledService;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private FriendshipQueryRepository friendshipQueryRepository;

    @Autowired
    private FriendRecommendationRepository friendRecommendationRepository;

    // 테스트용 회원들
    private Member member1, member2, member3, member4, member5, member6, member7, member8;

    /**
     * 각 테스트 전 친구 네트워크 설정
     * <p>
     * Member1 - Member2 (1촌)
     * Member1 - Member3 (1촌)
     * Member2 - Member4 (1촌)
     * Member2 - Member5 (1촌)
     * Member3 - Member8 (1촌)
     * Member4 - Member7 (1촌)
     * Member5 - Member6 (1촌)
     * </p>
     */
    @BeforeEach
    void setUpFriendNetwork() {
        // 회원 생성 및 저장
        member1 = saveMember(TestMembers.createUniqueWithPrefix("member1"));
        member2 = saveMember(TestMembers.createUniqueWithPrefix("member2"));
        member3 = saveMember(TestMembers.createUniqueWithPrefix("member3"));
        member4 = saveMember(TestMembers.createUniqueWithPrefix("member4"));
        member5 = saveMember(TestMembers.createUniqueWithPrefix("member5"));
        member6 = saveMember(TestMembers.createUniqueWithPrefix("member6"));
        member7 = saveMember(TestMembers.createUniqueWithPrefix("member7"));
        member8 = saveMember(TestMembers.createUniqueWithPrefix("member8"));

        // 친구 관계 설정 (양방향)
        createBidirectionalFriendship(member1, member2);
        createBidirectionalFriendship(member1, member3);
        createBidirectionalFriendship(member2, member4);
        createBidirectionalFriendship(member2, member5);
        createBidirectionalFriendship(member3, member8);
        createBidirectionalFriendship(member4, member7);
        createBidirectionalFriendship(member5, member6);

        entityManagerDelegate.flush();
        entityManagerDelegate.clear();
    }

    /**
     * 양방향 친구 관계 생성 헬퍼 메서드
     * <p>
     * Member A와 Member B가 서로 친구가 되도록 Friendship 엔티티 1개만 생성합니다.
     * (비즈니스 로직: 1,2 존재 시 2,1 저장 안 됨)
     * </p>
     */
    private void createBidirectionalFriendship(Member memberA, Member memberB) {
        Friendship friendship = FriendTestDataBuilder.createFriendship(memberA, memberB);
        friendshipRepository.save(friendship);
    }

    // ==================== 현재 구현된 기능 테스트 ====================

    @Test
    @DisplayName("2촌 관계 조회가 실제 DB에서 정상 동작")
    void shouldFetchTwoDegreeRelations_FromRealDatabase() {
        // when
        List<Tuple> results = friendshipQueryRepository.findAllTwoDegreeRelations();

        // then
        assertThat(results).isNotNull();
        assertThat(results).isNotEmpty();

        // 결과 출력 (디버깅용)
        System.out.println("\n========== 2촌 관계 조회 결과 ==========");
        results.forEach(tuple -> {
            Long memberId = tuple.get(0, Long.class);
            Long firstDegreeId = tuple.get(1, Long.class);
            Long secondDegreeId = tuple.get(2, Long.class);
            System.out.printf("Member %d -> %d (1촌) -> %d (2촌)%n",
                    memberId, firstDegreeId, secondDegreeId);
        });
        System.out.println("총 2촌 관계 수: " + results.size());
        System.out.println("=====================================\n");
    }

    @Test
    @DisplayName("Member1의 2촌 관계가 올바르게 조회됨")
    void shouldFindCorrect2ndDegreeConnections_ForMember1() {
        // when
        List<Tuple> allResults = friendshipQueryRepository.findAllTwoDegreeRelations();

        // Member1의 2촌만 필터링
        List<Tuple> member1Results = allResults.stream()
                .filter(tuple -> tuple.get(0, Long.class).equals(member1.getId()))
                .toList();

        // Member1의 2촌 ID 추출
        List<Long> secondDegreeIds = member1Results.stream()
                .map(tuple -> tuple.get(2, Long.class))
                .distinct()
                .toList();

        // then
        System.out.println("\n========== Member1의 2촌 관계 ==========");
        System.out.println("Member1 ID: " + member1.getId());
        System.out.println("2촌 IDs: " + secondDegreeIds);
        System.out.println("=====================================\n");

        // Member1의 예상 2촌: Member4 (via Member2), Member5 (via Member2), Member8 (via Member3)
        assertThat(secondDegreeIds).contains(member4.getId(), member5.getId(), member8.getId());
    }

    @Test
    @DisplayName("스케줄러 실행 시 예외 없이 정상 종료")
    void shouldCompleteWithoutException_WhenSchedulerRuns() {
        // when & then
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            scheduledService.friendRecommendUpdate();
        });
    }

    @Test
    @DisplayName("친구 관계가 없는 경우에도 스케줄러가 정상 동작")
    void shouldHandleEmptyFriendships_Gracefully() {
        // given
        friendshipRepository.deleteAll();
        entityManagerDelegate.flush();

        // when & then
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            scheduledService.friendRecommendUpdate();
        });
    }

    // ==================== 2촌 계산 및 저장 테스트 (미구현) ====================

    @Test
    @Disabled("미구현: 2촌 추천 생성 및 저장 로직 필요")
    @DisplayName("2촌 관계에서 추천 친구 생성 및 저장")
    void shouldCreateAndSaveRecommendations_From2ndDegree() {
        // when
        scheduledService.friendRecommendUpdate();
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        // then
        List<FriendRecommendation> recommendations = friendRecommendationRepository.findAll();
        assertThat(recommendations).isNotEmpty();

        // Member1에 대한 추천 확인
        List<FriendRecommendation> member1Recommendations = recommendations.stream()
                .filter(r -> r.getMember().getId().equals(member1.getId()))
                .toList();

        assertThat(member1Recommendations).isNotEmpty();
        assertThat(member1Recommendations).hasSizeLessThanOrEqualTo(10);

        // 2촌인 Member4, Member5, Member8이 추천에 포함되는지 확인
        List<Long> recommendedIds = member1Recommendations.stream()
                .map(r -> r.getRecommendMember().getId())
                .toList();

        assertThat(recommendedIds).contains(member4.getId(), member5.getId(), member8.getId());
    }

    @Test
    @Disabled("미구현: 점수 계산 로직 필요")
    @DisplayName("추천 친구에 올바른 점수 부여 (2촌 기본 50점)")
    void shouldAssignCorrectScores_To2ndDegreeRecommendations() {
        // when
        scheduledService.friendRecommendUpdate();
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        // then
        List<FriendRecommendation> recommendations = friendRecommendationRepository.findAll();

        // 2촌 추천의 점수가 최소 50점 이상인지 확인
        List<FriendRecommendation> secondDegreeRecs = recommendations.stream()
                .filter(r -> r.getDepth() == 2)
                .toList();

        assertThat(secondDegreeRecs).allMatch(r -> r.getScore() >= 50);
    }

    @Test
    @Disabled("미구현: acquaintanceId 설정 로직 필요")
    @DisplayName("2촌 추천에 공통 친구 ID 설정")
    void shouldSetAcquaintanceId_For2ndDegreeRecommendations() {
        // when
        scheduledService.friendRecommendUpdate();
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        // then
        List<FriendRecommendation> recommendations = friendRecommendationRepository.findAll();

        // 2촌 추천에 acquaintanceId가 설정되어 있는지 확인
        List<FriendRecommendation> secondDegreeRecs = recommendations.stream()
                .filter(r -> r.getDepth() == 2)
                .toList();

        assertThat(secondDegreeRecs).allMatch(r -> r.getAcquaintanceId() != null);
    }

    // ==================== 3촌 계산 테스트 (미구현) ====================

    @Test
    @Disabled("미구현: 3촌 계산 로직 필요")
    @DisplayName("2촌이 10명 미만일 때 3촌까지 계산")
    void shouldCalculate3rdDegree_When2ndDegreeIsLessThan10() {
        // when
        scheduledService.friendRecommendUpdate();
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        // then
        List<FriendRecommendation> recommendations = friendRecommendationRepository.findAll();

        // 3촌 추천이 존재하는지 확인
        boolean has3rdDegree = recommendations.stream()
                .anyMatch(r -> r.getDepth() == 3);

        assertThat(has3rdDegree).isTrue();
    }

    @Test
    @Disabled("미구현: 3촌 점수 계산 로직 필요")
    @DisplayName("3촌 추천에 기본 20점 부여")
    void shouldAssignBase20Points_To3rdDegreeRecommendations() {
        // when
        scheduledService.friendRecommendUpdate();
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        // then
        List<FriendRecommendation> recommendations = friendRecommendationRepository.findAll();

        // 3촌 추천의 점수가 20점 이상인지 확인
        List<FriendRecommendation> thirdDegreeRecs = recommendations.stream()
                .filter(r -> r.getDepth() == 3)
                .toList();

        assertThat(thirdDegreeRecs).allMatch(r -> r.getScore() >= 20);
        assertThat(thirdDegreeRecs).allMatch(r -> r.getScore() <= 35); // 최대 35점
    }

    @Test
    @Disabled("미구현: 3촌 acquaintanceId null 설정 로직 필요")
    @DisplayName("3촌 추천의 acquaintanceId는 null")
    void shouldSetAcquaintanceIdToNull_For3rdDegreeRecommendations() {
        // when
        scheduledService.friendRecommendUpdate();
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        // then
        List<FriendRecommendation> recommendations = friendRecommendationRepository.findAll();

        // 3촌 추천의 acquaintanceId가 null인지 확인
        List<FriendRecommendation> thirdDegreeRecs = recommendations.stream()
                .filter(r -> r.getDepth() == 3)
                .toList();

        assertThat(thirdDegreeRecs).allMatch(r -> r.getAcquaintanceId() == null);
    }

    // ==================== 공통 친구 점수 테스트 (미구현) ====================

    @Test
    @Disabled("미구현: 공통 친구 보너스 계산 로직 필요")
    @DisplayName("공통 친구가 여러 명일 경우 보너스 점수 부여")
    void shouldAddCommonFriendBonus_WhenMultipleCommonFriendsExist() {
        // given
        // Member1과 Member4가 공통 친구 2명 (Member2, Member5)을 가지도록 설정
        createBidirectionalFriendship(member1, member5);
        createBidirectionalFriendship(member4, member2);
        entityManagerDelegate.flush();

        // when
        scheduledService.friendRecommendUpdate();
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        // then
        List<FriendRecommendation> member1Recommendations = friendRecommendationRepository.findAll()
                .stream()
                .filter(r -> r.getMember().getId().equals(member1.getId()))
                .filter(r -> r.getRecommendMember().getId().equals(member4.getId()))
                .toList();

        assertThat(member1Recommendations).hasSize(1);

        FriendRecommendation recommendation = member1Recommendations.get(0);
        // 기본 50점 + 공통 친구 2명 * 2점 = 54점 이상
        assertThat(recommendation.getScore()).isGreaterThanOrEqualTo(54);
    }

    @Test
    @Disabled("미구현: manyAcquaintance 플래그 설정 로직 필요")
    @DisplayName("공통 친구 2명 이상이면 manyAcquaintance 플래그 true")
    void shouldSetManyAcquaintanceFlag_WhenTwoOrMoreCommonFriends() {
        // given
        createBidirectionalFriendship(member1, member5);
        createBidirectionalFriendship(member4, member2);
        entityManagerDelegate.flush();

        // when
        scheduledService.friendRecommendUpdate();
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        // then
        List<FriendRecommendation> recommendations = friendRecommendationRepository.findAll()
                .stream()
                .filter(r -> r.getMember().getId().equals(member1.getId()))
                .filter(r -> r.getRecommendMember().getId().equals(member4.getId()))
                .toList();

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).isManyAcquaintance()).isTrue();
    }

    // ==================== 상위 10명 선정 테스트 (미구현) ====================

    @Test
    @Disabled("미구현: Top 10 선정 로직 필요")
    @DisplayName("각 회원당 최대 10명의 추천만 저장")
    void shouldSaveMaximum10Recommendations_PerMember() {
        // when
        scheduledService.friendRecommendUpdate();
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        // then
        List<FriendRecommendation> allRecommendations = friendRecommendationRepository.findAll();

        // 회원별 추천 수를 그룹핑
        Map<Long, Long> recommendationsPerMember = allRecommendations.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getMember().getId(),
                        Collectors.counting()
                ));

        // 각 회원당 최대 10개까지만 저장되었는지 확인
        assertThat(recommendationsPerMember.values())
                .allMatch(count -> count <= 10);
    }

    @Test
    @Disabled("미구현: 점수 순 정렬 로직 필요")
    @DisplayName("추천 친구가 점수 내림차순으로 저장")
    void shouldSaveRecommendations_InDescendingOrderByScore() {
        // when
        scheduledService.friendRecommendUpdate();
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        // then
        List<FriendRecommendation> member1Recommendations = friendRecommendationRepository.findAll()
                .stream()
                .filter(r -> r.getMember().getId().equals(member1.getId()))
                .toList();

        // 점수가 내림차순으로 정렬되어 있는지 확인
        List<Integer> scores = member1Recommendations.stream()
                .map(FriendRecommendation::getScore)
                .toList();

        List<Integer> sortedScores = scores.stream()
                .sorted((a, b) -> b - a)
                .toList();

        assertThat(scores).isEqualTo(sortedScores);
    }

    // ==================== 복잡한 네트워크 테스트 (미구현) ====================

    @Test
    @Disabled("미구현: 전체 플로우 구현 필요")
    @DisplayName("복잡한 친구 네트워크에서 정확한 추천 생성")
    void shouldHandleComplexFriendNetwork_Correctly() {
        // when
        scheduledService.friendRecommendUpdate();
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        // then
        List<FriendRecommendation> allRecommendations = friendRecommendationRepository.findAll();
        assertThat(allRecommendations).isNotEmpty();

        // 각 회원이 자기 자신을 추천받지 않는지 확인
        boolean hasSelfRecommendation = allRecommendations.stream()
                .anyMatch(r -> r.getMember().getId().equals(r.getRecommendMember().getId()));

        assertThat(hasSelfRecommendation).isFalse();

        // 각 회원이 이미 친구인 사람을 추천받지 않는지 확인
        // (복잡한 검증 로직 필요)
    }

    @Test
    @Disabled("미구현: 데이터 무결성 검증 로직 필요")
    @DisplayName("스케줄러 여러 번 실행 시 데이터 무결성 유지")
    void shouldMaintainDataIntegrity_AfterMultipleRuns() {
        // when
        scheduledService.friendRecommendUpdate();
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        long countAfterFirstRun = friendRecommendationRepository.count();

        scheduledService.friendRecommendUpdate();
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        long countAfterSecondRun = friendRecommendationRepository.count();

        // then
        // 두 번째 실행 시 기존 추천을 삭제하고 새로 생성하므로 개수가 동일해야 함
        assertThat(countAfterSecondRun).isEqualTo(countAfterFirstRun);
    }

    // ==================== 엣지 케이스 테스트 (미구현) ====================

    @Test
    @Disabled("미구현: 고립된 회원 처리 로직 필요")
    @DisplayName("친구가 없는 회원도 랜덤 추천 받음")
    void shouldProvideRandomRecommendations_ForIsolatedMembers() {
        // given
        Member isolatedMember = saveMember(TestMembers.createUniqueWithPrefix("isolated"));
        entityManagerDelegate.flush();

        // when
        scheduledService.friendRecommendUpdate();
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        // then
        List<FriendRecommendation> isolatedRecommendations = friendRecommendationRepository.findAll()
                .stream()
                .filter(r -> r.getMember().getId().equals(isolatedMember.getId()))
                .toList();

        // 랜덤으로라도 추천을 받아야 함
        assertThat(isolatedRecommendations).isNotEmpty();
    }

    @Test
    @Disabled("미구현: 전체 플로우 구현 필요")
    @DisplayName("순환 친구 관계에서도 올바른 추천 생성")
    void shouldHandleCircularFriendships_Correctly() {
        // given
        // A -> B -> C -> A 순환 관계 추가
        createBidirectionalFriendship(member4, member7);
        createBidirectionalFriendship(member7, member1);
        entityManagerDelegate.flush();

        // when
        scheduledService.friendRecommendUpdate();
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        // then
        List<FriendRecommendation> allRecommendations = friendRecommendationRepository.findAll();

        // 중복 추천이 없는지 확인
        long distinctCount = allRecommendations.stream()
                .map(r -> r.getMember().getId() + "-" + r.getRecommendMember().getId())
                .distinct()
                .count();

        assertThat(distinctCount).isEqualTo(allRecommendations.size());
    }
}
