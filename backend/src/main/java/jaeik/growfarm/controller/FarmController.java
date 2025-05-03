package jaeik.growfarm.controller;

import jaeik.growfarm.dto.farm.CropDTO;
import jaeik.growfarm.dto.farm.VisitCropDTO;
import jaeik.growfarm.global.jwt.CustomUserDetails;
import jaeik.growfarm.service.FarmService;
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
@RequestMapping("/api/farm")
public class FarmController {

    private final FarmService farmService;

    /*
     * 내 농장 가기 API
     * param CustomUserDetails userDetails: 현재 로그인 한 유저 정보
     * return: ResponseEntity<List<CropDTO>> 내 농장에 심어진 농작물 리스트
     * 수정일 : 2025-05-03
     */
    @PostMapping("/myFarm")
    public ResponseEntity<List<CropDTO>> myFarm(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<CropDTO> crops = farmService.myFarm(userDetails.getUserDTO().getUserId());
        return ResponseEntity.ok(crops);
    }

    /*
     * 다른 농장 가기 API
     * param String farmName: 농장 이름
     * return: ResponseEntity<List<VisitCropDTO>> 다른 농장에 심어진 농작물 리스트
     * 수정일 : 2025-05-03
     */
    @GetMapping("{farmName}")
    public ResponseEntity<List<VisitCropDTO>> visitFarm(@PathVariable String farmName) {
        List<VisitCropDTO> crops = farmService.visitFarm(farmName);
        return ResponseEntity.ok(crops);
    }

    /*
     * 농작물 삭제 API
     * param Long cropId: 농작물 ID
     * return: ResponseEntity<String> 농작물 삭제 완료 메시지
     * 수정일 : 2025-05-03
     */
    @PostMapping("/myFarm/{cropId}")
    public ResponseEntity<String> deleteCrop(@PathVariable Long cropId) {
        farmService.deleteCrop(cropId);
        return ResponseEntity.ok("농작물이 삭제되었습니다.");
    }

    /*
     * 농작물 심기 API
     * param String farmName: 농장 이름
     * param CropDTO cropDTO: 농작물 DTO
     * return: ResponseEntity<String> 농작물 심기 완료 메시지
     * 수정일 : 2025-05-03
     */
    @PostMapping("{farmName}")
    public ResponseEntity<String> plantCrop(@PathVariable String farmName,
            @RequestBody CropDTO cropDTO) throws IOException {
        farmService.plantCrop(farmName, cropDTO);
        return ResponseEntity.ok("농작물이 심어졌습니다.");
    }
}
