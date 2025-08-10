package jaeik.growfarm.repository.user;

import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.user.read.UserReadRepository;
import jaeik.growfarm.repository.user.validation.UserValidationRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>사용자 Repository 파사드</h2>
 * <p>
 * 사용자 관련 데이터베이스 작업을 조정하는 Facade Repository
 * Facade Pattern: UserReadRepository, UserValidationRepository를 조정
 * </p>
 * 
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<Users, Long>, UserReadRepository, UserValidationRepository {

    // 모든 메서드는 UserReadRepository와 UserValidationRepository에서 상속
}