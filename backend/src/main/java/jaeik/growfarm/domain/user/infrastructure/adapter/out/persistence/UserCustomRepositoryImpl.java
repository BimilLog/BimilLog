package jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.user.entity.QUser;
import jaeik.growfarm.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserCustomRepositoryImpl implements UserCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final QUser user = QUser.user;

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
