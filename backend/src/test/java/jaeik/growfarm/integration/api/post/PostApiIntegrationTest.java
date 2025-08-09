//package jaeik.growfarm.integration.api.post;
//
//import jaeik.growfarm.dto.post.PostReqDTO;
//import jaeik.growfarm.entity.post.Post;
//import jaeik.growfarm.entity.user.Setting;
//import jaeik.growfarm.entity.user.UserRole;
//import jaeik.growfarm.entity.user.Users;
//import jaeik.growfarm.repository.post.PostRepository;
//import jaeik.growfarm.repository.user.SettingRepository;
//import jaeik.growfarm.repository.user.UserRepository;
//import org.junit.jupiter.api.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
//
///**
// * <h2>Post API 통합 테스트</h2>
// * <p>
// * 실제 데이터베이스와 웹 계층을 포함한 Post 관련 API의 전체적인 동작을 테스트합니다.
// * </p>
// * <p>
// * Controller → Service → Repository 전체 플로우를 검증합니다.
// * </p>
// *
// * @author Jaeik
// * @version 1.0.0
// */
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureWebMvc
//@ActiveProfiles("test")
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//@Transactional
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
//public class PostApiIntegrationTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private PostRepository postRepository;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private SettingRepository settingRepository;
//
//    private Users testUser;
//    private Post testPost;
//    private static Long createdPostId;
//
//    @BeforeEach
//    void setUp() {
//        // 테스트용 사용자 및 게시글 데이터 준비
//        Setting setting = Setting.builder()
//                .messageNotification(true)
//                .commentNotification(true)
//                .postFeaturedNotification(true)
//                .build();
//        settingRepository.save(setting);
//
//        testUser = Users.builder()
//                .kakaoId(12345L)
//                .kakaoNickname("testNickname")
//                .thumbnailImage("testImage.jpg")
//                .userName("testUser")
//                .role(UserRole.USER)
//                .setting(setting)
//                .build();
//        userRepository.save(testUser);
//
//        testPost = Post.builder()
//                .title("Integration Test Post")
//                .content("Integration Test Content")
//                .user(testUser)
//                .views(0)
//                .isNotice(false)
//                .build();
//        postRepository.save(testPost);
//    }
//
//    @Test
//    @Order(1)
//    @DisplayName("게시판 목록 조회 API 테스트")
//    void testGetBoardApi() throws Exception {
//        mockMvc.perform(get("/api/post/board")
//                        .param("page", "0")
//                        .param("size", "10"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content").isArray())
//                .andExpect(jsonPath("$.totalElements").isNumber())
//                .andExpect(jsonPath("$.size").value(10))
//                .andExpect(jsonPath("$.number").value(0));
//    }
//
//    @Test
//    @Order(2)
//    @DisplayName("게시글 상세 조회 API 테스트")
//    void testGetPostApi() throws Exception {
//        mockMvc.perform(get("/api/post/{postId}", testPost.getId()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.postId").value(testPost.getId()))
//                .andExpect(jsonPath("$.title").value("Integration Test Post"))
//                .andExpect(jsonPath("$.content").value("Integration Test Content"))
//                .andExpect(jsonPath("$.userName").value("testUser"));
//    }
//
//    @Test
//    @Order(3)
//    @DisplayName("게시글 작성 API 테스트 - 비회원")
//    void testWritePostByGuestApi() throws Exception {
//        PostReqDTO postReqDTO = new PostReqDTO();
//        postReqDTO.setTitle("Guest Post Title");
//        postReqDTO.setContent("Guest Post Content");
//        postReqDTO.setPassword(1234);
//
//        String response = mockMvc.perform(post("/api/post")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(postReqDTO)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.title").value("Guest Post Title"))
//                .andExpect(jsonPath("$.content").value("Guest Post Content"))
//                .andExpect(jsonPath("$.userName").value("익명"))
//                .andReturn().getResponse().getContentAsString();
//
//        // 생성된 게시글 ID 추출하여 저장
//        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
//        createdPostId = jsonNode.get("postId").asLong();
//    }
//
//    @Test
//    @Order(4)
//    @WithMockUser(username = "testUser", roles = {"USER"})
//    @DisplayName("게시글 작성 API 테스트 - 회원")
//    void testWritePostByUserApi() throws Exception {
//        PostReqDTO postReqDTO = new PostReqDTO();
//        postReqDTO.setTitle("User Post Title");
//        postReqDTO.setContent("User Post Content");
//
//        mockMvc.perform(post("/api/post")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(postReqDTO)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.title").value("User Post Title"))
//                .andExpect(jsonPath("$.content").value("User Post Content"))
//                .andExpect(jsonPath("$.userName").value("testUser"));
//    }
//
//    @Test
//    @Order(5)
//    @DisplayName("게시글 수정 API 테스트 - 비회원 (올바른 비밀번호)")
//    void testUpdatePostByGuestApi() throws Exception {
//        if (createdPostId == null) {
//            // 비회원 게시글이 생성되지 않았다면 건너뛰기
//            return;
//        }
//
//        String updateJson = """
//                {
//                    "postId": %d,
//                    "title": "Updated Guest Title",
//                    "content": "Updated Guest Content",
//                    "password": 1234
//                }
//                """.formatted(createdPostId);
//
//        mockMvc.perform(put("/api/post")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(updateJson))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().string("게시글 수정 완료"));
//    }
//
//    @Test
//    @Order(6)
//    @DisplayName("게시글 수정 API 테스트 - 비회원 (잘못된 비밀번호)")
//    void testUpdatePostByGuestWrongPasswordApi() throws Exception {
//        if (createdPostId == null) {
//            return;
//        }
//
//        String updateJson = """
//                {
//                    "postId": %d,
//                    "title": "Updated Guest Title",
//                    "content": "Updated Guest Content",
//                    "password": 9999
//                }
//                """.formatted(createdPostId);
//
//        mockMvc.perform(put("/api/post")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(updateJson))
//                .andDo(print())
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @Order(7)
//    @WithMockUser(username = "testUser", roles = {"USER"})
//    @DisplayName("게시글 수정 API 테스트 - 회원")
//    void testUpdatePostByUserApi() throws Exception {
//        String updateJson = """
//                {
//                    "postId": %d,
//                    "userId": %d,
//                    "title": "Updated User Title",
//                    "content": "Updated User Content"
//                }
//                """.formatted(testPost.getId(), testUser.getId());
//
//        mockMvc.perform(put("/api/post")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(updateJson))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().string("게시글 수정 완료"));
//    }
//
//    @Test
//    @Order(8)
//    @WithMockUser(username = "testUser", roles = {"USER"})
//    @DisplayName("게시글 추천 API 테스트")
//    void testLikePostApi() throws Exception {
//        String likeJson = """
//                {
//                    "postId": %d,
//                    "userId": %d
//                }
//                """.formatted(testPost.getId(), testUser.getId());
//
//        // 첫 번째 추천 (추가)
//        mockMvc.perform(post("/api/post/like")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(likeJson))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().string("추천 처리 완료"));
//
//        // 두 번째 추천 (취소)
//        mockMvc.perform(post("/api/post/like")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(likeJson))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().string("추천 처리 완료"));
//    }
//
//    @Test
//    @Order(9)
//    @DisplayName("게시글 검색 API 테스트 - 제목 검색")
//    void testSearchPostApi() throws Exception {
//        mockMvc.perform(get("/api/post/search")
//                        .param("searchType", "title")
//                        .param("searchQuery", "Integration")
//                        .param("page", "0")
//                        .param("size", "10"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content").isArray())
//                .andExpect(jsonPath("$.totalElements").isNumber());
//    }
//
//    @Test
//    @Order(10)
//    @DisplayName("게시글 검색 API 테스트 - 작성자 검색")
//    void testSearchPostByAuthorApi() throws Exception {
//        mockMvc.perform(get("/api/post/search")
//                        .param("searchType", "author")
//                        .param("searchQuery", "testUser")
//                        .param("page", "0")
//                        .param("size", "10"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content").isArray());
//    }
//
//    @Test
//    @Order(11)
//    @DisplayName("실시간 인기글 조회 API 테스트")
//    void testGetRealtimeBoardApi() throws Exception {
//        mockMvc.perform(get("/api/post/realtime"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$").isArray());
//    }
//
//    @Test
//    @Order(12)
//    @DisplayName("주간 인기글 조회 API 테스트")
//    void testGetWeeklyBoardApi() throws Exception {
//        mockMvc.perform(get("/api/post/weekly"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$").isArray());
//    }
//
//    @Test
//    @Order(13)
//    @DisplayName("레전드글 조회 API 테스트")
//    void testGetLegendBoardApi() throws Exception {
//        mockMvc.perform(get("/api/post/legend"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$").isArray());
//    }
//
//    @Test
//    @Order(14)
//    @WithMockUser(username = "testUser", roles = {"USER"})
//    @DisplayName("게시글 삭제 API 테스트 - 회원")
//    void testDeletePostByUserApi() throws Exception {
//        String deleteJson = """
//                {
//                    "postId": %d,
//                    "userId": %d
//                }
//                """.formatted(testPost.getId(), testUser.getId());
//
//        mockMvc.perform(delete("/api/post")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(deleteJson))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().string("게시글 삭제 완료"));
//    }
//
//    @Test
//    @Order(15)
//    @DisplayName("게시글 삭제 API 테스트 - 비회원")
//    void testDeletePostByGuestApi() throws Exception {
//        if (createdPostId == null) {
//            return;
//        }
//
//        String deleteJson = """
//                {
//                    "postId": %d,
//                    "password": 1234
//                }
//                """.formatted(createdPostId);
//
//        mockMvc.perform(delete("/api/post")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(deleteJson))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().string("게시글 삭제 완료"));
//    }
//
//    @Test
//    @Order(16)
//    @DisplayName("존재하지 않는 게시글 조회 API 테스트")
//    void testGetNonExistentPostApi() throws Exception {
//        mockMvc.perform(get("/api/post/{postId}", 999999L))
//                .andDo(print())
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    @Order(17)
//    @DisplayName("잘못된 검색 타입 API 테스트")
//    void testInvalidSearchTypeApi() throws Exception {
//        mockMvc.perform(get("/api/post/search")
//                        .param("searchType", "invalid")
//                        .param("searchQuery", "test")
//                        .param("page", "0")
//                        .param("size", "10"))
//                .andDo(print())
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @Order(18)
//    @DisplayName("빈 검색어 API 테스트")
//    void testEmptySearchQueryApi() throws Exception {
//        mockMvc.perform(get("/api/post/search")
//                        .param("searchType", "title")
//                        .param("searchQuery", "")
//                        .param("page", "0")
//                        .param("size", "10"))
//                .andDo(print())
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @Order(19)
//    @DisplayName("페이지 범위 초과 API 테스트")
//    void testInvalidPageNumberApi() throws Exception {
//        mockMvc.perform(get("/api/post/board")
//                        .param("page", "-1")
//                        .param("size", "10"))
//                .andDo(print())
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @Order(20)
//    @DisplayName("잘못된 페이지 사이즈 API 테스트")
//    void testInvalidPageSizeApi() throws Exception {
//        mockMvc.perform(get("/api/post/board")
//                        .param("page", "0")
//                        .param("size", "0"))
//                .andDo(print())
//                .andExpect(status().isBadRequest());
//    }
//}