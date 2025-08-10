package jaeik.growfarm.repository.user;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.entity.user.QUsers;

/**
 * <h2>사용자 기본 레포지토리</h2>
 * <p>
 * 사용자 관련 QueryDSL 공통 기능을 제공하는 추상 클래스
 * DIP: 구체적인 구현체들이 이 추상화에 의존
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
public abstract class UserBaseRepository {

    protected final JPAQueryFactory jpaQueryFactory;
    protected final QUsers user = QUsers.users;

    protected UserBaseRepository(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }
}