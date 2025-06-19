package jaeik.growfarm.controller;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.paper.PaperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h2>롤링 페이퍼 API</h2>
 * <p>내 롤링 페이퍼 조회</p>
 * <p>다른 사람 롤링 페이퍼 조회</p>
 * <p>메시지 삭제</p>
 * <p>메시지 적기</p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/paper")
public class PaperController {

    private final PaperService paperService;

    /**
     * <h3>내 롤링페이퍼 조회 API</h3>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return ResponseEntity<List < MessageDTO>> 내 롤링 페이퍼 메시지 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    @GetMapping
    public ResponseEntity<List<MessageDTO>> myPaper(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<MessageDTO> messageDTOs = paperService.myPaper(userDetails);
        return ResponseEntity.ok(messageDTOs);
    }

    /**
     * <h3>다른 롤링페이퍼 방문 API</h3>
     *
     * @param userName 닉네임
     * @return ResponseEntity<List < VisitMessageDTO>> 방문한 롤링페이퍼의 메시지 목록
     * @author Jaeik
     * @since 1.0.0
     */
    @GetMapping("{userName}")
    public ResponseEntity<List<VisitMessageDTO>> visitPaper(@PathVariable String userName) {
        List<VisitMessageDTO> visitMessageDTOs = paperService.visitPaper(userName);
        return ResponseEntity.ok(visitMessageDTOs);
    }

    /**
     * <h3>메시지 작성 API</h3>
     *
     * @param userName   롤링페이퍼 주인의 닉네임
     * @param messageDTO 작성할 메시지 정보
     * @return ResponseEntity<String> 메시지 작성 완료 메시지
     * @author Jaeik
     * @since 1.0.0
     */
    @PostMapping("{userName}")
    public ResponseEntity<String> plantCrop(
            @PathVariable String userName,
            @RequestBody @Valid MessageDTO messageDTO) {
        paperService.writeMessage(userName, messageDTO);
        return ResponseEntity.ok("메시지가 작성되었습니다.");
    }


    /**
     * <h3>메시지 삭제 API</h3>
     * @param userDetails 현재 로그인한 사용자 정보
     * @param messageDTO 삭제할 메시지 정보
     * @return ResponseEntity<String> 농작물 삭제 완료 메시지
     * @author Jaeik
     * @since 1.0.0
     */
    @PostMapping("/delete")
    public ResponseEntity<String> deleteCrop(@AuthenticationPrincipal CustomUserDetails userDetails,
                                             @RequestBody@Valid MessageDTO messageDTO) {
        paperService.deleteMessageInMyPaper(userDetails, messageDTO);
        return ResponseEntity.ok("메시지가 삭제되었습니다.");
    }
}
