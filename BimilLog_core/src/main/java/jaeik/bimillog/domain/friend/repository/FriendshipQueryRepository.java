package jaeik.bimillog.domain.friend.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.friend.entity.Friend;
import jaeik.bimillog.domain.friend.entity.jpa.QFriendship;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.*;


@Repository
@RequiredArgsConstructor
public class FriendshipQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private final QFriendship friendship = QFriendship.friendship;

    // 자신의 ID가 Member에 속해있든 Friend에 속해있든 반대편의 ID를 가지고 온다.
    // Friend 클래스에 매핑한다.
    public Page<Friend> getMyFriendIds(Long myMemberId, Pageable pageable) {
        List<Friend> friendPage = jpaQueryFactory
                .select(Projections.constructor(Friend.class,
                                friendship.id,
                        new CaseBuilder()
                        .when(friendship.member.id.eq(myMemberId)).then(friendship.friend.id)
                        .otherwise(friendship.member.id),
                        friendship.createdAt))
                .from(friendship)
                .where(friendship.member.id.eq(myMemberId)
                        .or(friendship.friend.id.eq(myMemberId)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(friendship.count())
                .from(friendship)
                .where(friendship.member.id.eq(myMemberId)
                        .or(friendship.friend.id.eq(myMemberId)))
                .fetchOne();

        return new PageImpl<>(friendPage, pageable, total != null ? total : 0);

    }

    /**
     * DB에서 특정 회원의 1촌 친구 ID 목록을 조회합니다.
     * Redis 폴백용으로, 양방향 친구 관계를 처리합니다.
     *
     * @param memberId 조회할 회원 ID
     * @param limit    최대 조회 수
     * @return 친구 ID Set
     */
    public Set<Long> getMyFriendIdsSet(Long memberId, int limit) {
        List<Long> friendIds = jpaQueryFactory
                .select(new CaseBuilder()
                        .when(friendship.member.id.eq(memberId)).then(friendship.friend.id)
                        .otherwise(friendship.member.id))
                .from(friendship)
                .where(friendship.member.id.eq(memberId)
                        .or(friendship.friend.id.eq(memberId)))
                .limit(limit)
                .fetch();
        return new HashSet<>(friendIds);
    }

    /**
     * 여러 회원의 친구 목록을 한 번의 쿼리로 조회합니다.
     * Redis 폴백용으로, 양방향 친구 관계를 Java에서 매핑합니다.
     *
     * @param memberIdList 조회할 회원 ID 목록 (순서 유지)
     * @return 파이프라인 결과 (memberIdList 순서와 동일)
     */
    public List<Set<Long>> getFriendIdsBatch(List<Long> memberIdList) {
        if (memberIdList.isEmpty()) return new ArrayList<>();

        Set<Long> memberIdSet = new HashSet<>(memberIdList);

        List<Tuple> results = jpaQueryFactory
                .select(friendship.member.id, friendship.friend.id)
                .from(friendship)
                .where(friendship.member.id.in(memberIdSet)
                        .or(friendship.friend.id.in(memberIdSet)))
                .fetch();

        Map<Long, Set<Long>> resultMap = new HashMap<>();
        for (Long id : memberIdList) {
            resultMap.put(id, new HashSet<>());
        }

        for (Tuple tuple : results) {
            Long mId = tuple.get(friendship.member.id);
            Long fId = tuple.get(friendship.friend.id);

            if (memberIdSet.contains(mId)) {
                resultMap.get(mId).add(fId);
            }
            if (memberIdSet.contains(fId)) {
                resultMap.get(fId).add(mId);
            }
        }

        // memberIdList 순서대로 결과 반환
        List<Set<Long>> resultList = new ArrayList<>();
        for (Long id : memberIdList) {
            resultList.add(resultMap.get(id));
        }

        return resultList;
    }

    // 모든 멤버의 본인, 1촌, 2촌 연결관계를 가지고 온다.
    public List<Tuple> findAllTwoDegreeRelations() {
        QFriendship f1 = QFriendship.friendship;
        QFriendship f2 = new QFriendship("f2");

        return jpaQueryFactory
                .select(f1.member.id, f1.friend.id, f2.friend.id)
                .from(f1)
                .join(f2).on(f1.friend.id.eq(f2.member.id))
                .where(f2.friend.id.ne(f1.member.id),
                        f2.friend.id.ne(f1.friend.id))
                .fetch();
    }
}
