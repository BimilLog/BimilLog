package jaeik.bimillog.domain.global.application.service;

import jaeik.bimillog.domain.global.application.port.in.GlobalFcmSaveUseCase;
import jaeik.bimillog.domain.global.application.port.out.GlobalFcmSavePort;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GlobalFcmSaveService implements GlobalFcmSaveUseCase {

    private final GlobalFcmSavePort globalFcmSavePort;

    /**
     * <h3>FCM 토큰 등록 처리</h3>
     * <p>클라이언트에서 전송한 FCM 토큰을 서버에 등록하여 푸시 알림 수신을 준비합니다.</p>
     * <p>소셜 로그인(기존 회원)·회원가입 완료 시 자동 등록에도 재사용되며, 중복 토큰은 무시합니다.</p>
     *
     * @param member   사용자
     * @param fcmToken FCM 토큰 문자열 (Firebase SDK에서 생성)
     * @return 저장된 FCM 토큰 엔티티의 ID (토큰이 없거나 빈 값인 경우 null)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public Long registerFcmToken(Member member, String fcmToken) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            return null;
        }
        FcmToken savedToken = globalFcmSavePort.save(FcmToken.create(member, fcmToken));
        return savedToken.getId();
    }
}
