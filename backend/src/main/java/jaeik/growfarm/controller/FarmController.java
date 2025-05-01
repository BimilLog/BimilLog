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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/farm")
public class FarmController {

    private final FarmService farmService;

    // 내 농장 확인
    @PostMapping("/myFarm")
    public ResponseEntity<List<CropDTO>> myFarm(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<CropDTO> crops = farmService.myFarm(userDetails.getUserDTO().getUserId());
        return ResponseEntity.ok(crops);
    }


    // 내 농장에서 농작물 삭제
    @PostMapping("/myFarm/{cropId}")
    public ResponseEntity<String> deleteCrop(@PathVariable Long cropId) {
        farmService.deleteCrop(cropId);
        return ResponseEntity.ok("농작물이 삭제되었습니다.");
    }

    // 다른 농장 들리기
    @GetMapping("{farmName}")
    public ResponseEntity<List<VisitCropDTO>> visitFarm(@PathVariable String farmName) {
        List<VisitCropDTO> crops = farmService.visitFarm(farmName);
        return ResponseEntity.ok(crops);
    }

    // 다른 농장에 농작물 심기
    @PostMapping("{farmName}")
    public ResponseEntity<String> plantCrop(@PathVariable String farmName,
            @RequestBody CropDTO cropDTO) throws IOException {
        farmService.plantCrop(farmName, cropDTO);
        return ResponseEntity.ok("농작물이 심어졌습니다.");
    }
}
