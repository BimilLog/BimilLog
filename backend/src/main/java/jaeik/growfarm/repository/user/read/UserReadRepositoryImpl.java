package jaeik.growfarm.repository.user.read;

import com.querydsl.core.Tuple;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.user.UserBaseRepository;
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
public class UserReadRepositoryImpl extends UserBaseRepository implements UserReadRepository {

    public UserReadRepositoryImpl(com.querydsl.jpa.impl.JPAQueryFactory jpaQueryFactory) {
        super(jpaQueryFactory);
    }

    @Override
    public Optional<Users> findByKakaoId(Long kakaoId) {
        Users result = jpaQueryFactory
                .selectFrom(user)
                .where(user.kakaoId.eq(kakaoId))
                .fetchOne();
        return Optional.ofNullable(result);
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