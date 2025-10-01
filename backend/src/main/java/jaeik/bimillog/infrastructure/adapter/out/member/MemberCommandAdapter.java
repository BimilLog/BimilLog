package jaeik.bimillog.infrastructure.adapter.out.member;

import jaeik.bimillog.domain.member.application.port.out.MemberCommandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 명령 어댑터</h2>
 * <p>사용자 도메인의 명령 작업을 담당하는 아웃바운드 어댑터입니다.</p>
 * <p>UserCommandPort의 구현체로 사용자 삭제 등의 명령 작업을 처리합니다.</p>
 * <p>Native Query를 통한 효율적인 데이터 삭제</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MemberCommandAdapter implements MemberCommandPort {

    private final MemberRepository userRepository;
    private final SettingRepository settingRepository;

    /**
     * <h3>사용자 계정과 설정 삭제</h3>
     * <p>Member를 먼저 조회하여 settingId를 저장한 후, Member → Setting 순서로 삭제합니다.</p>
     * <p>FK constraint를 고려하여 Member를 먼저 삭제해야 합니다.</p>
     *
     * @param memberId 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void deleteMemberAndSetting(Long memberId) {
        log.debug("회원 계정 삭제 수행 - memberId: {}", memberId);

        // 1. settingId 조회 (Member 삭제 전)
        Long settingId = userRepository.findById(memberId)
                .map(member -> member.getSetting().getId())
                .orElse(null);

        // 2. Member 먼저 삭제 (FK 참조 제거)
        userRepository.deleteByMemberId(memberId);

        // 3. Setting 나중 삭제
        if (settingId != null) {
            settingRepository.deleteById(settingId);
        }

        log.debug("회원 계정 삭제 완료 - memberId: {}", memberId);
    }
}