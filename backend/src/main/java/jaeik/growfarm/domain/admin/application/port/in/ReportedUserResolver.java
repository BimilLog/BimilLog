package jaeik.growfarm.domain.admin.application.port.in;

import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.user.entity.User;

/**
 * <h2>신고 사용자 해결사 인터페이스</h2>
 * <p>다양한 신고 유형에 대해 신고 대상 사용자 정보를 해결하기 위한 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface ReportedUserResolver {
    /**
     * <h3>신고 대상 사용자 해결</h3>
     * <p>주어진 대상 ID에 해당하는 사용자 정보를 해결하여 반환합니다.</p>
     *
     * @param targetId 신고 대상 ID
     * @return User 신고 대상 사용자 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    User resolve(Long targetId);

    /**
     * <h3>지원하는 신고 유형 반환</h3>
     * <p>이 해결사가 처리할 수 있는 신고 유형을 반환합니다.</p>
     *
     * @return ReportType 지원하는 신고 유형
     * @author Jaeik
     * @since 2.0.0
     */
    ReportType supports();
}
