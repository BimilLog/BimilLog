package jaeik.growfarm.repository.user.read;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.entity.user.SocialProvider;
import jaeik.growfarm.entity.user.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
public class UserReadRepositoryImpl implements UserReadRepository {

    private final JPAQueryFactory jpaQueryFactory;

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
                .selectFrom(user)
                .where(user.userName.eq(userName))
                .fetchOne();
    }

    @Override
    public Optional<Users> findByIdWithSetting(Long id) {
        Users result = jpaQueryFactory
                .selectFrom(user)
                .leftJoin(user.setting).fetchJoin()
                .where(user.id.eq(id))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public List<String> findUserNamesInOrder(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Tuple> results = jpaQueryFactory
                .select(user.kakaoId, user.userName)
                .from(user)
                .where(user.kakaoId.in(ids))
                .fetch();

        Map<Long, String> kakaoIdToUserName = results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(user.kakaoId),
                        tuple -> Optional.ofNullable(tuple.get(user.userName)).orElse(""),
                        (existing, replacement) -> existing // 중복 키 처리
                ));

        return ids.stream()
                .map(id -> kakaoIdToUserName.getOrDefault(id, ""))
                .collect(Collectors.toList());
    }
}