package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.out.MemberRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>회원 프로필 변경 서비스</h2>
 * <p>닉네임, 설정 등 기본 프로필 정보를 수정합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class MemberProfileCommandService {

    private final MemberRepository memberRepository;

    @Transactional
    public void updateMemberSettings(Long memberId, Setting newSetting) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));

        member.updateSettings(
                newSetting.isMessageNotification(),
                newSetting.isCommentNotification(),
                newSetting.isPostFeaturedNotification()
        );
    }

    @Transactional
    public void updateMemberName(Long memberId, String newMemberName) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));
            member.changeMemberName(newMemberName);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("member_name")) {
                throw new CustomException(ErrorCode.MEMBER_EXISTED_NICKNAME);
            }
            throw e;
        }
    }
}
