package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.member.entity.MemberBlacklist;
import jaeik.bimillog.domain.member.out.MemberBlacklistQueryRepository;
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

    public Page<String> getMyBlacklist(Long memberId, Pageable pageable) {
        return memberBlacklistQueryRepository.getMyBlacklist(memberId, pageable);
    }
}
