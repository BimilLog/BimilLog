package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.RecommendedFriend;
import jaeik.bimillog.domain.friend.entity.jpa.FriendRecommendation;
import jaeik.bimillog.domain.friend.repository.FriendRecommendationRepository;
import jaeik.bimillog.domain.friend.repository.FriendshipRepository;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.out.MemberRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>친구 추천 스케줄러 성능 테스트 (로컬 MySQL)</h2>
 * <p>1000명의 회원과 친구 관계 데이터를 사용하여 스케줄러의 성능을 측정합니다.</p>
 *
 * <h3>테스트 시나리오:</h3>
 * <ul>
 *   <li>1000명의 회원 (각 회원당 평균 15명의 친구)</li>
 *   <li>상호작용 데이터 (게시글, 댓글, 좋아요)</li>
 *   <li>스케줄러 실행 후 성능 측정</li>
 * </ul>
 *
 * <h3>측정 항목:</h3>
 * <ul>
 *   <li>자바 애플리케이션 실행 시간 (StopWatch)</li>
 *   <li>DB 쿼리 수와 시간 (Hibernate Statistics)</li>
 *   <li>저장된 추천 친구 수</li>
 *   <li>회원당 평균 처리 시간</li>
 * </ul>
 *
 * <h3>사전 준비:</h3>
 * <pre>
 * mysql -u root -p bimillogTest < src/test/resources/friend_recommendation_performance_seed.sql
 * </pre>
 *
 * <h3>실행 방법:</h3>
 * <pre>
 * ./gradlew test --tests "*.FriendRecommendScheduledServicePerformanceTest.shouldMeasurePerformanceFor1000Members"
 * </pre>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.scheduling.enabled=false"
})
@DisplayName("친구 추천 스케줄러 성능 테스트 (로컬 MySQL)")
@Tag("local-integration")
@ActiveProfiles("local-integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FriendRecommendScheduledServicePerformanceTest extends BaseIntegrationTest {

    @Autowired
    private FriendRecommendScheduledService scheduledService;

    @Autowired
    private FriendRecommendService friendRecommendService;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private FriendRecommendationRepository friendRecommendationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics hibernateStatistics;

    /**
     * 각 테스트 전 Hibernate Statistics 활성화
     */
    @BeforeEach
    void setUpStatistics() {
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class);
        hibernateStatistics = sessionFactory.getStatistics();
        hibernateStatistics.setStatisticsEnabled(true);
        hibernateStatistics.clear();
    }

    /**
     * 시드 데이터 확인 테스트
     * <p>
     * friend_recommendation_performance_seed.sql이 정상적으로 로드되었는지 확인합니다.
     * </p>
     */
    @Test
    @Order(1)
    @DisplayName("시드 데이터가 정상적으로 로드되었는지 확인")
    void shouldHaveSeedDataLoaded() {
        // when
        long memberCount = memberRepository.count();
        long friendshipCount = friendshipRepository.count();

        // then
        System.out.println("\n========== 시드 데이터 확인 ==========");
        System.out.println("회원 수: " + memberCount + "명");
        System.out.println("친구 관계 수: " + friendshipCount + "개 (양방향 포함)");
        System.out.println("====================================\n");

        assertThat(memberCount).isGreaterThanOrEqualTo(1000);
        assertThat(friendshipCount).isGreaterThanOrEqualTo(5000);
    }

    /**
     * 1000명의 회원에 대한 친구 추천 스케줄러 성능 측정
     * <p>
     * - 자바 애플리케이션 실행 시간 측정 (StopWatch)
     * - DB 쿼리 수와 시간 측정 (Hibernate Statistics)
     * - 저장된 추천 친구 수 검증
     * </p>
     */
    @Test
    @Order(2)
    @DisplayName("1000명의 회원에 대한 친구 추천 성능 측정")
    void shouldMeasurePerformanceFor1000Members() {
        // given
        long memberCount = memberRepository.count();
        long initialRecommendationCount = friendRecommendationRepository.count();

        System.out.println("\n========== 친구 추천 스케줄러 성능 측정 시작 ==========");
        System.out.println("총 회원 수: " + memberCount + "명");
        System.out.println("초기 추천 친구 수: " + initialRecommendationCount + "개");
        System.out.println("======================================================\n");

        // when - StopWatch로 시간 측정
        StopWatch stopWatch = new StopWatch("FriendRecommendScheduler");
        stopWatch.start("스케줄러 실행");

        scheduledService.friendRecommendUpdate();

        stopWatch.stop();

        // DB 반영 및 캐시 클리어
        entityManagerDelegate.flush();
        entityManagerDelegate.clear();

        // then - 결과 조회
        long finalRecommendationCount = friendRecommendationRepository.count();
        List<FriendRecommendation> allRecommendations = friendRecommendationRepository.findAll();

        // 회원별 추천 수 그룹핑
        Map<Long, Long> recommendationsPerMember = allRecommendations.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getMember().getId(),
                        Collectors.counting()
                ));

        // Hibernate Statistics 수집
        long queryCount = hibernateStatistics.getQueryExecutionCount();
        long queryTime = hibernateStatistics.getQueryExecutionMaxTime();
        long entityLoadCount = hibernateStatistics.getEntityLoadCount();
        long entityInsertCount = hibernateStatistics.getEntityInsertCount();
        long entityUpdateCount = hibernateStatistics.getEntityUpdateCount();
        long entityDeleteCount = hibernateStatistics.getEntityDeleteCount();

        // 결과 출력
        System.out.println("\n========== 친구 추천 스케줄러 성능 측정 결과 ==========");
        System.out.println("=== 실행 시간 ===");
        System.out.printf("총 실행 시간: %.3f초%n", stopWatch.getTotalTimeSeconds());
        System.out.printf("회원당 평균 처리 시간: %.2f밀리초%n",
                (stopWatch.getTotalTimeMillis() / (double) memberCount));

        System.out.println("\n=== DB 쿼리 통계 (Hibernate Statistics) ===");
        System.out.println("총 쿼리 수: " + queryCount + "개");
        System.out.println("쿼리 최대 실행 시간: " + queryTime + "ms");
        System.out.println("엔티티 로드 횟수: " + entityLoadCount + "개");
        System.out.println("엔티티 삽입 횟수: " + entityInsertCount + "개");
        System.out.println("엔티티 수정 횟수: " + entityUpdateCount + "개");
        System.out.println("엔티티 삭제 횟수: " + entityDeleteCount + "개");

        System.out.println("\n=== 추천 친구 생성 결과 ===");
        System.out.println("저장된 추천 친구 수: " + finalRecommendationCount + "개");
        System.out.println("추천을 받은 회원 수: " + recommendationsPerMember.size() + "명");
        System.out.printf("회원당 평균 추천 수: %.2f개%n",
                (finalRecommendationCount / (double) recommendationsPerMember.size()));

        // 회원별 추천 수 분포
        long membersWithMax10 = recommendationsPerMember.values().stream()
                .filter(count -> count <= 10)
                .count();
        long membersWithLessThan10 = recommendationsPerMember.values().stream()
                .filter(count -> count < 10)
                .count();

        System.out.println("10개 이하 추천 받은 회원: " + membersWithMax10 + "명");
        System.out.println("10개 미만 추천 받은 회원: " + membersWithLessThan10 + "명");
        System.out.println("======================================================\n");

        // 검증
        assertThat(finalRecommendationCount).isGreaterThan(0);
        assertThat(finalRecommendationCount).isLessThanOrEqualTo(memberCount * 10); // 최대 10개씩

        // 각 회원은 최대 10개의 추천만 받아야 함
        assertThat(recommendationsPerMember.values())
                .allMatch(count -> count <= 10);

        // 자기 자신을 추천받지 않는지 확인
        boolean hasSelfRecommendation = allRecommendations.stream()
                .anyMatch(r -> r.getMember().getId().equals(r.getRecommendMember().getId()));
        assertThat(hasSelfRecommendation).isFalse();
    }

    /**
     * 추천친구 조회 API 성능 측정
     * <p>
     * - 추천친구 데이터가 저장된 상태에서 조회 API 성능 측정
     * - 10명의 샘플 회원에 대해 페이징 조회 수행
     * - 자바 애플리케이션 실행 시간 측정 (StopWatch)
     * - DB 쿼리 수와 시간 측정 (Hibernate Statistics)
     * </p>
     */
    @Test
    @Order(3)
    @DisplayName("추천친구 조회 API 성능 측정 (10명 샘플)")
    void shouldMeasureRecommendFriendListQueryPerformance() {
        // given
        long totalMemberCount = memberRepository.count();
        long recommendationCount = friendRecommendationRepository.count();
        List<Member> sampleMembers = memberRepository.findAll().stream()
                .limit(10) // 10명만 샘플링
                .toList();

        System.out.println("\n========== 추천친구 조회 API 성능 측정 시작 ==========");
        System.out.println("전체 회원 수: " + totalMemberCount + "명");
        System.out.println("조회 대상 회원: " + sampleMembers.size() + "명 (샘플)");
        System.out.println("저장된 추천 친구 수: " + recommendationCount + "개");
        System.out.println("======================================================\n");

        // when - StopWatch로 시간 측정
        StopWatch stopWatch = new StopWatch("RecommendFriendListQuery");
        stopWatch.start("샘플 회원 조회");

        Pageable pageable = PageRequest.of(0, 10); // 페이지당 10개씩 조회
        int totalRetrievedCount = 0;

        // 샘플 멤버별로 조회 API 호출
        for (Member member : sampleMembers) {
            Page<RecommendedFriend> recommendedFriendPage =
                friendRecommendService.getRecommendFriendList(member.getId(), pageable);
            totalRetrievedCount += recommendedFriendPage.getContent().size();
        }

        stopWatch.stop();

        // Hibernate Statistics 수집c
        long queryCount = hibernateStatistics.getQueryExecutionCount();
        long queryTime = hibernateStatistics.getQueryExecutionMaxTime();
        long entityLoadCount = hibernateStatistics.getEntityLoadCount();

        // 결과 출력
        System.out.println("\n========== 추천친구 조회 API 성능 측정 결과 ==========");
        System.out.println("=== 실행 시간 ===");
        System.out.printf("총 실행 시간: %.3f초%n", stopWatch.getTotalTimeSeconds());
        System.out.printf("회원당 평균 조회 시간: %.2f밀리초%n",
                (stopWatch.getTotalTimeMillis() / (double) sampleMembers.size()));

        System.out.println("\n=== DB 쿼리 통계 (Hibernate Statistics) ===");
        System.out.println("총 쿼리 수: " + queryCount + "개");
        System.out.println("쿼리 최대 실행 시간: " + queryTime + "ms");
        System.out.println("엔티티 로드 횟수: " + entityLoadCount + "개");

        System.out.println("\n=== 조회 결과 ===");
        System.out.println("총 조회된 추천친구 수: " + totalRetrievedCount + "개");
        System.out.printf("회원당 평균 조회 수: %.2f개%n",
                (totalRetrievedCount / (double) sampleMembers.size()));
        System.out.println("======================================================\n");

        // 검증
        assertThat(totalRetrievedCount).isGreaterThan(0);
        assertThat((long) totalRetrievedCount).isLessThanOrEqualTo(sampleMembers.size() * 10); // 페이징 크기 10
    }
}
