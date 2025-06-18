package jaeik.growfarm.controller;

//@SpringBootTest
//public class UserControllerTest {
//
//    @Mock
//    private UserService userService;
//
//    @InjectMocks
//    private UserController userController;
//
//    private CustomUserDetails userDetails;
//    private SimplePostDTO simplePostDTO;
//    private CommentDTO commentDTO;
//    private UserNameReqDTO userNameReqDTO;
//    private ReportDTO reportDTO;
//    private SettingDTO settingDTO;
//    private KakaoFriendListDTO kakaoFriendListDTO;
//    private List<SimplePostDTO> simplePostDTOList;
//    private List<CommentDTO> commentDTOList;
//
//    @BeforeEach
//    void setUp() {
//        // Setup mock data
//        userDetails = mock(CustomUserDetails.class);
//        when(userDetails.getClientDTO()).thenReturn(mock(jaeik.growfarm.dto.user.UserDTO.class));
//        when(userDetails.getClientDTO().getUserId()).thenReturn(1L);
//
//        // Create SimplePostDTO with required parameters
//        simplePostDTO = new SimplePostDTO(
//                1L,                           // postId
//                1L,                           // userId
//                "testPaper",                   // userName
//                "Test Title",                 // title
//                0,                            // commentCount
//                0,                            // likes
//                0,                            // views
//                Instant.now(),          // createdAt
//                false,                        // is_notice
//                false,                        // is_RealtimePopular
//                false,                        // is_WeeklyPopular
//                false                         // is_HallOfFame
//        );
//
//        simplePostDTOList = new ArrayList<>();
//        simplePostDTOList.add(simplePostDTO);
//
////        // Create CommentDTO with required parameters
////        commentDTO = new CommentDTO(
////                1L,                           // id
////                1L,                           // postId
////                1L,                           // userId
////                "testPaper",                   // userName
////                "Test comment content",       // content
////                0,                            // likes
////                Instant.now(),          // createdAt
////                false,                        // is_featured
////                false                         // userLike
////        );
//
//        commentDTOList = new ArrayList<>();
//        commentDTOList.add(commentDTO);
//
//        // Create UserNameReqDTO
//        userNameReqDTO = new UserNameReqDTO();
//        userNameReqDTO.setUserName("newUserName");
//
//        // Create ReportDTO
//        reportDTO = ReportDTO.builder()
//                .reportId(1L)
//                .reportType(ReportType.IMPROVEMENT)
//                .userId(1L)
//                .content("Test suggestion content")
//                .build();
//
//        // Create SettingDTO
//        settingDTO = new SettingDTO();
//        // Set properties for settingDTO if needed
//
//        // Create KakaoFriendListDTO
//        kakaoFriendListDTO = new KakaoFriendListDTO();
//        // Set properties for kakaoFriendListDTO if needed
//    }
//
//    @Test
//    @DisplayName("유저의 작성 게시글 목록 조회 테스트")
//    void testGetPostList() {
//        // Given
//        Page<SimplePostDTO> postPage = new PageImpl<>(simplePostDTOList);
//        when(userService.getPostList(anyInt(), anyInt(), any())).thenReturn(postPage);
//
//        // When
//        ResponseEntity<Page<SimplePostDTO>> response = userController.getPostList(0, 10, userDetails);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(1, response.getBody().getContent().size());
//    }
//
//    @Test
//    @DisplayName("유저의 작성 댓글 목록 조회 테스트")
//    void testGetCommentList() {
//        // Given
//        Page<CommentDTO> commentPage = new PageImpl<>(commentDTOList);
//        when(userService.getCommentList(anyInt(), anyInt(), any())).thenReturn(commentPage);
//
//        // When
//        ResponseEntity<Page<CommentDTO>> response = userController.getCommentList(0, 10, userDetails);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(1, response.getBody().getContent().size());
//    }
//
//    @Test
//    @DisplayName("유저가 추천한 게시글 목록 조회 테스트")
//    void testGetLikedPosts() {
//        // Given
//        Page<SimplePostDTO> postPage = new PageImpl<>(simplePostDTOList);
//        when(userService.getLikedPosts(anyInt(), anyInt(), any())).thenReturn(postPage);
//
//        // When
//        ResponseEntity<Page<SimplePostDTO>> response = userController.getLikedPosts(0, 10, userDetails);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(1, response.getBody().getContent().size());
//    }
//
//    @Test
//    @DisplayName("유저가 추천한 댓글 목록 조회 테스트")
//    void testGetLikedComments() {
//        // Given
//        Page<CommentDTO> commentPage = new PageImpl<>(commentDTOList);
//        when(userService.getLikedComments(anyInt(), anyInt(), any())).thenReturn(commentPage);
//
//        // When
//        ResponseEntity<Page<CommentDTO>> response = userController.getLikedComments(0, 10, userDetails);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(1, response.getBody().getContent().size());
//    }
//
//    @Test
//    @DisplayName("닉네임 변경 테스트")
//    void testUpdateUserName() {
//        // Given
//        doNothing().when(userService).updateUserName(anyString(), any());
//
//        // When
//        ResponseEntity<String> response = userController.updateUserName(userDetails, userNameReqDTO);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("닉네임이 변경되었습니다.", response.getBody());
//        verify(userService, times(1)).updateUserName(userNameReqDTO.getUserName(), userDetails);
//    }
//
//    @Test
//    @DisplayName("건의하기 테스트")
//    void testSuggestion() {
//        // Given
//        doNothing().when(userService).suggestion(any());
//
//        // When
//        ResponseEntity<Void> response = userController.suggestion(reportDTO);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(userService, times(1)).suggestion(reportDTO);
//    }
//
//    @Test
//    @DisplayName("카카오 친구 목록 가져오기 테스트")
//    void testGetFriendList() {
//        // Given
//        when(userService.getFriendList(any(), anyInt())).thenReturn(kakaoFriendListDTO);
//
//        // When
//        ResponseEntity<KakaoFriendListDTO> response = userController.getFriendList(userDetails, 0);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//    }
//
//    @Test
//    @DisplayName("설정 조회 테스트")
//    void testGetSetting() {
//        // Given
//        when(userService.getSetting(anyLong())).thenReturn(settingDTO);
//
//        // When
//        ResponseEntity<SettingDTO> response = userController.getSetting(userDetails);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//    }
//
//    @Test
//    @DisplayName("설정 수정 테스트")
//    void testUpdateSetting() {
//        // Given
//        doNothing().when(userService).updateSetting(any(), anyLong());
//        when(userService.getSetting(anyLong())).thenReturn(settingDTO);
//
//        // When
//        ResponseEntity<SettingDTO> response = userController.updateSetting(settingDTO, userDetails);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        verify(userService, times(1)).updateSetting(settingDTO, userDetails.getClientDTO().getUserId());
//        verify(userService, times(1)).getSetting(userDetails.getClientDTO().getUserId());
//    }
//}
