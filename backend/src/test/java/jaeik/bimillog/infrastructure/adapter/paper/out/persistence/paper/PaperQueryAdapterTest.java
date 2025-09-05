package jaeik.bimillog.infrastructure.adapter.paper.out.persistence.paper;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.auth.entity.SocialProvider;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.util.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * <h2>PaperQueryAdapter 테스트</h2>
 * <p>PaperQueryAdapter의 모든 기능을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = BimilLogApplication.class
        )
)
@Import({PaperQueryAdapter.class, TestContainersConfiguration.class})
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect",
        "logging.level.org.hibernate.SQL=DEBUG"
})
@DisplayName("PaperQueryAdapter 통합 테스트")
class PaperQueryAdapterTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private PaperQueryAdapter paperQueryAdapter;

    private User testUser;
    private User anotherUser;
    private Message testMessage1;
    private Message testMessage2;
    private Message anotherUserMessage;

    @BeforeEach
    void setUp() {
        // 사용자 엔티티 생성 및 저장
        testUser = createAndSaveUser("testuser", "12345", "테스트유저");
        anotherUser = createAndSaveUser("anotheruser", "67890", "다른유저");

        // 메시지 엔티티 생성 및 저장
        testMessage1 = createAndSaveMessage(testUser, "첫 번째 메시지", DecoType.APPLE, 100, 50);
        testMessage2 = createAndSaveMessage(testUser, "두 번째 메시지", DecoType.BANANA, 150, 75);
        anotherUserMessage = createAndSaveMessage(anotherUser, "다른 사용자 메시지", DecoType.GRAPE, 200, 100);

        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    @DisplayName("메시지 ID로 메시지 조회 - 성공")
    void shouldFindMessageById_WhenValidId() {
        // When
        Optional<Message> result = paperQueryAdapter.findMessageById(testMessage1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(testMessage1.getId());
        assertThat(result.get().getContent()).isEqualTo("첫 번째 메시지");
        assertThat(result.get().getDecoType()).isEqualTo(DecoType.APPLE);
    }

    @Test
    @DisplayName("메시지 ID로 메시지 조회 - 존재하지 않는 ID")
    void shouldReturnEmpty_WhenMessageNotFound() {
        // When
        Optional<Message> result = paperQueryAdapter.findMessageById(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("메시지 ID로 메시지 조회 - null ID")
    void shouldReturnEmpty_WhenNullId() {
        // When
        Optional<Message> result = paperQueryAdapter.findMessageById(null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID로 메시지 목록 조회 - 성공")
    void shouldFindMessagesByUserId_WhenValidUserId() {
        // Given
        Long userId = testUser.getId();

        // When
        List<Message> result = paperQueryAdapter.findMessagesByUserId(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
        assertThat(result.get(1).getUserId()).isEqualTo(userId);
        
        // 내림차순 정렬 확인 (최신순)
        assertThat(result.get(0).getCreatedAt()).isAfterOrEqualTo(result.get(1).getCreatedAt());
    }

    @Test
    @DisplayName("사용자 ID로 메시지 목록 조회 - 메시지가 없는 경우")
    void shouldReturnEmptyList_WhenUserHasNoMessages() {
        // Given
        User userWithoutMessages = createAndSaveUser("noMessages", "555", "메시지없음");
        testEntityManager.flush();

        // When
        List<Message> result = paperQueryAdapter.findMessagesByUserId(userWithoutMessages.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자명으로 메시지 목록 조회 - 성공")
    void shouldFindMessagesByUserName_WhenValidUserName() {
        // When
        List<Message> result = paperQueryAdapter.findMessagesByUserName(testUser.getUserName());

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo(testUser.getId());
        assertThat(result.get(1).getUserId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("사용자명으로 메시지 목록 조회 - 메시지가 없는 경우")
    void shouldReturnEmptyList_WhenUserNameHasNoMessages() {
        // Given
        User userWithoutMessages = createAndSaveUser("noMessages", "555", "메시지없음");
        testEntityManager.flush();

        // When
        List<Message> result = paperQueryAdapter.findMessagesByUserName(userWithoutMessages.getUserName());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("null 및 빈 문자열 처리")
    void shouldHandleNullAndEmptyInputs() {
        assertThatNoException().isThrownBy(() -> {
            List<Message> nullUserIdResult = paperQueryAdapter.findMessagesByUserId(null);
            assertThat(nullUserIdResult).isEmpty();
            
            List<Message> nullUserNameResult = paperQueryAdapter.findMessagesByUserName(null);
            assertThat(nullUserNameResult).isEmpty();
            
            List<Message> emptyUserNameResult = paperQueryAdapter.findMessagesByUserName("");
            assertThat(emptyUserNameResult).isEmpty();
            
            List<Message> whiteSpaceUserNameResult = paperQueryAdapter.findMessagesByUserName("   ");
            assertThat(whiteSpaceUserNameResult).isEmpty();
        });
    }

    @Test
    @DisplayName("사용자 엔티티 페치 조인 확인")
    void shouldFetchUserEntity_WhenFindingMessages() {
        // When
        List<Message> result = paperQueryAdapter.findMessagesByUserId(testUser.getId());

        // Then - User 엔티티가 제대로 페치 조인되어야 함
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUser()).isNotNull();
        assertThat(result.get(0).getUser().getUserName()).isEqualTo(testUser.getUserName());
    }

    @Test
    @DisplayName("복수 사용자 메시지 조회시 각자의 메시지만 조회됨")
    void shouldReturnOnlyUserOwnMessages_WhenMultipleUsersExist() {
        // When
        List<Message> testUserMessages = paperQueryAdapter.findMessagesByUserId(testUser.getId());
        List<Message> anotherUserMessages = paperQueryAdapter.findMessagesByUserId(anotherUser.getId());

        // Then
        assertThat(testUserMessages).hasSize(2);
        assertThat(anotherUserMessages).hasSize(1);
        
        testUserMessages.forEach(message -> 
            assertThat(message.getUserId()).isEqualTo(testUser.getId())
        );
        
        anotherUserMessages.forEach(message -> 
            assertThat(message.getUserId()).isEqualTo(anotherUser.getId())
        );
    }

    @Test
    @DisplayName("대용량 메시지 조회 성능 테스트")
    void shouldHandleLargeNumberOfMessages() {
        // Given - 대용량 메시지 생성
        User bulkUser = createAndSaveUser("bulkuser", "999", "대용량");
        for (int i = 0; i < 100; i++) {
            createAndSaveMessage(bulkUser, "대용량 메시지 " + i, DecoType.APPLE, 100 + i, 50 + i);
        }
        testEntityManager.flush();

        // When
        long startTime = System.currentTimeMillis();
        List<Message> result = paperQueryAdapter.findMessagesByUserId(bulkUser.getId());
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(result).hasSize(100);
        assertThat(endTime - startTime).isLessThan(1000); // 1초 이내 조회
        
        // 정렬 확인
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getCreatedAt())
                .isAfterOrEqualTo(result.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("특수문자가 포함된 사용자명 검색")
    void shouldFindMessages_WithSpecialCharacterUserName() {
        // Given
        User specialUser = createAndSaveUser("user@test_123", "special123", "특수문자");
        createAndSaveMessage(specialUser, "특수문자 사용자 메시지", DecoType.STAR, 300, 150);
        testEntityManager.flush();

        // When
        List<Message> result = paperQueryAdapter.findMessagesByUserName("user@test_123");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("특수문자 사용자 메시지");
    }

    private User createAndSaveUser(String userName, String socialId, String socialNickname) {
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
        
        User user = User.builder()
                .userName(userName)
                .socialId(socialId)
                .socialNickname(socialNickname)
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(setting)
                .build();
        
        return testEntityManager.persistAndFlush(user);
    }

    private Message createAndSaveMessage(User user, String content, DecoType decoType, int width, int height) {
        Message message = Message.builder()
                .user(user)
                .content(content)
                .decoType(decoType)
                .anonymity("익명123")  // anonymity는 8자 제한이므로 짧게 변경
                .width(width)
                .height(height)
                .build();
        
        return testEntityManager.persistAndFlush(message);
    }
}