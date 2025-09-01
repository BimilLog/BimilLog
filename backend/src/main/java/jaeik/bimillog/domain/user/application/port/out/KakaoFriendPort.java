package jaeik.bimillog.domain.user.application.port.out;

import jaeik.bimillog.domain.user.entity.KakaoFriendsResponseVO;
import reactor.core.publisher.Mono;

/**
 * <h2>카카오 친구 목록 조회 포트</h2>
 * <p>카카오 API를 통한 친구 목록 조회 기능을 정의하는 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface KakaoFriendPort {

    /**
     * <h3>카카오 친구 목록 조회</h3>
     * <p>카카오 액세스 토큰을 사용하여 친구 목록을 비동기로 조회합니다.</p>
     *
     * @param accessToken 카카오 액세스 토큰
     * @param offset      조회 시작 위치 (기본값: 0)
     * @param limit       조회할 친구 수 (기본값: 10, 최대: 100)
     * @return Mono<KakaoFriendsResponseVO> 카카오 친구 목록 응답 (비동기)
     * @since 2.0.0
     * @author Jaeik
     */
    Mono<KakaoFriendsResponseVO> getFriendList(String accessToken, Integer offset, Integer limit);
}