package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.member.dto.BlacklistDTO;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.MemberBlacklist;
import jaeik.bimillog.domain.member.out.MemberBlacklistQueryRepository;
import jaeik.bimillog.domain.member.out.MemberBlacklistRepository;
import jaeik.bimillog.domain.member.out.MemberRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberBlacklistService {

    private final MemberBlacklistQueryRepository memberBlacklistQueryRepository;
    private final MemberBlacklistRepository memberBlacklistRepository;
    private final MemberRepository memberRepository;

    public Page<BlacklistDTO> getMyBlacklist(Long memberId, Pageable pageable) {
        return memberBlacklistQueryRepository.getMyBlacklist(memberId, pageable);
    }

    public void addMyBlacklist(Long memberId, String memberName) {
        Member requestMember = memberRepository.getReferenceById(memberId);
        Member blackMember = memberRepository.findByMemberName(memberName)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));

        MemberBlacklist blacklist = MemberBlacklist.createMemberBlacklist(requestMember, blackMember);
        memberBlacklistRepository.save(blacklist);
    }
}
