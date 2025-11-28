package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.member.out.MemberCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>회원 계정 서비스</h2>
 * <p>회원 탈퇴 등 계정 생명주기를 담당합니다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberAccountService {

    private final MemberCommandRepository memberCommandRepository;

    @Transactional
    public void removeMemberAccount(Long memberId) {
        log.info("회원 계정 삭제 시작 - memberId: {}", memberId);
        memberCommandRepository.deleteMemberAndSetting(memberId);
        log.info("회원 계정 및 설정 삭제 완료 - memberId: {}", memberId);
    }
}
