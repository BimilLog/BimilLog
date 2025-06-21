package jaeik.growfarm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jaeik.growfarm.dto.auth.LoginResponseDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.service.auth.AuthService;
import jaeik.growfarm.service.auth.UserUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h2>인증 관련 컨트롤러</h2>
 * <p>
 * 카카오 로그인 처리
 * </p>
 * <p>
 * 회원가입 처리
 * </p>
 * <p>
 * 로그아웃 처리
 * </p>
 * <p>
 * 회원 탈퇴 처리
 * </p>
 * <p>
 * 현재 로그인한 유저 정보 조회
 * </p>
 * <p>
 * AWS 로드 밸런서 상태 검사
 * </p>
 * 
 * @author Jaeik
 * @version 1.0.0
 */
@Tag(name = "인증", description = "인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserUpdateService userUpdateService;

    /**
     * <h3>카카오 로그인 API</h3>
     *
     * <p>
     * 기존 회원은 JWT를 담은 쿠키를 반환하고, 신규 회원은 UUID가 있는 임시 쿠키를 반환한다.
     * </p>
     *
     * @since 1.0.0
     * @author Jaeik
     * @param code     프론트에서 반환된 카카오 인가 코드
     * @param fcmToken (선택) FCM 토큰, 푸시 알림을 위한 토큰
     * @return Jwt가 삽입된 쿠키 또는 임시 쿠키
     */
    @Operation(summary = "카카오 로그인", description = "카카오 인가 코드를 받아 로그인 또는 임시 회원 데이터를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공 또는 임시 쿠키 발급 완료"),
            @ApiResponse(responseCode = "403", description = "차단된 회원"),
            @ApiResponse(responseCode = "500", description = "카카오 유저 정보 조회 실패")
    })
    @GetMapping("/login")
    public ResponseEntity<?> loginKakao(
            @Parameter(description = "카카오 인가 코드") @RequestParam String code,
            @Parameter(description = "FCM 토큰 (선택사항)") @RequestParam(required = false) String fcmToken) {
        LoginResponseDTO<?> result = authService.processKakaoLogin(code, fcmToken);

        if (result.getType() == LoginResponseDTO.LoginType.EXISTING_USER) {
            List<ResponseCookie> cookies = (List<ResponseCookie>) result.getData();
            return ResponseEntity.ok()
                    .header("Set-Cookie", cookies.get(0).toString())
                    .header("Set-Cookie", cookies.get(1).toString())
                    .body("로그인 성공");
        } else {
            ResponseCookie tempCookie = (ResponseCookie) result.getData();
            return ResponseEntity.ok()
                    .header("Set-Cookie", tempCookie.toString())
                    .body("임시 쿠키 발급 완료");
        }
    }

    /**
     * <h3>서비스 회원 가입 API</h3>
     *
     * <p>
     * 카카오 로그인 후 신규 회원일 경우 작동되며 userName과 uuid를 받아서 Jwt 쿠키를 반환한다.
     * </p>
     *
     * @param userName 닉네임
     * @param uuid     임시 쿠키에 저장된 UUID
     * @return JWT가 삽입된 쿠키
     * @author Jaeik
     * @since 1.0.0
     */
    @Operation(summary = "회원가입", description = "카카오 로그인 후 임시 쿠키와 닉네임을 받아 정식 회원으로 가입합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "이미 존재하는 닉네임 또는 유효하지 않은 임시 데이터"),
    })
    @GetMapping("/signUp")
    public ResponseEntity<String> SignUp(
            @Parameter(description = "사용할 닉네임") @RequestParam String userName,
            @Parameter(description = "로그인 시 발급받은 임시 쿠키 값") @CookieValue(value = "temp") String uuid) {

        List<ResponseCookie> cookies = authService.signUp(userName, uuid);

        return ResponseEntity.ok()
                .header("Set-Cookie", cookies.get(0).toString())
                .header("Set-Cookie", cookies.get(1).toString())
                .body("회원 가입 완료 - JWT 쿠키 발급됨");
    }

    /**
     * <h3>로그아웃 API</h3>
     *
     * <p>
     * 카카오 서버와 통신하여 카카오 로그아웃
     * </p>
     * <p>
     * 사용자 토큰 삭제
     * </p>
     * <p>
     * 쿠키를 삭제하여 자체 서비스 로그아웃
     * </p>
     *
     * @since 1.0.0
     * @author Jaeik
     * @param userDetails 인증된 사용자 정보
     * @return 로그아웃 쿠키
     */
    @Operation(summary = "로그아웃", description = "서버에서 사용자의 토큰을 만료시키고 로그아웃 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "500", description = "로그아웃 실패")
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.kakaoLogout(userDetails);
        userUpdateService.logoutUser(userDetails.getUserId());
        List<ResponseCookie> cookies = authService.logout(userDetails);
        return ResponseEntity.ok()
                .header("Set-Cookie", cookies.get(0).toString())
                .header("Set-Cookie", cookies.get(1).toString())
                .body("로그아웃 완료");
    }

    /**
     * <h3>회원 탈퇴 API</h3>
     * <p>
     * 쿠키를 삭제하고 관련 유저정보를 삭제하여 자체 서비스 회원 탈퇴
     * </p>
     * <p>
     * 카카오 서버와 통신하여 카카오 연결끊기
     * </p>
     *
     * @since 1.0.0
     * @author Jaeik
     * @param userDetails 인증된 사용자 정보
     * @return 로그아웃 쿠키
     */
    @Operation(summary = "회원 탈퇴", description = "서비스에서 회원 정보를 삭제하고 연결된 카카오 계정의 연결을 끊습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "500", description = "회원탈퇴 실패")
    })
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ResponseCookie> cookies = authService.withdraw(userDetails);
        return ResponseEntity.ok()
                .header("Set-Cookie", cookies.get(0).toString())
                .header("Set-Cookie", cookies.get(1).toString())
                .body("회원탈퇴 완료");
    }

    /**
     * <h3>현재 로그인한 사용자 정보 조회 API</h3>
     *
     * <p>
     * 현재 로그인한 사용자의 정보를 조회하여 반환한다.
     * </p>
     *
     * @since 1.0.0
     * @author Jaeik
     * @param userDetails 인증된 사용자 정보
     * @return 현재 로그인한 사용자 정보
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다.")
    })
    @PostMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.NULL_SECURITY_CONTEXT);
        }
        return ResponseEntity.ok(userDetails.getClientDTO());
    }

    /**
     * <h3>서버 상태 검사 API</h3>
     *
     * @since 2025-04-28
     * @author Jaeik
     * @return 상태 검사 완료 메시지
     */
    @Operation(summary = "서버 상태 확인", description = "서버의 상태를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "서버 정상 동작 중")
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Server is running");
    }}

    
            