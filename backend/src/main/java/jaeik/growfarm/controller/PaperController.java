package jaeik.growfarm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * <h2>롤링 페이퍼 컨트롤러</h2>
 * <p>
 * 내 롤링 페이퍼 조회
 * </p>
 * <p>
 * 다른 사람 롤링 페이퍼 조회
 * </p>
 * <p>
 * 메시지 삭제
 * </p>
 * <p>
 * 메시지 적기
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Tag(name = "롤링페이퍼", description = "롤링페이퍼 관련 API")
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
    @Operation(summary = "내 롤링페이퍼 조회", description = "현재 로그인한 사용자의 롤링페이퍼 메시지 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "롤링페이퍼 조회 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.")
    })
    @GetMapping
    public ResponseEntity<List<MessageDTO>> myPaper(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails) {
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
    @Operation(summary = "다른 사용자의 롤링페이퍼 조회", description = "지정된 사용자의 롤링페이퍼 메시지 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "롤링페이퍼 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 닉네임의 롤링페이퍼를 찾을 수 없습니다.")
    })
    @GetMapping("{userName}")
    public ResponseEntity<List<VisitMessageDTO>> visitPaper(
            @Parameter(description = "조회할 사용자의 닉네임") @PathVariable String userName) {
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
    @Operation(summary = "롤링페이퍼 메시지 작성", description = "지정된 사용자의 롤링페이퍼에 새로운 메시지를 작성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메시지 작성 성공"),
            @ApiResponse(responseCode = "404", description = "해당 닉네임의 롤링페이퍼를 찾을 수 없습니다.")
    })
    @PostMapping("{userName}")
    public ResponseEntity<String> writeMessage(
            @Parameter(description = "메시지를 작성할 대상 사용자의 닉네임") @PathVariable String userName,
            @Parameter(description = "작성할 메시지 정보") @RequestBody @Valid MessageDTO messageDTO) {
        paperService.writeMessage(userName, messageDTO);
        return ResponseEntity.ok("메시지가 작성되었습니다.");
    }

    /**
     * <h3>메시지 삭제 API</h3>
     * 
     * @param userDetails 현재 로그인한 사용자 정보
     * @param messageDTO  삭제할 메시지 정보
     * @return ResponseEntity<String> 농작물 삭제 완료 메시지
     * @author Jaeik
     * @since 1.0.0
     */
    @Operation(summary = "롤링페이퍼 메시지 삭제", description = "자신의 롤링페이퍼에 있는 메시지를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메시지 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "403", description = "본인 롤링페이퍼의 메시지만 삭제할 수 있습니다.")
    })
    @PostMapping("/delete")
    public ResponseEntity<String> deleteMessage(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "삭제할 메시지 정보") @RequestBody @Valid MessageDTO messageDTO) {
        paperService.deleteMessageInMyPaper(userDetails, messageDTO);
        return ResponseEntity.ok("메시지가 삭제되었습니다.");
    }
}
