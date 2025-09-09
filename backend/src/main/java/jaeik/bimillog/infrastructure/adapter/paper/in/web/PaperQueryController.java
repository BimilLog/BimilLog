package jaeik.bimillog.infrastructure.adapter.paper.in.web;

import jaeik.bimillog.domain.paper.application.port.in.PaperQueryUseCase;
import jaeik.bimillog.domain.paper.entity.MessageDetail;
import jaeik.bimillog.domain.paper.entity.VisitMessageDetail;
import jaeik.bimillog.infrastructure.adapter.paper.dto.MessageDTO;
import jaeik.bimillog.infrastructure.adapter.paper.dto.VisitMessageDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <h2>롤링페이퍼 Query 컨트롤러</h2>
 * <p>
 * 헥사고날 아키텍처에서 롤링페이퍼 도메인의 읽기 작업을 처리하는 REST API 어댑터입니다.
 * </p>
 * <p>
 * 클라이언트로부터 롤링페이퍼 메시지 조회 요청을 받아 도메인 계층의 PaperQueryUseCase로 전달하며,
 * CQRS 패턴에서 Query 책임을 담당합니다.
 * </p>
 * <p>
 * 처리하는 HTTP 요청: GET /api/paper, GET /api/paper/{userName}
 * </p>
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
     * <p>AES-256으로 암호화된 메시지 내용을 복호화하여 반환하며, 최신 작성일 순으로 정렬됩니다.</p>
     * <p>그리드 레이아웃 정보(위치, 크기)와 장식 정보를 포함하여 화면 구성에 필요한 모든 데이터를 제공합니다.</p>
     * <p>프론트엔드의 내 롤링페이퍼 페이지에서 호출되며, 실시간 메시지 표시를 위해 사용됩니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보 (롤링페이퍼 소유자 식별용)
     * @return HTTP 응답 엔티티 (내 롤링페이퍼 메시지 목록 DTO)
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping
    public ResponseEntity<List<MessageDTO>> myPaper(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        List<MessageDetail> messageDetails = paperQueryUseCase.getMyPaper(userId);
        List<MessageDTO> messageDTOs = messageDetails.stream()
                .map(MessageDTO::from)
                .toList();
        return ResponseEntity.ok(messageDTOs);
    }

    /**
     * <h3>다른 사용자 롤링페이퍼 방문 API</h3>
     * <p>특정 사용자명으로 다른 사용자의 롤링페이퍼를 방문하여 메시지 목록을 조회합니다.</p>
     * <p>방문자에게는 메시지 내용과 익명 작성자명을 제외한 그리드 레이아웃 정보만 제공하여 프라이버시를 보호합니다.</p>
     * <p>익명 사용자도 접근 가능하며, 방문 기록을 통해 최대 5개 페이퍼, 30일간 기록이 유지됩니다.</p>
     * <p>프론트엔드의 롤링페이퍼 방문 페이지에서 호출되며, 메시지 작성을 위한 그리드 레이아웃 표시에 사용됩니다.</p>
     *
     * @param userName 방문할 롤링페이퍼 소유자의 사용자명
     * @return HTTP 응답 엔티티 (방문 가능한 메시지 정보 목록 DTO)
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("{userName}")
    public ResponseEntity<List<VisitMessageDTO>> visitPaper(@PathVariable String userName) {
        List<VisitMessageDetail> visitMessageDetails = paperQueryUseCase.visitPaper(userName);
        List<VisitMessageDTO> visitMessageDTOs = visitMessageDetails.stream()
                .map(VisitMessageDTO::from)
                .toList();
        return ResponseEntity.ok(visitMessageDTOs);
    }
}