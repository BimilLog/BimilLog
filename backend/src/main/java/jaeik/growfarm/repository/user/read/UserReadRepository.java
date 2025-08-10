package jaeik.growfarm.repository.user.read;

import jaeik.growfarm.entity.user.Users;

import java.util.List;
import java.util.Optional;

/**
 * <h2>사용자 조회 레포지토리 인터페이스</h2>
 * <p>
 * 사용자 조회 관련 기능만 담당하는 인터페이스
 * SRP: 사용자 조회만 담당
 * ISP: 조회 기능만 노출
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
public interface UserReadRepository {

    /**
     * <h3>카카오 ID로 사용자 조회</h3>
     *
     * <p>
     * 카카오 ID를 통해 사용자를 조회한다.
     * </p>
     *
     * @param kakaoId 카카오 ID
     * @return 사용자 정보 (Optional)
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    Optional<Users> findByKakaoId(Long kakaoId);

    /**
     * <h3>닉네임으로 사용자 조회</h3>
     *
     * <p>
     * 닉네임을 통해 사용자를 조회한다.
     * </p>
     *
     * @param userName 유저 닉네임
     * @return 사용자 정보
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    Users findByUserName(String userName);

    /**
     * <h3>ID로 사용자와 설정 조회</h3>
     *
     * <p>
     * 사용자 ID를 통해 사용자와 설정 정보를 함께 조회한다.
     * </p>
     *
     * @param id 사용자 ID
     * @return 사용자 정보 (Setting 포함)
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    Optional<Users> findByIdWithSetting(Long id);

    /**
     * <h3>카카오 ID 목록으로 닉네임 조회</h3>
     * <p>
     * 카카오 ID 목록의 순서대로 닉네임을 조회한다.
     * </p>
     *
     * @param ids 카카오 ID 목록
     * @return 닉네임 리스트
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    List<String> findUserNamesInOrder(List<Long> ids);
}