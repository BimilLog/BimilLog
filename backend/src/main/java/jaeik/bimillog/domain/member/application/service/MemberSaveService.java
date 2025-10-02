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
     * <h3>신규 사용자 임시 데이터 저장</h3>
     * <p>최초 소셜 로그인하는 사용자의 임시 정보를 저장합니다.</p>
     * <p>회원가입 페이지에서 사용할 UUID 키를 생성합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void handleNewMember(SocialMemberProfile memberProfile, String uuid) {
        redisMemberDataPort.saveTempData(uuid, memberProfile);
    }
}
