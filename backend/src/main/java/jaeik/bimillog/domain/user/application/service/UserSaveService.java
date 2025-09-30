package jaeik.bimillog.domain.user.application.service;

import jaeik.bimillog.infrastructure.adapter.out.api.dto.SocialUserProfileDTO;
import jaeik.bimillog.domain.user.application.port.in.UserSaveUseCase;
import jaeik.bimillog.domain.user.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.user.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.userdetail.NewUserDetail;
import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import jaeik.bimillog.domain.user.entity.user.User;
import jaeik.bimillog.domain.user.entity.userdetail.UserDetail;
import jaeik.bimillog.infrastructure.adapter.out.auth.AuthToUserAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * <h2>사용자 저장 서비스</h2>
 * <p>소셜 로그인 시 사용자 데이터 저장 및 처리를 담당하는 서비스입니다.</p>
 * <p>기존 사용자와 신규 사용자를 구분하여 각각 적절한 처리를 수행합니다.</p>
 * <p>Auth 도메인과 분리되어 순수하게 사용자 데이터 관리 책임만 가집니다.</p>
 *
 * <h3>주요 책임:</h3>
 * <ul>
 *   <li>소셜 로그인 사용자 조회 및 판별</li>
 *   <li>기존 사용자: 프로필 업데이트 및 토큰 저장</li>
 *   <li>신규 사용자: 임시 데이터 저장 및 UUID 발급</li>
 *   <li>FCM 토큰 등록 요청</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class UserSaveService implements UserSaveUseCase {

    private final UserQueryPort userQueryPort;
    private final SaveUserPort saveUserPort;
    private final RedisUserDataPort redisUserDataPort;

    /**
     * <h3>사용자 데이터 저장 및 처리</h3>
     * <p>소셜 로그인 정보를 바탕으로 사용자 데이터를 저장하거나 업데이트합니다.</p>
     * <p>기존 사용자는 정보를 업데이트하고, 신규 사용자는 임시 데이터를 저장합니다.</p>
     * <p>{@link AuthToUserAdapter}에서 Auth 도메인의 요청을 받아 호출됩니다.</p>
     * <p>기존 사용자와 신규 사용자를 구분하여 각각의 로그인 처리를 수행합니다.</p>
     * <p>기존 사용자: 프로필 업데이트 후 즉시 로그인 완료</p>
     * <p>신규 사용자: 임시 데이터 저장 후 회원가입 페이지로 안내</p>
     *
     * @param provider 소셜 로그인 제공자 (KAKAO 등)
     * @param authResult 소셜 사용자 프로필 정보
     * @param fcmToken FCM 토큰 (선택사항)
     * @return UserDetail 기존 사용자(ExistingUserDetail) 또는 신규 사용자(NewUserDetail) 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public UserDetail processUserData(SocialProvider provider, SocialUserProfileDTO authResult, String fcmToken) {
        Optional<User> existingUser = userQueryPort.findByProviderAndSocialId(provider, authResult.getSocialId());
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            return saveUserPort.handleExistingUserData(user, authResult, fcmToken);
        } else {
            return handleNewUser(authResult, fcmToken);
        }
    }

    /**
     * <h3>신규 사용자 임시 데이터 저장</h3>
     * <p>최초 소셜 로그인하는 사용자의 임시 정보를 저장합니다.</p>
     * <p>회원가입 페이지에서 사용할 UUID 키와 임시 쿠키를 생성합니다.</p>
     * <p>{@link #processUserData(SocialProvider, SocialUserProfileDTO, String)}에서 신규 사용자 판별 후 호출됩니다.</p>
     *
     * @param authResult 소셜 로그인 인증 결과
     * @param fcmToken 푸시 알림용 FCM 토큰 (선택사항)
     * @return NewUser 회원가입용 UUID와 임시 쿠키 정보
     * @author Jaeik
     * @since 2.0.0
     */
    private NewUserDetail handleNewUser(SocialUserProfileDTO authResult, String fcmToken) {
        String uuid = UUID.randomUUID().toString();
        redisUserDataPort.saveTempData(uuid, authResult, fcmToken);
        return NewUserDetail.of(uuid);
    }
}
