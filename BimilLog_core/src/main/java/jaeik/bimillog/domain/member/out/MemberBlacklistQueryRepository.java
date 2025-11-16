package jaeik.bimillog.domain.member.out;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.member.entity.QMember;
import jaeik.bimillog.domain.member.entity.QMemberBlacklist;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberBlacklistQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private final QMember member = QMember.member;
    private final QMemberBlacklist memberBlacklist = QMemberBlacklist.memberBlacklist;

    public Page<String> getMyBlacklist(Long memberId, Pageable pageable) {
        List<String> content = jpaQueryFactory
                .select(member.memberName)
                .from(memberBlacklist)
                .join(member, memberBlacklist.blackMember)
                .where(memberBlacklist.requestMember.id.eq(memberId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(memberBlacklist.count())
                .from(memberBlacklist)
                .where(memberBlacklist.requestMember.id.eq(memberId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }
}
