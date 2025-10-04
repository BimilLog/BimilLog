package jaeik.bimillog.domain.member.application.service;

import jaeik.bimillog.domain.member.application.port.in.MemberCommandUseCase;
import jaeik.bimillog.domain.member.application.port.out.MemberCommandPort;
import jaeik.bimillog.domain.member.application.port.out.MemberQueryPort;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.exception.MemberCustomException;
import jaeik.bimillog.domain.member.exception.MemberErrorCode;
import jaeik.bimillog.infrastructure.adapter.in.member.web.MemberCommandController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 명령 서비스</h2>
 * <p>UserCommandUseCase의 구현체로 사용자 정보 수정 로직을 담당합니다.</p>
 * <p>사용자 알림 설정 수정, 닉네임 변경</p>
 * <p>Race Condition 방지, 데이터베이스 제약조건 예외 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberCommandService implements MemberCommandUseCase {

    private final MemberQueryPort memberQueryPort;
    private final MemberCommandPort memberCommandPort;

    /**
     * <h3>사용자 설정 수정</h3>
     * <p>사용자의 알림 설정을 수정합니다.</p>
     * <p>{@link MemberCommandController}에서 설정 수정 API 요청 시 호출됩니다.</p>
     *
     * @param memberId   사용자 ID
     * @param newSetting 수정할 설정 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void updateMemberSettings(Long memberId, Setting newSetting) {
        Member member = memberQueryPort.findById(memberId)
                .orElseThrow(() -> new MemberCustomException(MemberErrorCode.USER_NOT_FOUND));

        member.updateSettings(
                newSetting.isMessageNotification(),
                newSetting.isCommentNotification(),
                newSetting.isPostFeaturedNotification()
        );
    }

    /**
     * <h3>닉네임 변경</h3>
     * <p>사용자의 닉네임을 변경합니다.</p>
     * <p>Race Condition 방지를 위해 데이터베이스 UNIQUE 제약조건 위반 예외를 처리합니다.</p>
     * <p>{@link MemberCommandController}에서 닉네임 변경 API 요청 시 호출됩니다.</p>
     *
     * @param memberId      사용자 ID
     * @param newMemberName 새로운 닉네임
     * @throws MemberCustomException EXISTED_NICKNAME - 닉네임이 중복된 경우
     * @throws MemberCustomException USER_NOT_FOUND - 사용자를 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void updateMemberName(Long memberId, String newMemberName) {
        try {
            Member member = memberQueryPort.findById(memberId)
                    .orElseThrow(() -> new MemberCustomException(MemberErrorCode.USER_NOT_FOUND));
            member.changeMemberName(newMemberName);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("member_name")) {
                throw new MemberCustomException(MemberErrorCode.EXISTED_NICKNAME);
            }
            throw e;
        }
    }

    /**
     * <h3>사용자 계정 삭제</h3>
     * <p>회원 탈퇴 시 사용자 계정과 설정을 데이터베이스에서 완전히 삭제합니다.</p>
     * <p>Native Query를 통해 Member와 Setting을 원자적으로 삭제합니다.</p>
     * <p>UserWithdrawListener에서 회원 탈퇴 이벤트 처리 흐름 중 호출됩니다.</p>
     *
     * @param memberId 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void removeMemberAccount(Long memberId) {
        log.info("사용자 계정 삭제 시작 - memberId: {}", memberId);
        memberCommandPort.deleteMemberAndSetting(memberId);
        log.info("사용자 계정 및 설정 삭제 완료 - memberId: {}", memberId);
    }
}
