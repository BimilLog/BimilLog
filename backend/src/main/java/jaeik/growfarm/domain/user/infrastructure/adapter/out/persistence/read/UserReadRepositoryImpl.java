package jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.read;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.user.domain.QUser;
import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.user.ClientDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>사용자 조회 레포지토리 구현체</h2>
 * <p>
 * 사용자 조회 관련 기능을 구현
 * SRP: 사용자 조회 기능만 구현
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class UserReadRepositoryImpl implements UserReadRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final QUser user = QUser.user;

    @Override
    public Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId) {
        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(user)
                .where(user.provider.eq(provider)
                        .and(user.socialId.eq(socialId)))
                .fetchOne());
    }

    @Override
    public User findByUserName(String userName) {
        return jpaQueryFactory
                .selectFrom(user)
                .where(user.userName.eq(userName))
                .fetchOne();
    }

    @Override
    public Optional<User> findByIdWithSetting(Long id) {
        User result = jpaQueryFactory
                .selectFrom(user)
                .leftJoin(user.setting).fetchJoin()
                .where(user.id.eq(id))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public ClientDTO findClientInfoById(Long id) {
        // This method's placement is questionable as it needs data from other repositories.
        // Returning null to satisfy the interface for now.
        // A proper implementation would require injecting TokenRepository and FcmTokenRepository.
        return null;
    }

    @Override
    public List<String> findUserNamesInOrder(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Tuple> results = jpaQueryFactory
                .select(user.socialId, user.userName)
                .from(user)
                .where(user.socialId.in(ids))
                .fetch();

        Map<String, String> socialIdToUserName = results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(user.socialId),
                        tuple -> Optional.ofNullable(tuple.get(user.userName)).orElse(""),
                        (existing, replacement) -> existing // Handle duplicate keys if any
                ));

        return ids.stream()
                .map(id -> socialIdToUserName.getOrDefault(id, ""))
                .collect(Collectors.toList());
    }
}