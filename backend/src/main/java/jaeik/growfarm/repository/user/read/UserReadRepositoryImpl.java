package jaeik.growfarm.repository.user.read;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.entity.user.SocialProvider;
import jaeik.growfarm.entity.user.Users;
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
    private final QUsers users = QUsers.users;

    @Override
    public Optional<Users> findByProviderAndSocialId(SocialProvider provider, String socialId) {
        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(users)
                .where(users.provider.eq(provider)
                        .and(users.socialId.eq(socialId)))
                .fetchOne());
    }

    @Override
    public Users findByUserName(String userName) {
        return jpaQueryFactory
                .selectFrom(users)
                .where(users.userName.eq(userName))
                .fetchOne();
    }

    @Override
    public Optional<Users> findByIdWithSetting(Long id) {
        Users result = jpaQueryFactory
                .selectFrom(users)
                .leftJoin(users.setting).fetchJoin()
                .where(users.id.eq(id))
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
                .select(users.socialId, users.userName)
                .from(users)
                .where(users.socialId.in(ids))
                .fetch();

        Map<String, String> socialIdToUserName = results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(users.socialId),
                        tuple -> Optional.ofNullable(tuple.get(users.userName)).orElse(""),
                        (existing, replacement) -> existing // Handle duplicate keys if any
                ));

        return ids.stream()
                .map(id -> socialIdToUserName.getOrDefault(id, ""))
                .collect(Collectors.toList());
    }
}