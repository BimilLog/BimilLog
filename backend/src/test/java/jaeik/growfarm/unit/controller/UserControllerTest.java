package jaeik.growfarm.unit.controller;

import jaeik.growfarm.controller.UserController;
import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import jaeik.growfarm.dto.kakao.KakaoFriendListDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.dto.user.UserNameDTO;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private CustomUserDetails userDetails;
    private UserNameDTO userNameDTO;
    private ReportDTO reportDTO;
    private SettingDTO settingDTO;
    private KakaoFriendListDTO kakaoFriendListDTO;
    private List<SimplePostDTO> simplePostDTOList;
    private List<SimpleCommentDTO> simpleCommentDTOList;

    @BeforeEach
    void setUp() {
        // Setup mock data
        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getClientDTO()).thenReturn(mock(jaeik.growfarm.dto.user.ClientDTO.class));
        when(userDetails.getClientDTO().getUserId()).thenReturn(1L);

        // Create SimplePostDTO with required parameters
        // postId
        // userId
        // userName
        // title
        // commentCount
        // likes
        // views
        // createdAt
        // is_notice
        SimplePostDTO simplePostDTO = new SimplePostDTO(
                1L,                           // postId
                1L,                           // userId
                "testPaper",                   // userName
                "Test Title",                 // title
                0,                            // commentCount
                0,                            // likes
                0,                            // views
                Instant.now(),          // createdAt
                false                         // is_notice
        );
        // No need to set PopularFlag for this test

        simplePostDTOList = new ArrayList<>();
        simplePostDTOList.add(simplePostDTO);

        // Create SimpleCommentDTO with required parameters
        // id
        // postId
        // userName
        // content
        // createdAt
        // likes
        // userLike
        SimpleCommentDTO simpleCommentDTO = new SimpleCommentDTO(
                1L,                           // id
                1L,                           // postId
                "testPaper",                   // userName
                "Test comment content",       // content
                Instant.now(),                // createdAt
                0,                            // likes
                false                         // userLike
        );

        simpleCommentDTOList = new ArrayList<>();
        simpleCommentDTOList.add(simpleCommentDTO);

        // Create UserNameDTO
        userNameDTO = new UserNameDTO();
        userNameDTO.setUserName("newUserName");

        // Create ReportDTO
        reportDTO = ReportDTO.builder()
                .reportId(1L)
                .reportType(ReportType.IMPROVEMENT)
                .userId(1L)
                .content("Test suggestion content")
                .build();

        // Create SettingDTO
        Setting setting = mock(Setting.class);
        when(setting.getId()).thenReturn(1L);
        when(setting.isMessageNotification()).thenReturn(true);
        when(setting.isCommentNotification()).thenReturn(true);
        when(setting.isPostFeaturedNotification()).thenReturn(true);
        settingDTO = new SettingDTO(setting);

        // Create KakaoFriendListDTO
        kakaoFriendListDTO = new KakaoFriendListDTO();
        // Set properties for kakaoFriendListDTO if needed
    }

    @Test
    @DisplayName("유저의 작성 게시글 목록 조회 테스트")
    void testGetPostList() {
        // Given
        Page<SimplePostDTO> postPage = new PageImpl<>(simplePostDTOList);
        when(userService.getPostList(anyInt(), anyInt(), any())).thenReturn(postPage);

        // When
        ResponseEntity<Page<SimplePostDTO>> response = userController.getPostList(0, 10, userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
    }

    @Test
    @DisplayName("유저의 작성 댓글 목록 조회 테스트")
    void testGetCommentList() {
        // Given
        Page<SimpleCommentDTO> commentPage = new PageImpl<>(simpleCommentDTOList);
        when(userService.getCommentList(anyInt(), anyInt(), any())).thenReturn(commentPage);

        // When
        ResponseEntity<Page<SimpleCommentDTO>> response = userController.getCommentList(0, 10, userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
    }

    @Test
    @DisplayName("유저가 추천한 게시글 목록 조회 테스트")
    void testGetLikedPosts() {
        // Given
        Page<SimplePostDTO> postPage = new PageImpl<>(simplePostDTOList);
        when(userService.getLikedPosts(anyInt(), anyInt(), any())).thenReturn(postPage);

        // When
        ResponseEntity<Page<SimplePostDTO>> response = userController.getLikedPosts(0, 10, userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
    }

    @Test
    @DisplayName("유저가 추천한 댓글 목록 조회 테스트")
    void testGetLikedComments() {
        // Given
        Page<SimpleCommentDTO> commentPage = new PageImpl<>(simpleCommentDTOList);
        when(userService.getLikedComments(anyInt(), anyInt(), any())).thenReturn(commentPage);

        // When
        ResponseEntity<Page<SimpleCommentDTO>> response = userController.getLikedComments(0, 10, userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
    }

    @Test
    @DisplayName("닉네임 변경 테스트")
    void testUpdateUserName() {
        // Given
        doNothing().when(userService).updateUserName(anyString(), any());

        // When
        ResponseEntity<String> response = userController.updateUserName(userDetails, userNameDTO);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("닉네임이 변경되었습니다.", response.getBody());
        verify(userService, times(1)).updateUserName(userNameDTO.getUserName(), userDetails);
    }

    @Test
    @DisplayName("건의하기 테스트")
    void testSuggestion() {
        // Given
        doNothing().when(userService).suggestion(any(), any());

        // When
        ResponseEntity<String> response = userController.suggestion(userDetails, reportDTO);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("건의가 접수되었습니다.", response.getBody());
        verify(userService, times(1)).suggestion(userDetails, reportDTO);
    }

    @Test
    @DisplayName("카카오 친구 목록 가져오기 테스트")
    void testGetFriendList() {
        // Given
        when(userService.getFriendList(any(), anyInt())).thenReturn(kakaoFriendListDTO);

        // When
        ResponseEntity<KakaoFriendListDTO> response = userController.getFriendList(userDetails, 0);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("설정 조회 테스트")
    void testGetSetting() {
        // Given
        when(userService.getSetting(any())).thenReturn(settingDTO);

        // When
        ResponseEntity<SettingDTO> response = userController.getSetting(userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("설정 수정 테스트")
    void testUpdateSetting() {
        // Given
        doNothing().when(userService).updateSetting(any(), any());

        // When
        ResponseEntity<String> response = userController.updateSetting(settingDTO, userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("설정 수정 완료", response.getBody());
        verify(userService, times(1)).updateSetting(settingDTO, userDetails);
    }
}
