package jaeik.bimillog.domain.user.application.port.in;

import jaeik.bimillog.domain.user.entity.SettingVO;
import jaeik.bimillog.infrastructure.exception.CustomException;

/**
 * <h2>설정 조회 유스케이스</h2>
 * <p>사용자 설정 관련 조회 요청을 처리하는 인터페이스</p>
 * <p>헥사고날 아키텍처의 Primary Port로, 설정 조회에 대한 비즈니스 로직을 정의</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
public interface SettingQueryUseCase {

    /**
     * <h3>설정 ID로 설정 조회</h3>
     * <p>JWT 토큰의 settingId를 활용하여 효율적으로 설정 정보를 조회</p>
     *
     * @param settingId 설정 ID
     * @return 설정 VO
     * @throws CustomException 설정을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    SettingVO findBySettingId(Long settingId);
}