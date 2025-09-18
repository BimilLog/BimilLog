package jaeik.bimillog.infrastructure.adapter.paper.in.web;

import jaeik.bimillog.domain.paper.application.port.in.PaperCommandUseCase;
import jaeik.bimillog.infrastructure.adapter.paper.dto.MessageDTO;
import jaeik.bimillog.infrastructure.adapter.auth.out.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>롤링페이퍼 명령 컨트롤러</h2>
 * <p>롤링페이퍼 도메인의 명령 작업을 처리하는 REST API 컨트롤러입니다.</p>
 * <p>메시지 작성, 메시지 삭제</p>
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
     * <h3>롤링페이퍼 메시지 작성 API</h3>
     * <p>메시지 작성 POST 요청을 처리합니다.</p>
     * <p>익명 사용자도 접근 가능하며, 그리드 레이아웃에 맞는 좌표를 지정할 수 있습니다.</p>
     *
     * @param userName   메시지를 작성할 롤링페이퍼 소유자의 사용자명
     * @param messageDTO 작성할 메시지 정보
     * @return HTTP 응답 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("{userName}")
    public ResponseEntity<String> writeMessage(
            @PathVariable String userName,
            @RequestBody @Valid MessageDTO messageDTO) {
        paperCommandUseCase.writeMessage(userName, messageDTO.getDecoType(), 
                messageDTO.getAnonymity(), messageDTO.getContent(), 
                messageDTO.getX(), messageDTO.getY());
        return ResponseEntity.ok("메시지가 작성되었습니다.");
    }

    /**
     * <h3>내 롤링페이퍼 메시지 삭제 API</h3>
     * <p>메시지 삭제 POST 요청을 처리합니다.</p>
     * <p>로그인한 사용자만 접근 가능하며, 자신의 롤링페이퍼에 있는 메시지만 삭제할 수 있습니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param messageDTO  삭제할 메시지 정보
     * @return HTTP 응답 엔티티
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