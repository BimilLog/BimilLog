package jaeik.bimillog.domain.global.application.port.in;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.infrastructure.adapter.in.notification.web.NotificationSseController;

public interface GlobalFcmSaveUseCase {

    /**
     * <h3>FCM 토큰 등록</h3>
     * <p>클라이언트 앱의 FCM 토큰을 서버에 등록합니다.</p>
     * <p>중복 토큰 검사, 다중 기기 지원</p>
     * <p>{@link NotificationSseController}에서 클라이언트의 토큰 등록 API 요청 시 호출됩니다.</p>
     *
     * @param member   사용자
     * @param fcmToken FCM 토큰 문자열 (Firebase SDK에서 생성)
     * @return 저장된 FCM 토큰 엔티티의 ID (토큰이 없거나 빈 값인 경우 null)
     * @author Jaeik
     * @since 2.0.0
     */
    Long registerFcmToken(Member member, String fcmToken);
}
