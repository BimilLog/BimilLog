package jaeik.bimillog.domain.member.controller;

import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.member.dto.BlacklistDTO;
import jaeik.bimillog.domain.member.service.MemberBlacklistService;
import jaeik.bimillog.infrastructure.log.Log;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 블랙리스트 컨트롤러
 */
@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        logParams = false)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberBlackController {
    private final MemberBlacklistService memberBlacklistService;

    /**
     * 자신의 블랙리스트 목록 조회
     * @param userDetails 인증된 사용자 정보
     * @param pageable 페이징 정보
     * @return Page<BlacklistDTO> 블랙리스트 목록 (id, memberName, createdAt 포함)
     */
    @GetMapping("/blacklist")
    public ResponseEntity<Page<BlacklistDTO>> getBlacklist(@AuthenticationPrincipal CustomUserDetails userDetails, Pageable pageable) {
        Page<BlacklistDTO> myBlacklist = memberBlacklistService.getInterActionBlacklist(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(myBlacklist);
    }

    /**
     * 특정사람을 블랙리스트에 추가
     * @param userDetails 인증된 사용자 정보
     * @param blacklistDTO 블랙리스트 추가 정보 (memberName 필수)
     * @return 성공 응답
     */
    @PostMapping("/blacklist")
    public ResponseEntity<String> addBlacklist(@AuthenticationPrincipal CustomUserDetails userDetails,
                                               @Valid @RequestBody BlacklistDTO blacklistDTO) {
        memberBlacklistService.addMyBlacklist(userDetails.getMemberId(), blacklistDTO.getMemberName());
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 사람을 블랙리스트에서 삭제
     * @param id 블랙리스트 ID (Path Variable)
     * @param userDetails 인증된 사용자 정보
     * @param pageable 페이징 정보
     * @return Page<BlacklistDTO> 삭제 후 블랙리스트 목록
     */
    @DeleteMapping("/blacklist/{id}")
    public ResponseEntity<Page<BlacklistDTO>> deleteMemberFromBlacklist(@PathVariable Long id,
                                                                        @AuthenticationPrincipal CustomUserDetails userDetails,
                                                                        Pageable pageable) {
        memberBlacklistService.deleteMemberFromMyBlacklist(id, userDetails.getMemberId());
        Page<BlacklistDTO> myBlacklist = memberBlacklistService.getInterActionBlacklist(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(myBlacklist);
    }
}
