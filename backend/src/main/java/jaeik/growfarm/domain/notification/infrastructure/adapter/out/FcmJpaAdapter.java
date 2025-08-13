package jaeik.growfarm.domain.notification.infrastructure.adapter.out;

import jaeik.growfarm.domain.notification.entity.FcmToken;
import jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.FcmTokenRepository;
import jaeik.growfarm.domain.notification.application.port.out.LoadFcmTokenPort;
import jaeik.growfarm.domain.notification.application.port.out.SaveFcmTokenPort;
import jaeik.growfarm.domain.notification.application.port.out.DeleteFcmTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>FCM JPA 어댑터</h2>
 * <p>FCM 토큰 관련 데이터베이스 조회를 처리하는 Secondary Adapter</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class FcmJpaAdapter implements LoadFcmTokenPort, SaveFcmTokenPort, DeleteFcmTokenPort {

    private final FcmTokenRepository fcmTokenRepository;

    /**
     * <h3>사용자 ID로 유효한 FCM 토큰 조회</h3>
     * <p>주어진 사용자 ID에 해당하는 유효한 FCM 토큰 목록을 조회합니다.</p>
     *
     * @param userId 조회할 사용자의 ID
     * @return FCM 토큰 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<FcmToken> findValidFcmTokensByUserId(Long userId) {
        return fcmTokenRepository.findValidFcmTokensByUserId(userId);
    }

    /**
     * <h3>FCM 토큰 저장</h3>
     * <p>FCM 토큰 엔티티를 저장합니다.</p>
     *
     * @param fcmToken 저장할 FCM 토큰 엔티티
     * @return 저장된 FCM 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public FcmToken save(FcmToken fcmToken) {
        return fcmTokenRepository.save(fcmToken);
    }

    /**
     * <h3>사용자 ID로 FCM 토큰 삭제</h3>
     * <p>주어진 사용자 ID에 해당하는 모든 FCM 토큰을 삭제합니다.</p>
     *
     * @param userId 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteByUserId(Long userId) {
        fcmTokenRepository.deleteByUser_Id(userId);
    }
}
