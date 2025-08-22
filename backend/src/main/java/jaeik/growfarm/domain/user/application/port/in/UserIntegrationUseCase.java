package jaeik.growfarm.domain.user.application.port.in;

import jaeik.growfarm.infrastructure.adapter.user.in.web.dto.KakaoFriendsResponse;

/**
 * <h2>사용자 통합 기능 UseCase</h2>
 * <p>사용자와 관련된 외부 API 통합 기능을 정의하는 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface UserIntegrationUseCase {

    /**
     * <h3>카카오 친구 목록 조회</h3>
     * <p>현재 로그인한 사용자의 카카오 친구 목록을 조회합니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 현재 요청 기기의 토큰 ID (UserDetails에서 추출)
     * @param offset 조회 시작 위치 (기본값: 0)
     * @param limit  조회할 친구 수 (기본값: 10, 최대: 100)
     * @return 카카오 친구 목록 응답
     * @since 2.0.0
     * @author Jaeik
     */
    KakaoFriendsResponse getKakaoFriendList(Long userId, Long tokenId, Integer offset, Integer limit);
}