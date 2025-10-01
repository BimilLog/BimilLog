package jaeik.bimillog.infrastructure.adapter.in.paper.web;

import jaeik.bimillog.domain.paper.application.port.in.PaperQueryUseCase;
import jaeik.bimillog.domain.paper.entity.MessageDetail;
import jaeik.bimillog.domain.paper.entity.VisitMessageDetail;
import jaeik.bimillog.infrastructure.adapter.in.paper.dto.MessageDTO;
import jaeik.bimillog.infrastructure.adapter.in.paper.dto.VisitMessageDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <h2>롤링페이퍼 조회 컨트롤러</h2>
 * <p>롤링페이퍼 도메인의 조회 작업을 처리하는 REST API 컨트롤러입니다.</p>
 * <p>내 롤링페이퍼 조회, 타인 롤링페이퍼 방문</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/paper")
public class PaperQueryController {

    private final PaperQueryUseCase paperQueryUseCase;

    /**
     * <h3>내 롤링페이퍼 조회 API</h3>
     * <p>로그인한 사용자의 롤링페이퍼에 작성된 모든 메시지를 조회합니다.</p>
     * <p>메시지 내용과 그리드 레이아웃 정보를 최신 작성일 순으로 반환합니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return HTTP 응답 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping
    public ResponseEntity<List<MessageDTO>> myPaper(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        List<MessageDetail> messageDetails = paperQueryUseCase.getMyPaper(memberId);
        List<MessageDTO> messageDTOs = messageDetails.stream()
                .map(MessageDTO::from)
                .toList();
        return ResponseEntity.ok(messageDTOs);
    }

    /**
     * <h3>다른 사용자 롤링페이퍼 방문 API</h3>
     * <p>특정 사용자명으로 다른 사용자의 롤링페이퍼를 방문하여 메시지 목록을 조회합니다.</p>
     * <p>방문자에게는 메시지 내용과 익명 작성자명을 제외한 그리드 레이아웃 정보만 제공합니다.</p>
     *
     * @param memberName 방문할 롤링페이퍼 소유자의 사용자명
     * @return HTTP 응답 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/{memberName}")
    public ResponseEntity<List<VisitMessageDTO>> visitPaper(@PathVariable String memberName) {
        List<VisitMessageDetail> visitMessageDetails = paperQueryUseCase.visitPaper(memberName);
        List<VisitMessageDTO> visitMessageDTOs = visitMessageDetails.stream()
                .map(VisitMessageDTO::from)
                .toList();
        return ResponseEntity.ok(visitMessageDTOs);
    }
}