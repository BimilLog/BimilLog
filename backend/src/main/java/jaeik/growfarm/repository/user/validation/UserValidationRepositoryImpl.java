package jaeik.growfarm.repository.user.validation;

import jaeik.growfarm.repository.user.UserBaseRepository;
import org.springframework.stereotype.Repository;

/**
 * <h2>사용자 검증 레포지토리 구현체</h2>
 * <p>
 * 사용자 검증 관련 기능을 구현
 * SRP: 사용자 검증 기능만 구현
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Repository
public class UserValidationRepositoryImpl extends UserBaseRepository implements UserValidationRepository {

    public UserValidationRepositoryImpl(com.querydsl.jpa.impl.JPAQueryFactory jpaQueryFactory) {
        super(jpaQueryFactory);
    }

    @Override
    public boolean existsByUserName(String userName) {
        Integer count = queryFactory
                .selectOne()
                .from(user)
                .where(user.userName.eq(userName))
                .fetchFirst();
        return count != null;
    }

}