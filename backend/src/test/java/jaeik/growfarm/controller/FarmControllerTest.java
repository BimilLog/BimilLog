package jaeik.growfarm.controller;

import jaeik.growfarm.dto.farm.CropDTO;
import jaeik.growfarm.dto.farm.VisitCropDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.FarmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
public class FarmControllerTest {

    @Mock
    private FarmService farmService;

    @InjectMocks
    private FarmController farmController;

    private CustomUserDetails userDetails;
    private CropDTO cropDTO;
    private List<CropDTO> cropDTOList;
    private List<VisitCropDTO> visitCropDTOList;

    @BeforeEach
    void setUp() {
        // Setup mock data
        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getClientDTO()).thenReturn(mock(jaeik.growfarm.dto.user.UserDTO.class));
        when(userDetails.getClientDTO().getUserId()).thenReturn(1L);

        cropDTO = new CropDTO();
        // Set properties for cropDTO if needed

        cropDTOList = new ArrayList<>();
        cropDTOList.add(cropDTO);

        VisitCropDTO visitCropDTO = new VisitCropDTO();
        // Set properties for visitCropDTO if needed

        visitCropDTOList = new ArrayList<>();
        visitCropDTOList.add(visitCropDTO);
    }

    @Test
    @DisplayName("내 농장 가기 테스트")
    void testMyFarm() {
        // Given
        when(farmService.myFarm(anyLong())).thenReturn(cropDTOList);

        // When
        ResponseEntity<List<CropDTO>> response = farmController.myFarm(userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("다른 농장 가기 테스트")
    void testVisitFarm() {
        // Given
        when(farmService.visitFarm(anyString())).thenReturn(visitCropDTOList);

        // When
        ResponseEntity<List<VisitCropDTO>> response = farmController.visitFarm("testFarm");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("농작물 삭제 테스트")
    void testDeleteCrop() {
        // Given
        doNothing().when(farmService).deleteCrop(any(), anyLong());

        // When
        ResponseEntity<String> response = farmController.deleteCrop(userDetails, 1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("농작물이 삭제되었습니다.", response.getBody());
        verify(farmService, times(1)).deleteCrop(userDetails, 1L);
    }

    @Test
    @DisplayName("농작물 심기 테스트")
    void testPlantCrop() throws IOException {
        // Given
        doNothing().when(farmService).plantCrop(anyString(), any());

        // When
        ResponseEntity<String> response = farmController.plantCrop("testFarm", cropDTO);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("농작물이 심어졌습니다.", response.getBody());
        verify(farmService, times(1)).plantCrop("testFarm", cropDTO);
    }
}
