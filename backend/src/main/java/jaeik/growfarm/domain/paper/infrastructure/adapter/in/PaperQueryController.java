package jaeik.growfarm.domain.paper.infrastructure.adapter.in;

import jaeik.growfarm.domain.paper.application.port.in.ReadPaperUseCase;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <h2>롤링 페이퍼 조회 컨트롤러 V2 (헥사고날)</h2>
 * <p>
 * Primary Adapter: 롤링 페이퍼 조회 관련 웹 API를 관리합니다.
 * 헥사고날 아키텍처 적용 - UseCase 기반 구현
 * 기존 PaperQueryController와 동일한 API 스펙 보장
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/paper")
public class PaperQueryController {

    private final ReadPaperUseCase readPaperUseCase;

    /**
     * <h3>내 롤링페이퍼 조회 API</h3>
     * <p>기존 PaperQueryController.myPaper()와 동일한 기능</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return ResponseEntity<List < MessageDTO>> 내 롤링 페이퍼 메시지 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping
    public ResponseEntity<List<MessageDTO>> myPaper(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<MessageDTO> messageDTOs = readPaperUseCase.getMyPaper(userDetails);
        return ResponseEntity.ok(messageDTOs);
    }

    /**
     * <h3>다른 롤링페이퍼 방문 API</h3>
     * <p>기존 PaperQueryController.visitPaper()와 동일한 기능</p>
     *
     * @param userName 닉네임
     * @return ResponseEntity<List < VisitMessageDTO>> 방문한 롤링페이퍼의 메시지 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("{userName}")
    public ResponseEntity<List<VisitMessageDTO>> visitPaper(@PathVariable String userName) {
        List<VisitMessageDTO> visitMessageDTOs = readPaperUseCase.visitPaper(userName);
        return ResponseEntity.ok(visitMessageDTOs);
    }
}