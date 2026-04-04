package jaeik.bimillog.domain.auth.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.auth.entity.QSocialToken;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SocialTokenQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private final QSocialToken socialToken = QSocialToken.socialToken;

    public Optional<SocialToken> findSocialTokenByMemberId(Long memberId) {
        SocialToken result = jpaQueryFactory
                .selectFrom(socialToken)
                .where(socialToken.member.id.eq(memberId))
                .fetchOne();
        return Optional.ofNullable(result);
    }
}
