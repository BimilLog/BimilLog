package jaeik.bimillog.domain.member.application.service;

import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.application.port.in.MemberSaveUseCase;
import jaeik.bimillog.domain.member.application.port.out.RedisMemberDataPort;
import jaeik.bimillog.domain.member.entity.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 저장 서비스</h2>
 * <p>소셜 로그인 단계에서 Member 도메인이 담당해야 할 최소 책임만 수행합니다.</p>
 * <p>기존 회원의 프로필/카카오 토큰을 갱신하거나, 신규 회원의 프로필 정보를 Redis에 임시 저장합니다.</p>
 * <p>이 서비스는 엔티티 상태 변경에만 집중하고, JWT/FCM/토큰 발급은 상위(Auth) 서비스가 조립합니다.</p>
 *
 * <h3>책임 분리:</h3>
 * <ul>
 *   <li>기존 회원: 엔티티에 최신 프로필과 카카오 토큰을 적용</li>
 *   <li>신규 회원: Redis에 {@link SocialMemberProfile}을 UUID 키와 함께 저장</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class MemberSaveService implements MemberSaveUseCase {

    private final RedisMemberDataPort redisMemberDataPort;

    @Override
    @Transactional
    public Member handleExistingMember(Member member, String newNickname, String newProfileImage, KakaoToken savedKakaoToken) {
        member.updateKakaoToken(savedKakaoToken);
        member.updateMemberInfo(newNickname, newProfileImage);
        return member;
    }

    /**
     * <h3>신규 회원 임시 정보 저장</h3>
     * <p>회원가입이 완료되기 전까지 사용할 소셜 프로필을 Redis에 저장합니다.</p>
     * <p>이 메서드는 로그인 단계에서만 호출되며, UUID는 클라이언트 임시 쿠키와 매칭됩니다.</p>
     */
    @Override
    @Transactional
    public void handleNewMember(SocialMemberProfile memberProfile, String uuid) {
        redisMemberDataPort.saveTempData(uuid, memberProfile);
    }
}

