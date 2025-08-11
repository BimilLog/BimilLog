package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.AuthQueryUseCase;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <h2>인증 조회 서비스</h2>
 * <p>사용자 정보 조회 관련 비즈니스 로직 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class AuthQueryService implements AuthQueryUseCase {

    @Override
    public ClientDTO getCurrentUser(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.NULL_SECURITY_CONTEXT);
        }
        return userDetails.getClientDTO();
    }
}