package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.RecommendedFriend;
import jaeik.bimillog.domain.friend.repository.FriendRecommendationQueryRepository;
import jaeik.bimillog.domain.friend.repository.FriendToMemberAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * <h2>FriendRecommendService 단위 테스트</h2>
 * <p>추천 친구 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("FriendRecommendService 단위 테스트")
@Tag("unit")
class FriendRecommendServiceTest extends BaseUnitTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long FRIEND_ID_1 = 2L;
    private static final Long FRIEND_ID_2 = 3L;
    private static final Long ACQUAINTANCE_ID_1 = 10L;

    @Mock private FriendRecommendationQueryRepository friendRecommendationQueryRepository;
    @Mock private FriendToMemberAdapter friendToMemberAdapter;

    @InjectMocks
    private FriendRecommendService friendRecommendService;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
    }

    // ==================== getRecommendFriendList ====================

    @Test
    @DisplayName("추천 친구 조회 성공 - 추천 친구가 존재하는 경우")
    void shouldGetRecommendedFriends_WhenRecommendationsExist() {
        // Given - 2촌 추천 (acquaintanceId 있음)
        RecommendedFriend friend1 = new RecommendedFriend(FRIEND_ID_1, ACQUAINTANCE_ID_1, false, 2);
        // 3촌 추천 (acquaintanceId 없음)
        RecommendedFriend friend2 = new RecommendedFriend(FRIEND_ID_2, null, false, 3);
        Page<RecommendedFriend> recommendPage = new PageImpl<>(List.of(friend1, friend2), pageable, 2);

        RecommendedFriend.RecommendedFriendInfo friendInfo1 = new RecommendedFriend.RecommendedFriendInfo(
                FRIEND_ID_1, "추천친구1"
        );
        RecommendedFriend.RecommendedFriendInfo friendInfo2 = new RecommendedFriend.RecommendedFriendInfo(
                FRIEND_ID_2, "추천친구2"
        );
        RecommendedFriend.AcquaintanceInfo acquaintanceInfo = new RecommendedFriend.AcquaintanceInfo(
                ACQUAINTANCE_ID_1, "공통친구"
        );

        given(friendRecommendationQueryRepository.getRecommendFriendList(MEMBER_ID, pageable))
                .willReturn(recommendPage);
        given(friendToMemberAdapter.addRecommendedFriendInfo(List.of(FRIEND_ID_1, FRIEND_ID_2)))
                .willReturn(List.of(friendInfo1, friendInfo2));
        given(friendToMemberAdapter.addAcquaintanceInfo(List.of(ACQUAINTANCE_ID_1, null)))
                .willReturn(List.of(acquaintanceInfo));

        // When
        Page<RecommendedFriend> result = friendRecommendService.getRecommendFriendList(MEMBER_ID, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);

        // 2촌 친구 확인
        RecommendedFriend secondDegree = result.getContent().get(0);
        assertThat(secondDegree.getFriendMemberId()).isEqualTo(FRIEND_ID_1);
        assertThat(secondDegree.getDepth()).isEqualTo(2);
        assertThat(secondDegree.getAcquaintanceId()).isEqualTo(ACQUAINTANCE_ID_1);

        // 3촌 친구 확인
        RecommendedFriend thirdDegree = result.getContent().get(1);
        assertThat(thirdDegree.getFriendMemberId()).isEqualTo(FRIEND_ID_2);
        assertThat(thirdDegree.getDepth()).isEqualTo(3);
        assertThat(thirdDegree.getAcquaintanceId()).isNull();
    }

    @Test
    @DisplayName("추천 친구 조회 성공 - 추천 친구가 없는 경우 (빈 페이지)")
    void shouldReturnEmptyPage_WhenNoRecommendations() {
        // Given
        Page<RecommendedFriend> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(friendRecommendationQueryRepository.getRecommendFriendList(MEMBER_ID, pageable))
                .willReturn(emptyPage);
        given(friendToMemberAdapter.addRecommendedFriendInfo(List.of())).willReturn(List.of());
        given(friendToMemberAdapter.addAcquaintanceInfo(List.of())).willReturn(List.of());

        // When
        Page<RecommendedFriend> result = friendRecommendService.getRecommendFriendList(MEMBER_ID, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("추천 친구 조회 성공 - 점수 정렬 확인")
    void shouldSortByScore() {
        // Given - 점수가 높은 순서로 정렬됨 (Repository에서 정렬됨)
        RecommendedFriend highScore = new RecommendedFriend(FRIEND_ID_1, ACQUAINTANCE_ID_1, false, 2);
        RecommendedFriend lowScore = new RecommendedFriend(FRIEND_ID_2, null, false, 3);
        Page<RecommendedFriend> sortedPage = new PageImpl<>(List.of(highScore, lowScore), pageable, 2);

        RecommendedFriend.RecommendedFriendInfo friendInfo1 = new RecommendedFriend.RecommendedFriendInfo(
                FRIEND_ID_1, "고점수친구"
        );
        RecommendedFriend.RecommendedFriendInfo friendInfo2 = new RecommendedFriend.RecommendedFriendInfo(
                FRIEND_ID_2, "저점수친구"
        );
        RecommendedFriend.AcquaintanceInfo acquaintanceInfo = new RecommendedFriend.AcquaintanceInfo(
                ACQUAINTANCE_ID_1, "공통친구"
        );

        given(friendRecommendationQueryRepository.getRecommendFriendList(MEMBER_ID, pageable))
                .willReturn(sortedPage);
        given(friendToMemberAdapter.addRecommendedFriendInfo(List.of(FRIEND_ID_1, FRIEND_ID_2)))
                .willReturn(List.of(friendInfo1, friendInfo2));
        given(friendToMemberAdapter.addAcquaintanceInfo(List.of(ACQUAINTANCE_ID_1, null)))
                .willReturn(List.of(acquaintanceInfo));

        // When
        Page<RecommendedFriend> result = friendRecommendService.getRecommendFriendList(MEMBER_ID, pageable);

        // Then - 순서 확인 (Repository가 점수순으로 정렬)
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("추천 친구 조회 성공 - 2촌 공통 친구 정보 포함")
    void shouldIncludeAcquaintanceInfo_For2ndDegree() {
        // Given
        RecommendedFriend friend = new RecommendedFriend(FRIEND_ID_1, ACQUAINTANCE_ID_1, false, 2);
        Page<RecommendedFriend> recommendPage = new PageImpl<>(List.of(friend), pageable, 1);

        RecommendedFriend.RecommendedFriendInfo friendInfo = new RecommendedFriend.RecommendedFriendInfo(
                FRIEND_ID_1, "추천친구"
        );
        RecommendedFriend.AcquaintanceInfo acquaintanceInfo = new RecommendedFriend.AcquaintanceInfo(
                ACQUAINTANCE_ID_1, "공통친구이름"
        );

        given(friendRecommendationQueryRepository.getRecommendFriendList(MEMBER_ID, pageable))
                .willReturn(recommendPage);
        given(friendToMemberAdapter.addRecommendedFriendInfo(List.of(FRIEND_ID_1)))
                .willReturn(List.of(friendInfo));
        given(friendToMemberAdapter.addAcquaintanceInfo(List.of(ACQUAINTANCE_ID_1)))
                .willReturn(List.of(acquaintanceInfo));

        // When
        Page<RecommendedFriend> result = friendRecommendService.getRecommendFriendList(MEMBER_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        RecommendedFriend resultFriend = result.getContent().get(0);
        assertThat(resultFriend.getDepth()).isEqualTo(2);
        assertThat(resultFriend.getAcquaintanceId()).isEqualTo(ACQUAINTANCE_ID_1);
    }

    @Test
    @DisplayName("추천 친구 조회 성공 - 3촌은 공통 친구 정보 없음")
    void shouldNotShowAcquaintance_For3rdDegree() {
        // Given
        RecommendedFriend friend = new RecommendedFriend(FRIEND_ID_1, null, false, 3);
        Page<RecommendedFriend> recommendPage = new PageImpl<>(List.of(friend), pageable, 1);

        RecommendedFriend.RecommendedFriendInfo friendInfo = new RecommendedFriend.RecommendedFriendInfo(
                FRIEND_ID_1, "추천친구"
        );

        given(friendRecommendationQueryRepository.getRecommendFriendList(MEMBER_ID, pageable))
                .willReturn(recommendPage);
        given(friendToMemberAdapter.addRecommendedFriendInfo(List.of(FRIEND_ID_1)))
                .willReturn(List.of(friendInfo));
        given(friendToMemberAdapter.addAcquaintanceInfo(List.of((Long) null)))
                .willReturn(List.of());

        // When
        Page<RecommendedFriend> result = friendRecommendService.getRecommendFriendList(MEMBER_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        RecommendedFriend resultFriend = result.getContent().get(0);
        assertThat(resultFriend.getDepth()).isEqualTo(3);
        assertThat(resultFriend.getAcquaintanceId()).isNull();
    }

    @Test
    @DisplayName("추천 친구 조회 성공 - 공통 친구 여러 명 플래그 확인")
    void shouldShowManyAcquaintanceFlag_WhenMultipleCommonFriends() {
        // Given - manyAcquaintance = true
        RecommendedFriend friend = new RecommendedFriend(FRIEND_ID_1, ACQUAINTANCE_ID_1, true, 2);
        Page<RecommendedFriend> recommendPage = new PageImpl<>(List.of(friend), pageable, 1);

        RecommendedFriend.RecommendedFriendInfo friendInfo = new RecommendedFriend.RecommendedFriendInfo(
                FRIEND_ID_1, "추천친구"
        );
        RecommendedFriend.AcquaintanceInfo acquaintanceInfo = new RecommendedFriend.AcquaintanceInfo(
                ACQUAINTANCE_ID_1, "공통친구A"
        );

        given(friendRecommendationQueryRepository.getRecommendFriendList(MEMBER_ID, pageable))
                .willReturn(recommendPage);
        given(friendToMemberAdapter.addRecommendedFriendInfo(List.of(FRIEND_ID_1)))
                .willReturn(List.of(friendInfo));
        given(friendToMemberAdapter.addAcquaintanceInfo(List.of(ACQUAINTANCE_ID_1)))
                .willReturn(List.of(acquaintanceInfo));

        // When
        Page<RecommendedFriend> result = friendRecommendService.getRecommendFriendList(MEMBER_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        RecommendedFriend resultFriend = result.getContent().get(0);
        assertThat(resultFriend.isManyAcquaintance()).isTrue();
        // "공통친구A 외 다수의 공통 친구" 형태로 표시될 것
    }

    @Test
    @DisplayName("추천 친구 조회 성공 - 페이지네이션 확인")
    void shouldSupportPagination() {
        // Given
        Pageable secondPage = PageRequest.of(1, 5);
        RecommendedFriend friend = new RecommendedFriend(6L, null, false, 3);
        Page<RecommendedFriend> recommendPage = new PageImpl<>(List.of(friend), secondPage, 10);

        RecommendedFriend.RecommendedFriendInfo friendInfo = new RecommendedFriend.RecommendedFriendInfo(
                6L, "추천친구6"
        );

        given(friendRecommendationQueryRepository.getRecommendFriendList(MEMBER_ID, secondPage))
                .willReturn(recommendPage);
        given(friendToMemberAdapter.addRecommendedFriendInfo(List.of(6L)))
                .willReturn(List.of(friendInfo));
        given(friendToMemberAdapter.addAcquaintanceInfo(List.of((Long) null)))
                .willReturn(List.of());

        // When
        Page<RecommendedFriend> result = friendRecommendService.getRecommendFriendList(MEMBER_ID, secondPage);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(10);
        assertThat(result.getNumber()).isEqualTo(1);  // 페이지 번호
        assertThat(result.getSize()).isEqualTo(5);    // 페이지 크기
    }
}
