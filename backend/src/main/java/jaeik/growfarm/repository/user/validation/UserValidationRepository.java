package jaeik.growfarm.repository.user.validation;

/**
 * <h2>사용자 검증 레포지토리 인터페이스</h2>
 * <p>
 * 사용자 검증 관련 기능만 담당하는 인터페이스
 * SRP: 사용자 검증만 담당
 * ISP: 검증 기능만 노출
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
public interface UserValidationRepository {

    /**
     * <h3>사용자 이름으로 사용자 존재 여부 확인</h3>
     *
     * <p>
     * 사용자 이름이 이미 존재하는지 확인한다.
     * </p>
     *
     * @param userName 사용자 이름
     * @return 존재 여부 (true/false)
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    boolean existsByUserName(String userName);

}