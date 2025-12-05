package jaeik.bimillog.domain.friend.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.friend.entity.FriendReceiverRequest;
import jaeik.bimillog.domain.friend.entity.FriendSenderRequest;
import jaeik.bimillog.domain.friend.entity.jpa.QFriendRequest;
import jaeik.bimillog.domain.member.entity.QMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class FriendRequestQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private final QFriendRequest friendRequest = QFriendRequest.friendRequest;
    private final QMember member = QMember.member;

    // 보낸 친구 요청 조회
    public Page<FriendSenderRequest> findAllBySenderId(Long memberId, Pageable pageable) {
        List<FriendSenderRequest> content =  jpaQueryFactory
                .select(Projections.constructor(FriendSenderRequest.class,
                        friendRequest.id,
                        member.id,
                        member.memberName))
                .from(friendRequest)
                .join(friendRequest.receiver, member)
                .where(friendRequest.sender.id.eq(memberId))
                .orderBy(friendRequest.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(friendRequest.count())
                .from(friendRequest)
                .where(friendRequest.sender.id.eq(memberId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    // 받은 친구 요청 조회
    public Page<FriendReceiverRequest> findAllByReceiveId(Long memberId, Pageable pageable) {
        List<FriendReceiverRequest> content =  jpaQueryFactory
                .select(Projections.constructor(FriendReceiverRequest.class,
                        friendRequest.id,
                        member.id,
                        member.memberName))
                .from(friendRequest)
                .join(friendRequest.sender, member)
                .where(friendRequest.receiver.id.eq(memberId))
                .orderBy(friendRequest.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(friendRequest.count())
                .from(friendRequest)
                .where(friendRequest.receiver.id.eq(memberId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

}
