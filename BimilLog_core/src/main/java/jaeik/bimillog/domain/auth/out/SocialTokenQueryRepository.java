package jaeik.bimillog.domain.auth.out;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.auth.entity.QSocialToken;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.member.entity.QMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SocialTokenQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private final QMember member = QMember.member;
    private final QSocialToken socialToken = QSocialToken.socialToken;


    public Optional<SocialToken> findSocialTokenByMemberId(Long memberId) {
        SocialToken result = jpaQueryFactory
                .select(socialToken)
                .from(member)
                .join(member.socialToken, socialToken)
                .where(member.id.eq(memberId))
                .fetchOne();

        return Optional.ofNullable(result);

    }
}
