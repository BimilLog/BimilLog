package jaeik.bimillog.domain.member.application.service;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.application.port.in.MemberSaveUseCase;
import jaeik.bimillog.domain.member.application.port.out.MemberQueryPort;
import jaeik.bimillog.domain.member.application.port.out.RedisMemberDataPort;
import jaeik.bimillog.domain.member.application.port.out.SaveMemberPort;
import jaeik.bimillog.domain.member.entity.MemberDetail;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.infrastructure.adapter.out.auth.AuthToMemberAdapter;
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
public class MemberSaveService implements MemberSaveUseCase {

    private final MemberQueryPort memberQueryPort;
    private final SaveMemberPort saveMemberPort;
    private final RedisMemberDataPort redisMemberDataPort;

    /**
     * <h3>사용자 데이터 저장 및 처리</h3>
     * <p>소셜 로그인 정보를 바탕으로 사용자 데이터를 저장하거나 업데이트합니다.</p>
     * <p>기존 사용자는 정보를 업데이트하고, 신규 사용자는 임시 데이터를 저장합니다.</p>
     * <p>{@link AuthToMemberAdapter}에서 Auth 도메인의 요청을 받아 호출됩니다.</p>
     * <p>기존 사용자와 신규 사용자를 구분하여 각각의 로그인 처리를 수행합니다.</p>
     * <p>기존 사용자: 프로필 업데이트 후 즉시 로그인 완료 (uuid = null)</p>
     * <p>신규 사용자: 임시 데이터 저장 후 회원가입 페이지로 안내 (uuid != null)</p>
     *
     * @param userProfile 소셜 사용자 프로필 정보 (FCM 토큰, provider 포함)
     * @return MemberDetail 기존 사용자(uuid = null) 또는 신규 사용자(uuid != null) 정보
     * @author Jaeik
     * @since 3.0.0
     */
    @Override
    public MemberDetail processUserData(SocialMemberProfile userProfile) {
        Optional<Member> existingUser = memberQueryPort.findByProviderAndSocialId(userProfile.getProvider(), userProfile.getSocialId());
        if (existingUser.isPresent()) {
            Member member = existingUser.get();
            return saveMemberPort.handleExistingUserData(member, userProfile);
        } else {
            return handleNewUser(userProfile);
        }
    }

    /**
     * <h3>신규 사용자 임시 데이터 저장</h3>
     * <p>최초 소셜 로그인하는 사용자의 임시 정보를 저장합니다.</p>
     * <p>회원가입 페이지에서 사용할 UUID 키를 생성합니다.</p>
     * <p>{@link #processUserData(SocialMemberProfile)}에서 신규 사용자 판별 후 호출됩니다.</p>
     *
     * @param authResult 소셜 로그인 인증 결과 (FCM 토큰 포함)
     * @return MemberDetail 회원가입용 UUID를 포함하는 신규 사용자 정보
     * @author Jaeik
     * @since 3.0.0
     */
    private MemberDetail handleNewUser(SocialMemberProfile authResult) {
        String uuid = UUID.randomUUID().toString();
        redisMemberDataPort.saveTempData(uuid, authResult);
        return MemberDetail.ofNew(uuid);
    }
}
