package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.RecommendedFriend;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.MemberBlacklist;
import jaeik.bimillog.domain.member.out.MemberBlacklistRepository;
import jaeik.bimillog.domain.member.out.MemberQueryAdapter;
import jaeik.bimillog.domain.member.out.MemberRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
import jaeik.bimillog.testutil.RedisTestHelper;
import jaeik.bimillog.testutil.TestMembers;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>FriendRecommendService 통합 테스트</h2>
 * <p>친구 추천 서비스의 전체 워크플로우를 로컬 MySQL + Redis 환경에서 검증합니다.</p>
 * <p>Redis 캐싱, BFS 알고리즘, 블랙리스트 필터링 등 핵심 기능을 통합 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Tag("local-integration")
@DisplayName("FriendRecommendService 통합 테스트")
class FriendRecommendServiceIntegrationTest {

    @Autowired
    private FriendRecommendService friendRecommendService;

    @Autowired
    private RedisFriendshipRepository redisFriendshipRepository;

    @Autowired
    private RedisInteractionScoreRepository redisInteractionScoreRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberBlacklistRepository memberBlacklistRepository;

    @Autowired
    private MemberQueryAdapter memberQueryAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private Member member1; // 본인
    private Member member2; // 1촌
    private Member member3; // 1촌
    private Member member4; // 2촌 (member2의 친구)
    private Member member5; // 2촌 (member3의 친구)
    private Member member6; // 3촌 (member4의 친구)
    private Member member7; // 블랙리스트에 추가될 2촌

    @BeforeEach
    void setUp() {
        // Redis 초기화
        RedisTestHelper.flushRedis(redisTemplate);

        // 테스트 회원 생성
        member1 = TestMembers.createUniqueWithPrefix("testUser1");
        member2 = TestMembers.createUniqueWithPrefix("testUser2");
        member3 = TestMembers.createUniqueWithPrefix("testUser3");
        member4 = TestMembers.createUniqueWithPrefix("testUser4");
        member5 = TestMembers.createUniqueWithPrefix("testUser5");
        member6 = TestMembers.createUniqueWithPrefix("testUser6");
        member7 = TestMembers.createUniqueWithPrefix("testUser7");

        persistAndFlush(member1);
        persistAndFlush(member2);
        persistAndFlush(member3);
        persistAndFlush(member4);
        persistAndFlush(member5);
        persistAndFlush(member6);
        persistAndFlush(member7);

        // Redis에 친구 관계 세팅
        setupFriendships();

        // 상호작용 점수 세팅
        setupInteractionScores();
    }

    private void persistAndFlush(Object entity) {
        entityManager.persist(entity);
        entityManager.flush();
    }

    private void setupFriendships() {
        // member1의 1촌: member2, member3
        redisFriendshipRepository.addFriend(member1.getId(), member2.getId());
        redisFriendshipRepository.addFriend(member1.getId(), member3.getId());

        // member2의 친구: member1, member4 (member4는 member1의 2촌)
        redisFriendshipRepository.addFriend(member2.getId(), member4.getId());

        // member3의 친구: member1, member5 (member5는 member1의 2촌)
        redisFriendshipRepository.addFriend(member3.getId(), member5.getId());

        // member4의 친구: member2, member6 (member6은 member1의 3촌)
        redisFriendshipRepository.addFriend(member4.getId(), member6.getId());

        // member3의 친구에 member7 추가 (member7은 member1의 2촌, 블랙리스트 테스트용)
        redisFriendshipRepository.addFriend(member3.getId(), member7.getId());
    }

    private void setupInteractionScores() {
        // member1과 member4 간 상호작용 점수 (2촌)
        redisInteractionScoreRepository.addInteractionScore(member1.getId(), member4.getId());
        redisInteractionScoreRepository.addInteractionScore(member1.getId(), member4.getId());

        // member1과 member5 간 상호작용 점수 (2촌)
        redisInteractionScoreRepository.addInteractionScore(member1.getId(), member5.getId());
    }

    @Test
    @DisplayName("2촌 친구 추천 - Redis 캐싱 기반 BFS 탐색")
    void shouldRecommendSecondDegreeFriends() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<RecommendedFriend> result = friendRecommendService.getRecommendFriendList(member1.getId(), pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();

        // 2촌이 추천되어야 함 (member4, member5, member7)
        List<Long> recommendedIds = result.getContent().stream()
                .map(RecommendedFriend::getFriendMemberId)
                .toList();

        assertThat(recommendedIds)
                .contains(member4.getId(), member5.getId(), member7.getId())
                .doesNotContain(member1.getId(), member2.getId(), member3.getId()); // 본인 및 1촌 제외

        // member4가 더 높은 점수를 받아야 함 (상호작용 점수 2배)
        RecommendedFriend member4Recommendation = result.getContent().stream()
                .filter(f -> f.getFriendMemberId().equals(member4.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(member4Recommendation.getDepth()).isEqualTo(2); // 2촌
    }

    @Test
    @DisplayName("3촌 친구 추천 - 2촌이 부족할 때만 3촌 탐색")
    void shouldRecommendThirdDegreeFriendsWhenSecondDegreeIsInsufficient() {
        // Given
        // 2촌이 3명뿐이므로 3촌도 탐색되어야 함
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<RecommendedFriend> result = friendRecommendService.getRecommendFriendList(member1.getId(), pageable);

        // Then
        List<Long> recommendedIds = result.getContent().stream()
                .map(RecommendedFriend::getFriendMemberId)
                .toList();

        // 3촌 (member6)이 포함되어야 함
        assertThat(recommendedIds).contains(member6.getId());

        // member6은 3촌이어야 함
        RecommendedFriend member6Recommendation = result.getContent().stream()
                .filter(f -> f.getFriendMemberId().equals(member6.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(member6Recommendation.getDepth()).isEqualTo(3); // 3촌
    }

    @Test
    @DisplayName("블랙리스트 제외 - 차단한 사용자는 추천되지 않음")
    void shouldExcludeBlacklistedUsers() {
        // Given
        // member1이 member7을 블랙리스트에 추가
        MemberBlacklist blacklist = MemberBlacklist.builder()
                .requestMember(member1)
                .blackMember(member7)
                .build();
        memberBlacklistRepository.save(blacklist);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<RecommendedFriend> result = friendRecommendService.getRecommendFriendList(member1.getId(), pageable);

        // Then
        List<Long> recommendedIds = result.getContent().stream()
                .map(RecommendedFriend::getFriendMemberId)
                .toList();

        // member7은 블랙리스트이므로 추천되지 않아야 함
        assertThat(recommendedIds).doesNotContain(member7.getId());

        // 다른 2촌은 여전히 추천되어야 함
        assertThat(recommendedIds).contains(member4.getId(), member5.getId());
    }

    @Test
    @DisplayName("공통 친구 정보 - 2촌 추천 시 공통 친구 표시")
    void shouldShowMutualFriendsForSecondDegree() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<RecommendedFriend> result = friendRecommendService.getRecommendFriendList(member1.getId(), pageable);

        // Then
        // member4의 추천 정보 확인 (member2를 통해 연결됨)
        RecommendedFriend member4Recommendation = result.getContent().stream()
                .filter(f -> f.getFriendMemberId().equals(member4.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(member4Recommendation.getAcquaintanceId()).isNotNull();
        assertThat(member4Recommendation.getAcquaintanceId()).isEqualTo(member2.getId());
    }

    @Test
    @DisplayName("상호작용 점수 반영 - 높은 점수의 후보가 우선 추천")
    void shouldPrioritizeHigherInteractionScores() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<RecommendedFriend> result = friendRecommendService.getRecommendFriendList(member1.getId(), pageable);

        // Then
        List<Long> recommendedIds = result.getContent().stream()
                .map(RecommendedFriend::getFriendMemberId)
                .toList();

        // member4가 member5보다 앞에 있어야 함 (상호작용 점수가 2배)
        int member4Index = recommendedIds.indexOf(member4.getId());
        int member5Index = recommendedIds.indexOf(member5.getId());

        assertThat(member4Index).isLessThan(member5Index);
    }

    @Test
    @DisplayName("친구가 없는 경우 - 최근 가입자로 추천 채우기")
    void shouldFillWithRecentMembersWhenNoFriends() {
        // Given
        Member lonelyMember = TestMembers.createUniqueWithPrefix("lonelyUser");
        persistAndFlush(lonelyMember);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<RecommendedFriend> result = friendRecommendService.getRecommendFriendList(lonelyMember.getId(), pageable);

        // Then
        assertThat(result.getContent()).isNotEmpty();

        // 최근 가입자가 추천되어야 함 (depth = 0)
        RecommendedFriend recentRecommendation = result.getContent().get(0);
        assertThat(recentRecommendation.getDepth()).isEqualTo(0);
    }

    @Test
    @DisplayName("Redis 캐시 동작 검증 - getFriends 호출 시 데이터 반환")
    void shouldReturnFriendsFromRedisCache() {
        // When
        var friends = redisFriendshipRepository.getFriends(member1.getId());

        // Then
        assertThat(friends).contains(member2.getId(), member3.getId());
        assertThat(friends).hasSize(2);
    }

    @Test
    @DisplayName("상호작용 점수 조회 검증 - 배치 조회 동작")
    void shouldReturnInteractionScoresInBatch() {
        // Given
        var targetIds = java.util.Set.of(member4.getId(), member5.getId());

        // When
        var scores = redisInteractionScoreRepository.getInteractionScoresBatch(member1.getId(), targetIds);

        // Then
        assertThat(scores).containsKeys(member4.getId(), member5.getId());
        assertThat(scores.get(member4.getId())).isGreaterThan(scores.get(member5.getId()));
    }
}
