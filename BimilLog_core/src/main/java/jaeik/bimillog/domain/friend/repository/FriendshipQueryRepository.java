package jaeik.bimillog.domain.friend.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.friend.entity.Friend;
import jaeik.bimillog.domain.friend.entity.FriendRelation;
import jaeik.bimillog.domain.friend.entity.jpa.QFriendship;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;


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
