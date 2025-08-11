package jaeik.growfarm.domain.paper.infrastructure.adapter.in;

import jaeik.growfarm.domain.paper.application.port.in.DeletePaperUseCase;
import jaeik.growfarm.domain.paper.application.port.in.WritePaperUseCase;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>롤링 페이퍼 변경 컨트롤러 V2 (헥사고날)</h2>
 * <p>
 * Primary Adapter: 롤링 페이퍼 변경 관련 웹 API를 관리합니다.
 * 헥사고날 아키텍처 적용 - UseCase 기반 구현
 * 기존 PaperCommandController와 동일한 API 스펙 보장
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/paper/v2")
public class PaperCommandController {

    private final WritePaperUseCase writePaperUseCase;
    private final DeletePaperUseCase deletePaperUseCase;

    /**
     * <h3>메시지 작성 API</h3>
     * <p>기존 PaperCommandController.writeMessage()와 동일한 기능</p>
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
        writePaperUseCase.writeMessage(userName, messageDTO);
        return ResponseEntity.ok("메시지가 작성되었습니다.");
    }

    /**
     * <h3>메시지 삭제 API</h3>
     * <p>기존 PaperCommandController.deleteMessage()와 동일한 기능</p>
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
        deletePaperUseCase.deleteMessageInMyPaper(userDetails, messageDTO);
        return ResponseEntity.ok("메시지가 삭제되었습니다.");
    }
}