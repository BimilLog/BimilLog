package jaeik.growfarm.controller;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.PaperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/*
 * 농장 관련 API
 * 내 농장 가기
 * 다른 농장 가기
 * 농작물 삭제
 * 농작물 심기
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/farm")
public class PaperController {

    private final PaperService paperService;

    /*
     * 내 농장 가기 API
     * param CustomUserDetails userDetails: 현재 로그인 한 유저 정보
     * return: ResponseEntity<List<CropDTO>> 내 농장에 심어진 농작물 리스트
     * 수정일 : 2025-05-03
     */
    @PostMapping("/myFarm")
    public ResponseEntity<List<MessageDTO>> myFarm(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<MessageDTO> crops = paperService.myFarm(userDetails.getClientDTO().getUserId());
        return ResponseEntity.ok(crops);
    }

    /*
     * 다른 농장 가기 API
     * param String userName: 농장 이름
     * return: ResponseEntity<List<VisitCropDTO>> 다른 농장에 심어진 농작물 리스트
     * 수정일 : 2025-05-03
     */
    @GetMapping("{farmName}")
    public ResponseEntity<List<VisitMessageDTO>> visitFarm(@PathVariable String farmName) {
        List<VisitMessageDTO> crops = paperService.visitFarm(farmName);
        return ResponseEntity.ok(crops);
    }

    /*
     * 농작물 삭제 API
     * param Long cropId: 농작물 ID
     * return: ResponseEntity<String> 농작물 삭제 완료 메시지
     * 수정일 : 2025-05-03
     */
    @PostMapping("/myFarm/{cropId}")
    public ResponseEntity<String> deleteCrop(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long cropId) {
        paperService.deleteCrop(userDetails, cropId);
        return ResponseEntity.ok("농작물이 삭제되었습니다.");
    }

    /*
     * 농작물 심기 API
     * param String userName: 농장 이름
     * param CropDTO cropDTO: 농작물 DTO
     * return: ResponseEntity<String> 농작물 심기 완료 메시지
     * 수정일 : 2025-05-03
     */
    @PostMapping("{farmName}")
    public ResponseEntity<String> plantCrop(
            @PathVariable String farmName,
            @RequestBody @Valid MessageDTO messageDTO) throws IOException {
        paperService.plantCrop(farmName, messageDTO);
        return ResponseEntity.ok("농작물이 심어졌습니다.");
    }
}
