package jaeik.bimillog.domain.member.application.port.in;

import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.in.member.web.MemberCommandController;

/**
 * <h2>사용자 명령 유스케이스</h2>
 * <p>사용자 관련 명령 요청을 처리하는 입력 포트입니다.</p>
 * <p>사용자 설정 수정, 닉네임 변경</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface MemberCommandUseCase {

    /**
     * <h3>사용자 설정 수정</h3>
     * <p>사용자의 알림 설정을 수정합니다.</p>
     * <p>{@link MemberCommandController}에서 설정 수정 API 요청 시 호출됩니다.</p>
     *
     * @param memberId    사용자 ID
     * @param setting 수정할 설정 정보
     * @since 2.0.0
     * @author Jaeik
     */
    void updateMemberSettings(Long memberId, Setting setting);

    /**
     * <h3>닉네임 변경</h3>
     * <p>사용자의 닉네임을 변경합니다.</p>
     * <p>{@link MemberCommandController}에서 닉네임 변경 API 요청 시 호출됩니다.</p>
     *
     * @param memberId      사용자 ID
     * @param newUserName 새로운 닉네임
     * @since 2.0.0
     * @author Jaeik
     */
    void updateMemberName(Long memberId, String newUserName);

    /**
     * <h3>사용자 엔티티 삭제</h3>
     * <p>회원 탈퇴 시 사용자 엔티티를 데이터베이스에서 완전히 삭제합니다.</p>
     * <p>사용자 삭제 이후 설정도 삭제 합니다</p>
     * <p>MemberWithdrawnEvent 이벤트 발생시 회원 탈퇴 처리 흐름에서 호출됩니다.</p>
     *
     * @param memberId 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void removeMemberAccount(Long memberId);
}
