package jaeik.bimillog.infrastructure.outadapter.comment.persistence.comment;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.CommentSaveAdapter;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.jpa.CommentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>CommentSaveAdapter 통합 테스트</h2>
 * <p>실제 MySQL 데이터베이스를 사용한 CommentSaveAdapter의 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 댓글 저장 동작 검증</p>
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
@Testcontainers
@EntityScan(basePackages = {
        "jaeik.bimillog.domain.comment.entity",
        "jaeik.bimillog.domain.user.entity",
        "jaeik.bimillog.domain.post.entity",
        "jaeik.bimillog.domain.global.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.comment",
        "jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.commentclosure"
})
@Import(CommentSaveAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class CommentSaveAdapterIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }
    }

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentSaveAdapter commentSaveAdapter;

    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        commentRepository.deleteAll();
        
        // 테스트용 사용자 생성 (각 테스트마다 고유한 ID 사용)
        String uniqueSocialId = "kakao_save_" + System.nanoTime();
        Setting setting = Setting.createSetting();
        entityManager.persistAndFlush(setting);
        
        testUser = User.builder()
                .socialId(uniqueSocialId) // 나노타임으로 고유성 보장
                .provider(SocialProvider.KAKAO)
                .userName("testUserSave_" + System.nanoTime())
                .socialNickname("저장테스트유저")
                .role(UserRole.USER)
                .setting(setting)
                .build();
        entityManager.persistAndFlush(testUser);
        
        // 테스트용 게시글 생성
        testPost = Post.builder()
                .user(testUser)
                .title("테스트 게시글")
                .content("테스트 내용")
                .isNotice(false)
                .views(0)
                .build();
        entityManager.persistAndFlush(testPost);
    }

    @Test
    @DisplayName("정상 케이스 - 새로운 댓글 저장")
    void shouldSaveNewComment_WhenValidCommentProvided() {
        // Given: 새로운 댓글 엔티티
        Comment newComment = Comment.createComment(
                testPost, 
                testUser, 
                "테스트 댓글 내용", 
                null
        );

        // When: 댓글 저장
        Comment savedComment = commentRepository.save(newComment);

        // Then: 댓글이 올바르게 저장되었는지 검증
        assertThat(savedComment).isNotNull();
        assertThat(savedComment.getId()).isNotNull();
        assertThat(savedComment.getContent()).isEqualTo("테스트 댓글 내용");
        assertThat(savedComment.getPost()).isEqualTo(testPost);
        assertThat(savedComment.getUser()).isEqualTo(testUser);
        assertThat(savedComment.isDeleted()).isFalse();
        assertThat(savedComment.getPassword()).isNull();
        
        // DB에 실제로 저장되었는지 확인
        Optional<Comment> foundComment = commentRepository.findById(savedComment.getId());
        assertThat(foundComment).isPresent();
        assertThat(foundComment.get().getContent()).isEqualTo("테스트 댓글 내용");
    }

    @Test
    @DisplayName("정상 케이스 - 비밀번호가 있는 댓글 저장")
    void shouldSaveCommentWithPassword_WhenPasswordProvided() {
        // Given: 비밀번호가 있는 댓글
        Comment commentWithPassword = Comment.createComment(
                testPost, 
                null,  // 익명 댓글
                "익명 댓글 내용", 
                1234
        );

        // When: 댓글 저장
        Comment savedComment = commentRepository.save(commentWithPassword);

        // Then: 비밀번호 댓글이 올바르게 저장되었는지 검증
        assertThat(savedComment).isNotNull();
        assertThat(savedComment.getId()).isNotNull();
        assertThat(savedComment.getContent()).isEqualTo("익명 댓글 내용");
        assertThat(savedComment.getUser()).isNull();
        assertThat(savedComment.getPassword()).isEqualTo(1234);
        
        // DB에 실제로 저장되었는지 확인
        Optional<Comment> foundComment = commentRepository.findById(savedComment.getId());
        assertThat(foundComment).isPresent();
        assertThat(foundComment.get().getPassword()).isEqualTo(1234);
    }

    @Test
    @DisplayName("정상 케이스 - 기존 댓글 내용 수정")
    void shouldUpdateExistingComment_WhenCommentModified() {
        // Given: 기존 댓글 생성 및 저장
        Comment existingComment = Comment.createComment(
                testPost, 
                testUser, 
                "원본 댓글 내용", 
                null
        );
        existingComment = commentRepository.save(existingComment);

        // 댓글 내용 수정
        existingComment.updateComment("수정된 댓글 내용");

        // When: 수정된 댓글 저장
        Comment updatedComment = commentRepository.save(existingComment);

        // Then: 댓글 내용이 올바르게 업데이트되었는지 검증
        assertThat(updatedComment.getId()).isEqualTo(existingComment.getId());
        assertThat(updatedComment.getContent()).isEqualTo("수정된 댓글 내용");
        
        // DB에서 다시 조회하여 확인
        Optional<Comment> foundComment = commentRepository.findById(updatedComment.getId());
        assertThat(foundComment).isPresent();
        assertThat(foundComment.get().getContent()).isEqualTo("수정된 댓글 내용");
    }

    @Test
    @DisplayName("경계값 - 최대 길이 댓글 저장")
    void shouldSaveLongComment_WhenMaxLengthContentProvided() {
        // Given: 최대 길이(255자)의 댓글 내용
        String longContent = "가".repeat(255);
        Comment longComment = Comment.createComment(testPost, testUser, longContent, null);

        // When: 긴 댓글 저장
        Comment savedComment = commentRepository.save(longComment);

        // Then: 긴 댓글이 올바르게 저장되었는지 검증
        assertThat(savedComment).isNotNull();
        assertThat(savedComment.getContent()).hasSize(255);
        assertThat(savedComment.getContent()).isEqualTo(longContent);
        
        // DB에서 확인
        Optional<Comment> foundComment = commentRepository.findById(savedComment.getId());
        assertThat(foundComment).isPresent();
        assertThat(foundComment.get().getContent()).hasSize(255);
    }

    @Test
    @DisplayName("트랜잭션 - 복잡한 댓글 엔티티 저장 및 조회")
    void shouldSaveComplexCommentEntity_WhenAllFieldsProvided() {
        // Given: 모든 필드를 포함한 복잡한 댓글 엔티티
        Comment complexComment = Comment.createComment(
                testPost,
                testUser,
                "복잡한 댓글 내용입니다. 특수문자도 포함! @#$%^&*()",
                5678
        );

        // When: 복잡한 댓글 저장
        Comment savedComplexComment = commentRepository.save(complexComment);

        // Then: 모든 필드가 올바르게 저장되었는지 검증
        assertThat(savedComplexComment.getId()).isNotNull();
        assertThat(savedComplexComment.getPost()).isEqualTo(testPost);
        assertThat(savedComplexComment.getUser()).isEqualTo(testUser);
        assertThat(savedComplexComment.getContent()).isEqualTo("복잡한 댓글 내용입니다. 특수문자도 포함! @#$%^&*()");
        assertThat(savedComplexComment.getPassword()).isEqualTo(5678);
        assertThat(savedComplexComment.isDeleted()).isFalse();
        
        // 기본 엔티티 필드 검증
        assertThat(savedComplexComment.getCreatedAt()).isNotNull();
        assertThat(savedComplexComment.getModifiedAt()).isNotNull();
        
        // DB에서 직접 조회하여 재확인
        Optional<Comment> foundComment = commentRepository.findById(savedComplexComment.getId());
        assertThat(foundComment).isPresent();
        Comment dbComment = foundComment.get();
        assertThat(dbComment.getContent()).contains("특수문자도 포함");
        assertThat(dbComment.getPassword()).isEqualTo(5678);
    }
}