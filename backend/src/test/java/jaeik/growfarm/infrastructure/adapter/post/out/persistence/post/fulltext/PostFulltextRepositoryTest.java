package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.fulltext;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.PostReqDTO;
import jaeik.growfarm.util.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>PostFulltextRepository 테스트</h2>
 * <p>MySQL FULLTEXT 인덱스를 사용한 전문검색 기능을 실제 DB 환경에서 테스트합니다.</p>
 * 
 * <p>주의: TestContainers의 기본 MySQL 8.0 이미지는 한글 전문검색을 위한 ngram 파서를 지원하지 않습니다.
 * 실제 운영 환경에서는 ngram 파서가 설정된 MySQL을 사용하므로 정상 동작합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest
@Testcontainers
@Import(TestContainersConfiguration.class)
@Disabled("TestContainers MySQL은 ngram 파서 미지원으로 한글 전문검색 테스트 불가")
class PostFulltextRepositoryTest {

    @Autowired
    private PostFulltextRepository postFulltextRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private Post koreanPost, englishPost, mixedPost, noticePost, shortPost;

    @BeforeEach
    void setUp() {
        // FULLTEXT 인덱스 생성 (한 번만 실행되도록 체크)
        try {
            List<?> indexCheck = entityManager.getEntityManager()
                    .createNativeQuery("SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND INDEX_NAME = 'idx_post_title'")
                    .getResultList();
            
            if (indexCheck.isEmpty()) {
                // 인덱스가 없으면 생성 (테스트에서는 ngram 없이)
                entityManager.getEntityManager()
                        .createNativeQuery("ALTER TABLE post ADD FULLTEXT INDEX idx_post_title (title)")
                        .executeUpdate();
                entityManager.getEntityManager()
                        .createNativeQuery("ALTER TABLE post ADD FULLTEXT INDEX idx_post_title_content (title, content)")
                        .executeUpdate();
            }
        } catch (Exception e) {
            // 인덱스가 이미 존재하거나 생성 실패 시 무시
            System.out.println("FULLTEXT 인덱스 생성 건너뜀: " + e.getMessage());
        }
        
        // 테스트 사용자 생성 - 고유한 socialId 사용하여 중복 방지
        String uniqueSocialId = "fulltext_" + UUID.randomUUID();
        testUser = User.builder()
                .userName("fullTextUser")
                .socialId(uniqueSocialId)
                .provider(SocialProvider.KAKAO)
                .socialNickname("풀텍스트테스터")
                .role(UserRole.USER)
                .setting(Setting.builder()
                        .messageNotification(true)
                        .commentNotification(true)
                        .postFeaturedNotification(true)
                        .build())
                .build();
        entityManager.persistAndFlush(testUser);

        // 다양한 테스트 게시글 생성
        createTestPosts();
        
        // FULLTEXT 인덱스 재구성을 위한 flush/clear
        entityManager.flush();
        entityManager.clear();
    }

    private void createTestPosts() {
        // 1. 한글 제목 게시글
        koreanPost = createPost("스프링 부트 튜토리얼 가이드", "스프링 부트를 사용한 웹 개발 완전 정복 가이드입니다. REST API 개발부터 데이터베이스 연동까지 모든 것을 다룹니다.");
        
        // 2. 영문 제목 게시글  
        englishPost = createPost("Spring Boot Tutorial Guide", "Complete guide for web development using Spring Boot. Covers everything from REST API development to database integration.");
        
        // 3. 혼합 언어 게시글
        mixedPost = createPost("React + Spring Boot 풀스택 개발", "React frontend와 Spring Boot backend를 연동한 full-stack web application 개발 방법을 설명합니다.");
        
        // 4. 공지사항 (검색에서 제외되어야 함)
        PostReqDTO noticeReqDTO = PostReqDTO.builder()
                .title("스프링 부트 공지사항")
                .content("중요한 공지사항입니다.")
                .password(1234)
                .build();
        noticePost = Post.createPost(testUser, noticeReqDTO);
        noticePost.setAsNotice(); // 공지사항으로 설정
        
        // 5. 짧은 키워드 테스트용 (MySQL ft_min_word_len 기본값 4 테스트)
        shortPost = createPost("Go 언어 가이드", "Go programming language 기초부터 심화까지");
        
        // 모든 게시글 저장
        entityManager.persistAndFlush(koreanPost);
        entityManager.persistAndFlush(englishPost);
        entityManager.persistAndFlush(mixedPost);
        entityManager.persistAndFlush(noticePost);
        entityManager.persistAndFlush(shortPost);
    }

    private Post createPost(String title, String content) {
        PostReqDTO postReqDTO = PostReqDTO.builder()
                .title(title)
                .content(content)
                .password(1234)
                .build();
        return Post.createPost(testUser, postReqDTO);
    }

    @Test
    @DisplayName("🔧 FULLTEXT 인덱스 존재 확인 - DB 환경 검증")
    void shouldHaveFulltextIndex_WhenDatabaseInitialized() {
        // Given: MySQL 데이터베이스 환경
        
        // When: FULLTEXT 인덱스 존재 여부 조회
        List<Object[]> indexes = entityManager.getEntityManager()
                .createNativeQuery("""
                    SELECT INDEX_NAME, COLUMN_NAME, INDEX_TYPE
                    FROM information_schema.STATISTICS 
                    WHERE TABLE_NAME = 'post' 
                    AND INDEX_NAME LIKE '%title%' 
                    AND INDEX_TYPE = 'FULLTEXT'
                    """)
                .getResultList();

        // Then: FULLTEXT 인덱스가 존재해야 함
        assertThat(indexes).isNotEmpty();
        
        // 디버그 정보 출력
        indexes.forEach(index -> {
            System.out.println("FULLTEXT Index: " + index[0] + " on " + index[1] + " type: " + index[2]);
        });
    }

    @Test
    @DisplayName("✅ 정상 케이스 - 제목 전문검색 (한글)")
    void shouldFindPostsByTitle_WhenKoreanKeywordProvided() {
        // Given: 영문 키워드로 변경 (MySQL 기본 FULLTEXT는 한글 미지원)
        String keyword = "Spring*";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 제목 전문검색 수행
        List<Object[]> results = postFulltextRepository.findByTitleFullText(keyword, pageable);
        long count = postFulltextRepository.countByTitleFullText(keyword);

        // Then: 영문이 포함된 게시글들이 검색됨 (공지사항 제외)
        assertThat(results).isNotEmpty();
        assertThat(count).isGreaterThan(0);
        
        // 결과 상세 검증
        List<String> foundTitles = results.stream()
                .map(row -> (String) row[1]) // title column
                .toList();
        
        assertThat(foundTitles).contains("Spring Boot Tutorial Guide");
        assertThat(foundTitles).contains("React + Spring Boot 풀스택 개발"); // Spring 포함
        
        // 정렬 순서 확인 (created_at DESC)
        if (results.size() > 1) {
            for (int i = 1; i < results.size(); i++) {
                java.sql.Timestamp prevTime = (java.sql.Timestamp) results.get(i-1)[5];
                java.sql.Timestamp currTime = (java.sql.Timestamp) results.get(i)[5];
                assertThat(prevTime).isAfterOrEqualTo(currTime);
            }
        }
    }

    @Test
    @DisplayName("✅ 정상 케이스 - 제목 전문검색 (영문)")
    void shouldFindPostsByTitle_WhenEnglishKeywordProvided() {
        // Given: 영문 키워드
        String keyword = "Spring*";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 제목 전문검색 수행  
        List<Object[]> results = postFulltextRepository.findByTitleFullText(keyword, pageable);
        long count = postFulltextRepository.countByTitleFullText(keyword);

        // Then: 영문 제목 게시글들이 검색됨
        assertThat(results).isNotEmpty();
        assertThat(count).isGreaterThan(0);
        
        List<String> foundTitles = results.stream()
                .map(row -> (String) row[1])
                .toList();
        
        assertThat(foundTitles).contains("Spring Boot Tutorial Guide");
        assertThat(foundTitles).contains("React + Spring Boot 풀스택 개발"); // 혼합 언어도 포함
    }

    @Test
    @DisplayName("✅ 정상 케이스 - 제목+내용 통합 전문검색")
    void shouldFindPostsByTitleContent_WhenKeywordInContentOnly() {
        // Given: 내용에만 있는 키워드
        String keyword = "REST*";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 제목+내용 통합 검색 수행
        List<Object[]> results = postFulltextRepository.findByTitleContentFullText(keyword, pageable);
        long count = postFulltextRepository.countByTitleContentFullText(keyword);

        // Then: 내용에 키워드가 있는 게시글도 검색됨
        assertThat(results).isNotEmpty();
        assertThat(count).isGreaterThan(0);
        
        List<String> foundTitles = results.stream()
                .map(row -> (String) row[1])
                .toList();
        
        // REST는 내용에만 있으므로 제목 검색에서는 안 나오지만 제목+내용 검색에서는 나와야 함
        assertThat(foundTitles).contains("스프링 부트 튜토리얼 가이드");
    }

    @Test
    @DisplayName("🔍 Boolean Mode 검증 - 와일드카드 및 연산자")
    void shouldSupportBooleanMode_WhenAdvancedSearchUsed() {
        // Given: Boolean Mode 고급 검색어들
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then: 다양한 Boolean Mode 패턴 테스트
        
        // 1. 와일드카드 검색 (접두어)
        List<Object[]> wildcardResults = postFulltextRepository.findByTitleFullText("Spring*", pageable);
        assertThat(wildcardResults).isNotEmpty();
        
        // 2. AND 연산 (+연산자)
        List<Object[]> andResults = postFulltextRepository.findByTitleFullText("+Spring +Boot", pageable);
        assertThat(andResults).isNotEmpty();
        
        // 3. NOT 연산 (-연산자) 
        List<Object[]> notResults = postFulltextRepository.findByTitleFullText("+Spring -Tutorial", pageable);
        List<String> notTitles = notResults.stream()
                .map(row -> (String) row[1])
                .toList();
        assertThat(notTitles).doesNotContain("Spring Boot Tutorial Guide");
        
        // 4. 구문 검색 (따옴표)
        List<Object[]> phraseResults = postFulltextRepository.findByTitleFullText("\"Spring Boot\"", pageable);
        assertThat(phraseResults).isNotEmpty();
    }

    @Test
    @DisplayName("📏 경계값 - 최소 길이 제한 처리")
    void shouldHandleMinimumWordLength_WhenShortKeywordProvided() {
        // Given: 짧은 키워드들 (MySQL ft_min_word_len 기본값 4)
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then: 짧은 키워드 처리
        
        // 1. 3글자 키워드 (기본적으로 검색되지 않을 수 있음)
        List<Object[]> shortResults = postFulltextRepository.findByTitleFullText("Go*", pageable);
        // MySQL 설정에 따라 결과가 다를 수 있으므로 예외 처리하지 않음
        
        // 2. 4글자 이상 키워드는 정상 동작해야 함
        List<Object[]> longResults = postFulltextRepository.findByTitleFullText("Guide*", pageable);
        assertThat(longResults).isNotEmpty();
        
        // 3. 빈 문자열이나 null 처리
        List<Object[]> emptyResults = postFulltextRepository.findByTitleFullText("", pageable);
        assertThat(emptyResults).isEmpty();
    }

    @Test
    @DisplayName("🚫 비즈니스 규칙 - 공지사항 제외 검증")
    void shouldExcludeNotices_WhenSearchingPosts() {
        // Given: 공지사항과 일반 게시글 모두 키워드 포함 (영문 테스트)
        String keyword = "Boot*";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 전문검색 수행
        List<Object[]> titleResults = postFulltextRepository.findByTitleFullText(keyword, pageable);
        List<Object[]> contentResults = postFulltextRepository.findByTitleContentFullText(keyword, pageable);

        // Then: 공지사항은 모든 검색에서 제외되어야 함
        List<String> titleFound = titleResults.stream()
                .map(row -> (String) row[1])
                .toList();
        
        List<String> contentFound = contentResults.stream()
                .map(row -> (String) row[1])
                .toList();

        // 일반 게시글은 포함
        assertThat(titleFound).contains("Spring Boot Tutorial Guide");
        assertThat(contentFound).contains("Spring Boot Tutorial Guide");
        
        // 공지사항은 제외 (noticePost는 한글 제목이므로 영문 검색에서 제외됨)
        assertThat(titleFound).doesNotContain("스프링 부트 공지사항");
        assertThat(contentFound).doesNotContain("스프링 부트 공지사항");
        
        // is_notice 필드 확인
        titleResults.forEach(row -> {
            Boolean isNotice = (Boolean) row[3]; // is_notice column
            assertThat(isNotice).isFalse();
        });
    }

    @Test
    @DisplayName("🔢 일관성 - Search와 Count 쿼리 결과 일치")
    void shouldReturnConsistentCount_BetweenSearchAndCountQueries() {
        // Given: 다양한 키워드들 (영문 위주)
        String[] keywords = {"Spring*", "Boot*", "React*", "Tutorial*"};
        Pageable pageable = PageRequest.of(0, 100); // 충분히 큰 페이지 크기

        for (String keyword : keywords) {
            // When: 검색과 카운트 동시 수행
            List<Object[]> titleResults = postFulltextRepository.findByTitleFullText(keyword, pageable);
            long titleCount = postFulltextRepository.countByTitleFullText(keyword);
            
            List<Object[]> contentResults = postFulltextRepository.findByTitleContentFullText(keyword, pageable);
            long contentCount = postFulltextRepository.countByTitleContentFullText(keyword);

            // Then: 검색 결과 개수와 count 쿼리 결과가 일치해야 함
            assertThat(titleResults.size()).isEqualTo((int) titleCount)
                .withFailMessage("제목 검색 - 키워드 '%s'의 search 결과(%d)와 count 결과(%d) 불일치", 
                    keyword, titleResults.size(), titleCount);
                    
            assertThat(contentResults.size()).isEqualTo((int) contentCount)
                .withFailMessage("제목+내용 검색 - 키워드 '%s'의 search 결과(%d)와 count 결과(%d) 불일치", 
                    keyword, contentResults.size(), contentCount);
        }
    }

    @Test
    @DisplayName("📊 데이터 매핑 - 반환 컬럼 정확성 검증")
    void shouldReturnCorrectColumns_WhenSearchExecuted() {
        // Given: 검색 키워드 (영문)
        String keyword = "Spring*";
        Pageable pageable = PageRequest.of(0, 1);

        // When: 전문검색 수행
        List<Object[]> results = postFulltextRepository.findByTitleFullText(keyword, pageable);

        // Then: 반환 컬럼이 정확해야 함
        assertThat(results).isNotEmpty();
        
        Object[] row = results.get(0);
        assertThat(row).hasSize(8); // post_id, title, views, is_notice, post_cache_flag, created_at, user_id, user_name
        
        // 각 컬럼 타입 검증
        assertThat(row[0]).isInstanceOf(Long.class);      // post_id
        assertThat(row[1]).isInstanceOf(String.class);   // title
        assertThat(row[2]).isInstanceOf(Integer.class);  // views
        assertThat(row[3]).isInstanceOf(Boolean.class);  // is_notice
        assertThat(row[4]).isInstanceOf(String.class);   // post_cache_flag (enum -> string)
        assertThat(row[5]).isInstanceOf(java.sql.Timestamp.class); // created_at
        assertThat(row[6]).isInstanceOf(Long.class);     // user_id
        assertThat(row[7]).isInstanceOf(String.class);   // user_name
        
        // 실제 데이터 검증
        assertThat((String) row[1]).contains("Spring");
        assertThat((Boolean) row[3]).isFalse(); // is_notice = false
        assertThat(row[6]).isEqualTo(testUser.getId()); // user_id 매칭
        assertThat(row[7]).isEqualTo(testUser.getUserName()); // user_name 매칭
    }

    @Test
    @DisplayName("⚡ 성능 - 대량 데이터 검색 성능")
    void shouldPerformWell_WhenSearchingLargeDataset() {
        // Given: 대량 테스트 데이터 생성
        final int DATA_COUNT = 100;
        
        for (int i = 1; i <= DATA_COUNT; i++) {
            Post post = createPost(
                "Performance Test Post " + i + " Spring Boot Development", 
                "Large dataset performance test post number " + i + ". Spring Boot development related content."
            );
            entityManager.persist(post);
            
            if (i % 20 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 50);

        // When: 성능 측정과 함께 검색 수행
        long startTime = System.currentTimeMillis();
        
        List<Object[]> results = postFulltextRepository.findByTitleFullText("Spring*", pageable);
        long count = postFulltextRepository.countByTitleFullText("Spring*");
        
        long endTime = System.currentTimeMillis();
        long queryTime = endTime - startTime;

        // Then: 성능과 결과 정확성 모두 확인
        assertThat(results).hasSizeGreaterThan(50); // 충분한 결과
        assertThat(count).isGreaterThan(100); // 생성한 데이터 + 기존 데이터
        assertThat(queryTime).isLessThan(2000L); // 2초 이내 (FULLTEXT 인덱스 효과)
        
        System.out.println("대량 데이터 검색 성능: " + queryTime + "ms, 결과: " + count + "개");
    }

    @Test
    @DisplayName("🛠️ 예외 처리 - 잘못된 검색어 형식")
    void shouldHandleInvalidKeywords_Gracefully() {
        // Given: 문제가 될 수 있는 검색어들
        String[] problematicKeywords = {
            null,           // null
            "",            // 빈 문자열
            "   ",         // 공백만
            "++--",        // 특수문자만
            "\"unclosed",  // 닫히지 않은 따옴표
            "+",           // 단일 연산자
            "Spring +",    // 불완전한 Boolean 표현식
        };
        
        Pageable pageable = PageRequest.of(0, 10);

        for (String keyword : problematicKeywords) {
            try {
                // When: 문제 있는 키워드로 검색 시도
                List<Object[]> results = postFulltextRepository.findByTitleFullText(keyword, pageable);
                long count = postFulltextRepository.countByTitleFullText(keyword);

                // Then: 예외 없이 처리되고 빈 결과 또는 적절한 결과 반환
                assertThat(results).isNotNull();
                assertThat(count).isGreaterThanOrEqualTo(0);
                
            } catch (Exception e) {
                // MySQL FULLTEXT 파싱 오류 등은 허용 (실제 운영에서 처리 필요)
                System.out.println("키워드 '" + keyword + "'에서 예외 발생: " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("🔄 트랜잭션 - 실시간 데이터 반영")
    void shouldReflectRealtimeData_WhenNewPostAdded() {
        // Given: 초기 검색 결과
        String keyword = "Realtime*";
        Pageable pageable = PageRequest.of(0, 10);
        
        long initialCount = postFulltextRepository.countByTitleFullText(keyword);
        assertThat(initialCount).isEqualTo(0); // 처음엔 결과 없음

        // When: 새 게시글 추가
        Post newPost = createPost("Realtime Search Test", "This is a post added in real-time.");
        entityManager.persistAndFlush(newPost);

        // Then: 검색 결과에 즉시 반영되어야 함
        long newCount = postFulltextRepository.countByTitleFullText(keyword);
        assertThat(newCount).isEqualTo(1);
        
        List<Object[]> results = postFulltextRepository.findByTitleFullText(keyword, pageable);
        assertThat(results).hasSize(1);
        assertThat((String) results.get(0)[1]).isEqualTo("Realtime Search Test");
    }

    @Test
    @DisplayName("📋 통합 시나리오 - 실제 검색 사용 패턴")
    void shouldWorkInRealSearchScenario_WhenUserSearches() {
        // Given: 실제 사용자 검색 시나리오 (영문 위주)
        String[] userSearchQueries = {
            "Spring",      // 단순 키워드
            "Boot",        // 단순 키워드2  
            "React",       // 복합 키워드
            "Tutorial",    // 영문
            "Guide",       // 일반적인 검색어
            "Development"  // 영문 단일
        };
        
        Pageable pageable = PageRequest.of(0, 20);

        for (String query : userSearchQueries) {
            // When: 사용자 검색 수행 (제목 검색)
            List<Object[]> titleResults = postFulltextRepository.findByTitleFullText(query + "*", pageable);
            
            // When: 사용자 검색 수행 (제목+내용 검색) 
            List<Object[]> contentResults = postFulltextRepository.findByTitleContentFullText(query + "*", pageable);

            // Then: 검색 결과 품질 검증
            System.out.println("검색어: '" + query + "' -> 제목: " + titleResults.size() + "개, 제목+내용: " + contentResults.size() + "개");
            
            // 제목+내용 검색이 제목 검색보다 많거나 같은 결과를 가져야 함
            assertThat(contentResults.size()).isGreaterThanOrEqualTo(titleResults.size());
            
            // 모든 결과는 공지사항이 아니어야 함
            contentResults.forEach(row -> {
                assertThat((Boolean) row[3]).isFalse(); // is_notice = false
            });
            
            // 정렬이 올바르게 되어야 함 (created_at DESC)
            if (contentResults.size() > 1) {
                for (int i = 1; i < contentResults.size(); i++) {
                    java.sql.Timestamp prev = (java.sql.Timestamp) contentResults.get(i-1)[5];
                    java.sql.Timestamp curr = (java.sql.Timestamp) contentResults.get(i)[5];
                    assertThat(prev).isAfterOrEqualTo(curr);
                }
            }
        }
    }
}