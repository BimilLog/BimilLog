package jaeik.bimillog.domain.global.service;

import jaeik.bimillog.domain.global.out.GlobalFcmSaveAdapter;
import jaeik.bimillog.domain.global.out.GlobalMemberQueryAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.domain.notification.exception.NotificationErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * <h2>전역 FCM 토큰 저장 서비스</h2>
 * <p>여러 도메인에서 공통으로 사용하는 FCM 토큰 등록 기능을 제공하는 서비스입니다.</p>
 * <p>소셜 로그인, 회원가입 시 FCM 토큰 자동 등록에 사용됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalFcmSaveService {

    private final GlobalFcmSaveAdapter globalFcmSaveAdapter;
    private final GlobalMemberQueryAdapter globalMemberQueryAdapter;

    /**
     * <h3>FCM 토큰 등록 처리</h3>
     * <p>클라이언트에서 전송한 FCM 토큰을 서버에 등록하여 푸시 알림 수신을 준비합니다.</p>
     * <p>소셜 로그인(기존 회원)·회원가입 완료 시 자동 등록에도 재사용되며, 중복 토큰은 무시합니다.</p>
     *
     * @param fcmToken FCM 토큰 문자열 (Firebase SDK에서 생성)
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void registerFcmToken(Long memberId, String fcmToken) {

        if (fcmToken == null || fcmToken.isEmpty()) {
            throw new NotificationCustomException(NotificationErrorCode.NO_SEND_FCM_TOKEN);
        }

        Optional<Member> member = globalMemberQueryAdapter.findById(memberId);
        if (member.isPresent()) {
            FcmToken fcmTokenBuild = FcmToken.create(member.get(), fcmToken);
            globalFcmSaveAdapter.save(fcmTokenBuild);
        } else {
            throw new NotificationCustomException(NotificationErrorCode.NO_MEMBER_FCM_TOKEN);
        }
    }
}
