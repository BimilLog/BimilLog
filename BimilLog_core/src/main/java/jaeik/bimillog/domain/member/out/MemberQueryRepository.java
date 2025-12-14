package jaeik.bimillog.domain.member.out;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.auth.entity.QAuthToken;
import jaeik.bimillog.domain.friend.entity.Friend;
import jaeik.bimillog.domain.friend.entity.RecommendedFriend;
import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.QMember;
import jaeik.bimillog.domain.member.entity.QMemberBlacklist;
import jaeik.bimillog.domain.member.entity.QSetting;
import jaeik.bimillog.domain.member.service.MemberQueryService;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>사용자 조회 어댑터</h2>
 * <p>사용자 정보 조회를 위한 영속성 어댑터</p>
 * <p>사용자 엔티티 조회, 설정 조회, 닉네임 검증</p>
 * <p>소셜 로그인 사용자 조회, 카카오 친구 이름 매핑</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class MemberQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private final QMember member = QMember.member;
    private final QSetting setting = QSetting.setting;
    private final QAuthToken authToken = QAuthToken.authToken;

    /**
     * <h3>주어진 순서대로 사용자 이름 조회</h3>
     * <p>주어진 소셜 ID 목록에 해당하는 사용자 이름들을 요청된 순서대로 조회합니다.</p>
     * <p>카카오 친구 목록 매핑에 사용됩니다.</p>
     * <p>{@link MemberQueryService}에서 카카오 친구 목록 매핑 시 호출됩니다.</p>
     *
     * @param socialIds 조회할 소셜 ID 문자열 리스트
     * @return List<String> 조회된 사용자 이름 리스트
     * @author jaeik
     * @since  2.0.0
     */
    @Transactional(readOnly = true)
    public List<String> findMemberNamesInOrder(List<String> socialIds) {
        if (socialIds == null || socialIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Tuple> results = jpaQueryFactory
                .select(member.socialId, member.memberName)
                .from(member)
                .where(member.socialId.in(socialIds))
                .fetch();

        Map<String, String> socialIdToUserName = results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(member.socialId),
                        tuple -> Optional.ofNullable(tuple.get(member.memberName)).orElse(""),
                        (existing, replacement) -> existing // Handle duplicate keys if any
                ));

        return socialIds.stream()
                .map(id -> socialIdToUserName.getOrDefault(id, ""))
                .collect(Collectors.toList());
    }

    /**
     * <h3>여러 사용자 ID로 사용자명 배치 조회</h3>
     * <p>{@link MemberQueryService}에서 인기 롤링페이퍼 정보 보강 시 호출됩니다.</p>
     *
     * @param memberIds 조회할 사용자 ID 목록
     * @return Map<Long, String> 사용자 ID를 키로, 사용자명을 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public Map<Long, String> findMemberNamesByIds(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Tuple> results = jpaQueryFactory
                .select(member.id, member.memberName)
                .from(member)
                .where(member.id.in(memberIds))
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(member.id),
                        tuple -> Optional.ofNullable(tuple.get(member.memberName)).orElse(""),
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 여러 사용자 ID로 친구 추가 정보 조회
     * 친구 조회시 사용
     */
    public List<Friend.FriendInfo> getMyFriendPages (List<Long> friendIds) {
        return jpaQueryFactory
                .select(Projections.constructor(Friend.FriendInfo.class,
                        member.id,
                        member.memberName,
                        member.thumbnailImage
                ))
                .from(member)
                .where(member.id.in(friendIds))
                .fetch();
    }

    /**
     * 여러 사용자 ID로 추천 친구 추가 정보 조회
     */
    public List<RecommendedFriend.RecommendedFriendInfo> addRecommendedFriendInfo(List<Long> friendIds) {
        return jpaQueryFactory
                .select(Projections.constructor(RecommendedFriend.RecommendedFriendInfo.class,
                        member.id,
                        member.memberName
                ))
                .from(member)
                .where(member.id.in(friendIds))
                .fetch();
    }

    /**
     * 여러 사용자 ID로 추천 친구 아는 사람 추가 정보 조회
     */
    public List<RecommendedFriend.AcquaintanceInfo> addAcquaintanceInfo(List<Long> acquaintanceIds) {
        return jpaQueryFactory
                .select(Projections.constructor(RecommendedFriend.AcquaintanceInfo.class,
                        member.id,
                        member.memberName
                ))
                .from(member)
                .where(member.id.in(acquaintanceIds))
                .fetch();
    }

    /**
     * <h3>알림 수신 자격이 있는 FCM 토큰 조회</h3>
     * <p>사용자가 특정 타입의 알림을 받을 수 있는 경우 해당 사용자의 모든 FCM 토큰 문자열을 조회합니다.</p>
     *
     * @param memberId 사용자 ID
     * @param type   알림 타입
     * @return 알림 수신 자격이 있는 경우 FCM 토큰 문자열 목록, 없는 경우 빈 목록
     * @author Jaeik
     * @since 2.1.0
     */
    public List<String> fcmEligibleFcmTokens(Long memberId, NotificationType type) {
        return jpaQueryFactory
                .select(authToken.fcmRegistrationToken)
                .from(authToken)
                .join(authToken.member, member)
                .join(member.setting, setting)
                .where(
                        member.id.eq(memberId),
                        authToken.fcmRegistrationToken.isNotNull(),
                        notificationTypeCondition(type, setting)
                )
                .fetch();
    }

    /**
     * <h3>알림 타입별 조건 생성</h3>
     * <p>알림 타입에 따라 적절한 BooleanExpression을 생성합니다.</p>
     *
     * @param type 알림 타입
     * @param qSetting 설정 엔티티 Q클래스
     * @return 알림 타입에 해당하는 조건 표현식
     * @author Jaeik
     * @since 2.0.0
     */
    private BooleanExpression notificationTypeCondition(NotificationType type, QSetting qSetting) {
        // ADMIN, INITIATE는 항상 true (항상 전송)
        if (type == NotificationType.ADMIN || type == NotificationType.INITIATE) {
            return Expressions.TRUE;
        }

        return switch (type) {
            case MESSAGE -> qSetting.messageNotification.isTrue();
            case COMMENT -> qSetting.commentNotification.isTrue();
            case POST_FEATURED_WEEKLY, POST_FEATURED_LEGEND, POST_FEATURED_REALTIME -> qSetting.postFeaturedNotification.isTrue();
            case FRIEND -> qSetting.friendSendNotification.isTrue();
            default -> Expressions.FALSE;
        };
    }

}
