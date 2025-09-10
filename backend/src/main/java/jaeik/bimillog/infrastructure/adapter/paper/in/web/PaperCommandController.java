package jaeik.bimillog.infrastructure.adapter.paper.in.web;

import jaeik.bimillog.domain.paper.application.port.in.PaperCommandUseCase;
import jaeik.bimillog.domain.paper.application.service.PaperCommandService;
import jaeik.bimillog.infrastructure.adapter.paper.dto.MessageDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
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
     * <p>클라이언트에서 전송한 메시지 작성 POST 요청을 처리합니다.</p>
     * <p>익명 사용자도 메시지 작성이 가능하며, 그리드 레이아웃에 맞는 위치와 크기를 지정할 수 있습니다.</p>
     * <p>검증된 요청 데이터를 {@link PaperCommandService}에 전달하여 AES-256 암호화된 메시지를 저장합니다.</p>
     *
     * @param userName   방문하여 메시지를 작성할 롤링페이퍼 주인의 사용자명
     * @param messageDTO 작성할 메시지 정보 (장식 타입, 익명명, 내용, 위치, 크기)
     * @return HTTP 응답 엔티티 (메시지 작성 완료 메시지)
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
     * <h3>내 롤링페이퍼 메시지 삭제 API</h3>
     * <p>클라이언트에서 전송한 메시지 삭제 POST 요청을 처리합니다.</p>
     * <p>로그인한 사용자만 접근 가능하며, 자신의 롤링페이퍼에 있는 메시지만 삭제할 수 있습니다.</p>
     * <p>메시지 소유권 검증을 통해 삭제 권한을 확인하고, 검증된 요청을 {@link PaperCommandService}로 전달합니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보 (롤링페이퍼 소유자 확인용)
     * @param messageDTO  삭제할 메시지 정보 (메시지 ID 포함)
     * @return HTTP 응답 엔티티 (메시지 삭제 완료 메시지)
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