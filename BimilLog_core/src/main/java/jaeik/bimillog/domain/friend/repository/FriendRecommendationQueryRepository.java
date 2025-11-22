package jaeik.bimillog.domain.friend.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.friend.entity.RecommendedFriend;
import jaeik.bimillog.domain.friend.entity.jpa.QFriendRecommendation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class FriendRecommendationQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private final QFriendRecommendation friendRecommendation = QFriendRecommendation.friendRecommendation;

    public Page<RecommendedFriend> getRecommendFriendList(Long memberId, Pageable pageable) {
        List<RecommendedFriend> recommendedFriends = jpaQueryFactory
                .select(Projections.constructor(RecommendedFriend.class,
                        friendRecommendation.recommendMember.id,
                        friendRecommendation.depth,
                        friendRecommendation.acquaintance))
                .from(friendRecommendation)
                .where(friendRecommendation.member.id.eq(memberId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(friendRecommendation.count())
                .from(friendRecommendation)
                .where(friendRecommendation.member.id.eq(memberId))
                .fetchOne();

        return new PageImpl<>(recommendedFriends, pageable, total != null ? total : 0);
    }
}
