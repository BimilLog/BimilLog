package jaeik.bimillog.testutil.builder;

import jaeik.bimillog.domain.friend.entity.jpa.FriendRecommendation;
import jaeik.bimillog.domain.friend.entity.jpa.FriendRequest;
import jaeik.bimillog.domain.friend.entity.jpa.Friendship;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.MemberBlacklist;
import jaeik.bimillog.testutil.fixtures.TestFixtures;

/**
 * Friend 도메인 테스트 데이터 빌더
 * <p>
 * Friend 관련 테스트 데이터 생성 유틸리티
 *
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>FriendRequest 생성</li>
 *   <li>Friendship 생성</li>
 *   <li>FriendRecommendation 생성</li>
 *   <li>MemberBlacklist 생성</li>
 *   <li>ID가 설정된 엔티티 생성 (리플렉션)</li>
 * </ul>
 */
public class FriendTestDataBuilder {

    // ==================== FriendRequest ====================

    /**
     * 친구 요청 엔티티 생성
     * @param sender 요청 보낸 사람
     * @param receiver 요청 받은 사람
     * @return FriendRequest
     */
    public static FriendRequest createFriendRequest(Member sender, Member receiver) {
        return FriendRequest.createFriendRequest(sender, receiver);
    }

    /**
     * ID가 설정된 친구 요청 엔티티 생성
     * @param id 친구 요청 ID
     * @param sender 요청 보낸 사람
     * @param receiver 요청 받은 사람
     * @return ID가 설정된 FriendRequest
     */
    public static FriendRequest createFriendRequestWithId(Long id, Member sender, Member receiver) {
        FriendRequest friendRequest = createFriendRequest(sender, receiver);
        TestFixtures.setFieldValue(friendRequest, "id", id);
        return friendRequest;
    }

    /**
     * ID가 설정된 친구 요청 생성 (기존 객체에 ID 추가)
     * @param id 설정할 ID
     * @param friendRequest 원본 친구 요청
     * @return ID가 설정된 FriendRequest
     */
    public static FriendRequest withId(Long id, FriendRequest friendRequest) {
        TestFixtures.setFieldValue(friendRequest, "id", id);
        return friendRequest;
    }

    // ==================== Friendship ====================

    /**
     * 친구 관계 엔티티 생성
     * @param member 회원
     * @param friend 친구
     * @return Friendship
     */
    public static Friendship createFriendship(Member member, Member friend) {
        return Friendship.createFriendship(member, friend);
    }

    /**
     * ID가 설정된 친구 관계 엔티티 생성
     * @param id 친구 관계 ID
     * @param member 회원
     * @param friend 친구
     * @return ID가 설정된 Friendship
     */
    public static Friendship createFriendshipWithId(Long id, Member member, Member friend) {
        Friendship friendship = createFriendship(member, friend);
        TestFixtures.setFieldValue(friendship, "id", id);
        return friendship;
    }

    /**
     * ID가 설정된 친구 관계 생성 (기존 객체에 ID 추가)
     * @param id 설정할 ID
     * @param friendship 원본 친구 관계
     * @return ID가 설정된 Friendship
     */
    public static Friendship withId(Long id, Friendship friendship) {
        TestFixtures.setFieldValue(friendship, "id", id);
        return friendship;
    }

    // ==================== FriendRecommendation ====================

    /**
     * 추천 친구 엔티티 생성 (기본)
     * @param member 추천을 보는 사람
     * @param recommendMember 추천된 대상
     * @param score 점수
     * @param depth 촌수 (2촌, 3촌)
     * @return FriendRecommendation
     */
    public static FriendRecommendation createRecommendation(
            Member member,
            Member recommendMember,
            Integer score,
            Integer depth
    ) {
        return createRecommendation(member, recommendMember, score, depth, null, false);
    }

    /**
     * 추천 친구 엔티티 생성 (전체)
     * @param member 추천을 보는 사람
     * @param recommendMember 추천된 대상
     * @param score 점수
     * @param depth 촌수 (2촌, 3촌)
     * @param acquaintanceId 공통 친구 ID (3촌 이상은 null)
     * @param manyAcquaintance 공통 친구가 여러 명인지 여부
     * @return FriendRecommendation
     */
    public static FriendRecommendation createRecommendation(
            Member member,
            Member recommendMember,
            Integer score,
            Integer depth,
            Long acquaintanceId,
            boolean manyAcquaintance
    ) {
        try {
            FriendRecommendation recommendation = FriendRecommendation.class.getDeclaredConstructor().newInstance();
            TestFixtures.setFieldValue(recommendation, "member", member);
            TestFixtures.setFieldValue(recommendation, "recommendMember", recommendMember);
            TestFixtures.setFieldValue(recommendation, "score", score);
            TestFixtures.setFieldValue(recommendation, "depth", depth);
            TestFixtures.setFieldValue(recommendation, "acquaintanceId", acquaintanceId);
            TestFixtures.setFieldValue(recommendation, "manyAcquaintance", manyAcquaintance);
            return recommendation;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create FriendRecommendation", e);
        }
    }

    /**
     * ID가 설정된 추천 친구 엔티티 생성
     * @param id 추천 친구 ID
     * @param member 추천을 보는 사람
     * @param recommendMember 추천된 대상
     * @param score 점수
     * @param depth 촌수
     * @param acquaintanceId 공통 친구 ID
     * @param manyAcquaintance 공통 친구가 여러 명인지 여부
     * @return ID가 설정된 FriendRecommendation
     */
    public static FriendRecommendation createRecommendationWithId(
            Long id,
            Member member,
            Member recommendMember,
            Integer score,
            Integer depth,
            Long acquaintanceId,
            boolean manyAcquaintance
    ) {
        FriendRecommendation recommendation = createRecommendation(
                member, recommendMember, score, depth, acquaintanceId, manyAcquaintance
        );
        TestFixtures.setFieldValue(recommendation, "id", id);
        return recommendation;
    }

    /**
     * ID가 설정된 추천 친구 생성 (기존 객체에 ID 추가)
     * @param id 설정할 ID
     * @param recommendation 원본 추천 친구
     * @return ID가 설정된 FriendRecommendation
     */
    public static FriendRecommendation withId(Long id, FriendRecommendation recommendation) {
        TestFixtures.setFieldValue(recommendation, "id", id);
        return recommendation;
    }

    // ==================== MemberBlacklist ====================

    /**
     * 블랙리스트 엔티티 생성
     * @param requester 차단을 요청한 사람
     * @param blocked 차단된 사람
     * @return MemberBlacklist
     */
    public static MemberBlacklist createBlacklist(Member requester, Member blocked) {
        return MemberBlacklist.createMemberBlacklist(requester, blocked);
    }

    /**
     * ID가 설정된 블랙리스트 엔티티 생성
     * @param id 블랙리스트 ID
     * @param requester 차단을 요청한 사람
     * @param blocked 차단된 사람
     * @return ID가 설정된 MemberBlacklist
     */
    public static MemberBlacklist createBlacklistWithId(Long id, Member requester, Member blocked) {
        MemberBlacklist blacklist = createBlacklist(requester, blocked);
        TestFixtures.setFieldValue(blacklist, "id", id);
        return blacklist;
    }

    /**
     * ID가 설정된 블랙리스트 생성 (기존 객체에 ID 추가)
     * @param id 설정할 ID
     * @param blacklist 원본 블랙리스트
     * @return ID가 설정된 MemberBlacklist
     */
    public static MemberBlacklist withId(Long id, MemberBlacklist blacklist) {
        TestFixtures.setFieldValue(blacklist, "id", id);
        return blacklist;
    }
}
