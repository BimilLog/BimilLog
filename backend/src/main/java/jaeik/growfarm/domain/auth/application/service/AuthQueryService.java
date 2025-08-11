package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.AuthQueryUseCase;
import jaeik.growfarm.domain.auth.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthQueryService implements AuthQueryUseCase {

    private final LoadUserPort loadUserPort;

    @Override
    public User getUserFromUserDetails(CustomUserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return loadUserPort.findById(userDetails.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}