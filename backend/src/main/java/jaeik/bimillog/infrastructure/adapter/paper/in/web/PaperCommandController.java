package jaeik.bimillog.infrastructure.adapter.paper.in.web;

import jaeik.bimillog.domain.paper.application.port.in.PaperCommandUseCase;
import jaeik.bimillog.infrastructure.adapter.paper.in.web.dto.MessageDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>롤링 페이퍼 변경 컨트롤러</h2>
 * <p>
 * 롤링페이퍼 메시지 작성 및 삭제 관련 웹 API를 관리합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/paper")
public class PaperCommandController {

    private final PaperCommandUseCase paperCommandUseCase;

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
        paperCommandUseCase.writeMessage(userName, messageDTO.getDecoType(), 
                messageDTO.getAnonymity(), messageDTO.getContent(), 
                messageDTO.getWidth(), messageDTO.getHeight());
        return ResponseEntity.ok("메시지가 작성되었습니다.");
    }

    /**
     * <h3>메시지 삭제 API</h3>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param messageDTO  삭제할 메시지 정보
     * @return ResponseEntity<String> 메시지 삭제 완료 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/delete")
    public ResponseEntity<String> deleteMessage(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                @RequestBody @Valid MessageDTO messageDTO) {
        Long userId = userDetails.getUserId();
        paperCommandUseCase.deleteMessageInMyPaper(userId, messageDTO.getId());
        return ResponseEntity.ok("메시지가 삭제되었습니다.");
    }
}