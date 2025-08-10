package jaeik.growfarm.controller.paper;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.paper.delete.PaperDeleteService;
import jaeik.growfarm.service.paper.write.PaperWriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>롤링 페이퍼 변경 컨트롤러</h2>
 * <p>
 * 롤링 페이퍼 변경 관련 API를 관리합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/paper")
public class PaperCommandController {

    private final PaperWriteService paperWriteService;
    private final PaperDeleteService paperDeleteService;

    /**
     * <h3>메시지 작성 API</h3>
     *
     * @param userName   롤링페이퍼 주인의 닉네임
     * @param messageDTO 작성할 메시지 정보
     * @return ResponseEntity<String> 메시지 작성 완료 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("{userName}")
    public ResponseEntity<String> writeMessage(
            @PathVariable String userName,
            @RequestBody @Valid MessageDTO messageDTO) {
        paperWriteService.writeMessage(userName, messageDTO);
        return ResponseEntity.ok("메시지가 작성되었습니다.");
    }

    /**
     * <h3>메시지 삭제 API</h3>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param messageDTO  삭제할 메시지 정보
     * @return ResponseEntity<String> 농작물 삭제 완료 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/delete")
    public ResponseEntity<String> deleteMessage(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                @RequestBody @Valid MessageDTO messageDTO) {
        paperDeleteService.deleteMessageInMyPaper(userDetails, messageDTO);
        return ResponseEntity.ok("메시지가 삭제되었습니다.");
    }
}
