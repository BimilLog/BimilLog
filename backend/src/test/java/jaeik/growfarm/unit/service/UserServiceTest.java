//package jaeik.growfarm.unit.service;
//
//import jaeik.growfarm.dto.admin.ReportDTO;
//import jaeik.growfarm.dto.comment.SimpleCommentDTO;
//import jaeik.growfarm.dto.kakao.KakaoCheckConsentDTO;
//import jaeik.growfarm.dto.kakao.KakaoCheckConsentDetailDTO;
//import jaeik.growfarm.dto.kakao.KakaoFriendDTO;
//import jaeik.growfarm.dto.kakao.KakaoFriendListDTO;
//import jaeik.growfarm.dto.post.SimplePostDTO;
//import jaeik.growfarm.dto.user.ClientDTO;
//import jaeik.growfarm.dto.user.SettingDTO;
//import jaeik.growfarm.entity.report.Report;
//import jaeik.growfarm.entity.report.ReportType;
//import jaeik.growfarm.entity.user.Setting;
//import jaeik.growfarm.entity.user.Token;
//import jaeik.growfarm.entity.user.Users;
//import jaeik.growfarm.global.auth.CustomUserDetails;
//import jaeik.growfarm.global.exception.CustomException;
//import jaeik.growfarm.repository.admin.ReportRepository;
//import jaeik.growfarm.repository.comment.CommentRepository;
//import jaeik.growfarm.repository.post.PostRepository;
//import jaeik.growfarm.repository.token.TokenRepository;
//import jaeik.growfarm.repository.user.SettingRepository;
//import jaeik.growfarm.repository.user.UserRepository;
//import jaeik.growfarm.service.kakao.KakaoService;
//import jaeik.growfarm.service.user.UserService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
///**
// * <h2>UserService 단위 테스트</h2>
// * <p>
// * UserService의 비즈니스 로직을 테스트합니다.
// * </p>
// * @version 1.0.0
// * @author Jaeik
// */
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//public class UserServiceTest {
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private PostRepository postRepository;
//
//    @Mock
//    private CommentRepository commentRepository;
//
//    @Mock
//    private ReportRepository reportRepository;
//
//    @Mock
//    private KakaoService kakaoService;
//
//    @Mock
//    private TokenRepository tokenRepository;
//
//    @Mock
//    private SettingRepository settingRepository;
//
//    @InjectMocks
//    private UserService userService;
//
//    private CustomUserDetails userDetails;
//    private Users user;
//    private Setting setting;
//    private SettingDTO settingDTO;
//    private ReportDTO reportDTO;
//    private KakaoFriendListDTO kakaoFriendListDTO;
//    private List<SimplePostDTO> simplePostDTOList;
//    private List<SimpleCommentDTO> simpleCommentDTOList;
//
//    @BeforeEach
//    void setUp() {
//        // Setup mock data
//        ClientDTO clientDTO = mock(ClientDTO.class);
//        when(clientDTO.getUserId()).thenReturn(1L);
//
//        settingDTO = mock(SettingDTO.class);
//        when(settingDTO.getSettingId()).thenReturn(1L);
//        when(clientDTO.getSettingDTO()).thenReturn(settingDTO);
//
//        userDetails = mock(CustomUserDetails.class);
//        when(userDetails.getUserId()).thenReturn(1L);
//        when(userDetails.getTokenId()).thenReturn(1L);
//        when(userDetails.getClientDTO()).thenReturn(clientDTO);
//
//        user = mock(Users.class);
//        when(user.getId()).thenReturn(1L);
//        when(user.getUserName()).thenReturn("testUser");
//
//        setting = mock(Setting.class);
//        when(setting.getId()).thenReturn(1L);
//        when(setting.isMessageNotification()).thenReturn(true);
//        when(setting.isCommentNotification()).thenReturn(true);
//        when(setting.isPostFeaturedNotification()).thenReturn(true);
//
//        Token token = mock(Token.class);
//        when(token.getId()).thenReturn(1L);
//        when(token.getKakaoAccessToken()).thenReturn("test-kakao-access-token");
//
//        // Create SimplePostDTO
//        SimplePostDTO simplePostDTO = mock(SimplePostDTO.class);
//        when(simplePostDTO.getPostId()).thenReturn(1L);
//        when(simplePostDTO.getUserId()).thenReturn(1L);
//        when(simplePostDTO.getTitle()).thenReturn("Test Post Title");
//
//        simplePostDTOList = new ArrayList<>();
//        simplePostDTOList.add(simplePostDTO);
//
//        // Create SimpleCommentDTO
//        SimpleCommentDTO simpleCommentDTO = mock(SimpleCommentDTO.class);
//        when(simpleCommentDTO.getId()).thenReturn(1L);
//        when(simpleCommentDTO.getPostId()).thenReturn(1L);
//        when(simpleCommentDTO.getContent()).thenReturn("Test comment content");
//
//        simpleCommentDTOList = new ArrayList<>();
//        simpleCommentDTOList.add(simpleCommentDTO);
//
//        // Create ReportDTO
//        reportDTO = ReportDTO.builder()
//                .reportId(1L)
//                .reportType(ReportType.IMPROVEMENT)
//                .userId(1L)
//                .content("Test suggestion content")
//                .build();
//
//        // Create KakaoFriendListDTO
//        KakaoFriendDTO kakaoFriendDTO = new KakaoFriendDTO();
//        kakaoFriendDTO.setId(123456789L);
//        kakaoFriendDTO.setUuid("test-uuid");
//        kakaoFriendDTO.setProfileNickname("kakao-nickname");
//
//        List<KakaoFriendDTO> friendList = new ArrayList<>();
//        friendList.add(kakaoFriendDTO);
//
//        kakaoFriendListDTO = new KakaoFriendListDTO();
//        kakaoFriendListDTO.setElements(friendList);
//        kakaoFriendListDTO.setTotalCount(1);
//
//        // Setup mock repositories
//        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
//        when(tokenRepository.findById(anyLong())).thenReturn(Optional.of(token));
//        when(settingRepository.findById(anyLong())).thenReturn(Optional.of(setting));
//    }
//
//    @Test
//    @DisplayName("유저 작성 게시글 목록 조회 테스트")
//    void testGetPostList() {
//        // Given
//        Page<SimplePostDTO> postPage = new PageImpl<>(simplePostDTOList);
//        when(postRepository.findPostsByUserId(anyLong(), any(Pageable.class))).thenReturn(postPage);
//
//        // When
//        Page<SimplePostDTO> result = userService.getPostList(0, 10, userDetails);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(1, result.getContent().size());
//        assertEquals("Test Post Title", result.getContent().getFirst().getTitle());
//        verify(postRepository, times(1)).findPostsByUserId(eq(1L), any(Pageable.class));
//    }
//
//    @Test
//    @DisplayName("유저 작성 댓글 목록 조회 테스트")
//    void testGetCommentList() {
//        // Given
//        Page<SimpleCommentDTO> commentPage = new PageImpl<>(simpleCommentDTOList);
//        when(commentRepository.findCommentsByUserId(anyLong(), any(Pageable.class))).thenReturn(commentPage);
//
//        // When
//        Page<SimpleCommentDTO> result = userService.getCommentList(0, 10, userDetails);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(1, result.getContent().size());
//        assertEquals("Test comment content", result.getContent().getFirst().getContent());
//        verify(commentRepository, times(1)).findCommentsByUserId(eq(1L), any(Pageable.class));
//    }
//
//    @Test
//    @DisplayName("유저가 추천한 게시글 목록 조회 테스트")
//    void testGetLikedPosts() {
//        // Given
//        Page<SimplePostDTO> postPage = new PageImpl<>(simplePostDTOList);
//        when(postRepository.findLikedPostsByUserId(anyLong(), any(Pageable.class))).thenReturn(postPage);
//
//        // When
//        Page<SimplePostDTO> result = userService.getLikedPosts(0, 10, userDetails);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(1, result.getContent().size());
//        assertEquals("Test Post Title", result.getContent().getFirst().getTitle());
//        verify(postRepository, times(1)).findLikedPostsByUserId(eq(1L), any(Pageable.class));
//    }
//
//    @Test
//    @DisplayName("유저가 추천한 댓글 목록 조회 테스트")
//    void testGetLikedComments() {
//        // Given
//        Page<SimpleCommentDTO> commentPage = new PageImpl<>(simpleCommentDTOList);
//        when(commentRepository.findLikedCommentsByUserId(anyLong(), any(Pageable.class))).thenReturn(commentPage);
//
//        // When
//        Page<SimpleCommentDTO> result = userService.getLikedComments(0, 10, userDetails);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(1, result.getContent().size());
//        assertEquals("Test comment content", result.getContent().getFirst().getContent());
//        verify(commentRepository, times(1)).findLikedCommentsByUserId(eq(1L), any(Pageable.class));
//    }
//
//    @Test
//    @DisplayName("닉네임 변경 테스트 - 성공")
//    void testUpdateUserNameSuccess() {
//        // Given
//        when(userRepository.existsByUserName(anyString())).thenReturn(false);
//        doNothing().when(user).updateUserName(anyString());
//
//        // When
//        userService.updateUserName("newUserName", userDetails);
//
//        // Then
//        verify(userRepository, times(1)).existsByUserName(eq("newUserName"));
//        verify(user, times(1)).updateUserName(eq("newUserName"));
//    }
//
//    @Test
//    @DisplayName("닉네임 변경 테스트 - 실패 (이미 존재하는 닉네임)")
//    void testUpdateUserNameExistingNickname() {
//        // Given
//        when(userRepository.existsByUserName(anyString())).thenReturn(true);
//
//        // When & Then
//        assertThrows(CustomException.class, () -> userService.updateUserName("existingUserName", userDetails));
//        verify(userRepository, times(1)).existsByUserName(eq("existingUserName"));
//        verify(user, never()).updateUserName(anyString());
//    }
//
//    @Test
//    @DisplayName("건의하기 테스트")
//    void testSuggestion() {
//        // Given
//        when(userRepository.getReferenceById(anyLong())).thenReturn(user);
//        when(reportRepository.save(any(Report.class))).thenReturn(mock(Report.class));
//
//        // When
//        userService.suggestion(userDetails, reportDTO);
//
//        // Then
//        verify(reportRepository, times(1)).save(any(Report.class));
//    }
//
//    @Test
//    @DisplayName("카카오 친구 목록 조회 테스트 - 성공")
//    void testGetFriendListSuccess() {
//        // Given
//        KakaoCheckConsentDTO consentDTO = new KakaoCheckConsentDTO();
//        KakaoCheckConsentDetailDTO[] scopes = new KakaoCheckConsentDetailDTO[1];
//        scopes[0] = new KakaoCheckConsentDetailDTO();
//        scopes[0].setId("friends");
//        scopes[0].setAgreed(true);
//        consentDTO.setScopes(scopes);
//
//        when(kakaoService.checkConsent(anyString())).thenReturn(consentDTO);
//        when(kakaoService.getFriendList(anyString(), anyInt())).thenReturn(kakaoFriendListDTO);
//        when(userRepository.findUserNamesInOrder(anyList())).thenReturn(List.of("testUser"));
//
//        // When
//        KakaoFriendListDTO result = userService.getFriendList(userDetails, 0);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(1, result.getTotalCount());
//        assertEquals(1, result.getElements().size());
//        verify(kakaoService, times(1)).checkConsent(eq("test-kakao-access-token"));
//        verify(kakaoService, times(1)).getFriendList(eq("test-kakao-access-token"), eq(0));
//    }
//
//    @Test
//    @DisplayName("카카오 친구 목록 조회 테스트 - 실패 (동의 없음)")
//    void testGetFriendListNoConsent() {
//        // Given
//        KakaoCheckConsentDTO consentDTO = new KakaoCheckConsentDTO();
//        KakaoCheckConsentDetailDTO[] scopes = new KakaoCheckConsentDetailDTO[1];
//        scopes[0] = new KakaoCheckConsentDetailDTO();
//        scopes[0].setId("friends");
//        scopes[0].setAgreed(false);
//        consentDTO.setScopes(scopes);
//
//        when(kakaoService.checkConsent(anyString())).thenReturn(consentDTO);
//
//        // When & Then
//        assertThrows(CustomException.class, () -> userService.getFriendList(userDetails, 0));
//        verify(kakaoService, times(1)).checkConsent(eq("test-kakao-access-token"));
//        verify(kakaoService, never()).getFriendList(anyString(), anyInt());
//    }
//
//    @Test
//    @DisplayName("설정 조회 테스트")
//    void testGetSetting() {
//        // When
//        SettingDTO result = userService.getSetting(userDetails);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(1L, result.getSettingId());
//        assertTrue(result.isMessageNotification());
//        assertTrue(result.isCommentNotification());
//        assertTrue(result.isPostFeaturedNotification());
//        verify(settingRepository, times(1)).findById(eq(1L));
//    }
//
//    @Test
//    @DisplayName("설정 수정 테스트")
//    void testUpdateSetting() {
//        // Given
//        doNothing().when(setting).updateSetting(any(SettingDTO.class));
//
//        // When
//        userService.updateSetting(settingDTO, userDetails);
//
//        // Then
//        verify(settingRepository, times(1)).findById(eq(1L));
//        verify(setting, times(1)).updateSetting(eq(settingDTO));
//    }
//}
