package jaeik.bimillog.e2e.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

/**
 * 테스트 데이터 생성 유틸리티
 * E2E 테스트에서 사용할 고유한 테스트 데이터 생성
 */
public class TestDataGenerator {

    private static final Random random = new Random();
    private static final String[] KOREAN_FIRST_NAMES = {
        "민수", "지영", "서연", "준호", "하은", "도윤", "서준", "예은", "시우", "지우"
    };
    private static final String[] ADJECTIVES = {
        "행복한", "즐거운", "신나는", "멋진", "예쁜", "귀여운", "용감한", "지혜로운", "따뜻한", "시원한"
    };
    private static final String[] NOUNS = {
        "고양이", "강아지", "토끼", "햄스터", "앵무새", "거북이", "물고기", "나무", "꽃", "별"
    };

    /**
     * 고유한 타임스탬프 생성
     */
    public static String generateTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
    }

    /**
     * 고유한 UUID 생성
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 테스트용 닉네임 생성
     */
    public static String generateNickname() {
        String adjective = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[random.nextInt(NOUNS.length)];
        String number = String.valueOf(random.nextInt(1000));
        return adjective + noun + number;
    }

    /**
     * 테스트용 한국 이름 생성
     */
    public static String generateKoreanName() {
        return KOREAN_FIRST_NAMES[random.nextInt(KOREAN_FIRST_NAMES.length)] + "_" + generateUUID();
    }

    /**
     * 테스트용 이메일 생성
     */
    public static String generateEmail() {
        return "e2e_test_" + generateUUID() + "@test.com";
    }

    /**
     * 테스트용 게시글 제목 생성
     */
    public static String generatePostTitle() {
        String[] templates = {
            "E2E 테스트 게시글 - %s",
            "자동 생성 게시글 [%s]",
            "테스트 제목: %s",
            "[테스트] %s 게시글입니다"
        };
        String template = templates[random.nextInt(templates.length)];
        return String.format(template, generateTimestamp());
    }

    /**
     * 테스트용 게시글 내용 생성
     */
    public static String generatePostContent() {
        StringBuilder content = new StringBuilder();
        content.append("이것은 E2E 테스트를 위해 자동으로 생성된 게시글입니다.\n\n");
        content.append("생성 시간: ").append(LocalDateTime.now()).append("\n");
        content.append("고유 ID: ").append(generateUUID()).append("\n\n");

        // 랜덤 문단 추가
        String[] paragraphs = {
            "테스트 자동화는 소프트웨어 품질을 보장하는 중요한 과정입니다.",
            "Playwright를 사용하여 브라우저 자동화 테스트를 수행하고 있습니다.",
            "이 게시글은 테스트가 완료되면 자동으로 정리될 예정입니다.",
            "E2E 테스트는 실제 사용자의 시나리오를 시뮬레이션합니다."
        };

        for (int i = 0; i < 2 + random.nextInt(3); i++) {
            content.append(paragraphs[random.nextInt(paragraphs.length)]).append("\n\n");
        }

        return content.toString();
    }

    /**
     * 테스트용 댓글 내용 생성
     */
    public static String generateCommentContent() {
        String[] templates = {
            "테스트 댓글입니다. ID: %s",
            "자동 생성된 댓글 - %s",
            "E2E 테스트 댓글 [%s]",
            "댓글 테스트: %s"
        };
        String template = templates[random.nextInt(templates.length)];
        return String.format(template, generateUUID());
    }

    /**
     * 테스트용 롤링페이퍼 메시지 생성
     */
    public static String generateRollingPaperMessage() {
        String[] messages = {
            "테스트 메시지입니다! 항상 행복하세요 😊",
            "E2E 테스트로 작성된 롤링페이퍼 메시지입니다.",
            "자동화 테스트 메시지 - " + generateTimestamp(),
            "익명의 응원 메시지를 보냅니다!",
            "테스트지만 진심을 담아 작성합니다 ❤️"
        };
        return messages[random.nextInt(messages.length)];
    }

    /**
     * 테스트용 비밀번호 생성
     */
    public static String generatePassword() {
        return "Test@" + generateUUID();
    }

    /**
     * 테스트용 검색어 생성
     */
    public static String generateSearchQuery() {
        String[] queries = {
            "테스트",
            "E2E",
            "자동화",
            "Playwright",
            "게시글"
        };
        return queries[random.nextInt(queries.length)];
    }

    /**
     * 테스트용 URL 슬러그 생성
     */
    public static String generateSlug() {
        return "test-" + generateUUID().toLowerCase();
    }

    /**
     * 랜덤 지연 시간 생성 (밀리초)
     */
    public static int generateRandomDelay() {
        return 500 + random.nextInt(1500); // 500ms ~ 2000ms
    }

    /**
     * 테스트 데이터 접두사 생성
     */
    public static String withTestPrefix(String data) {
        return "E2E_TEST_" + data;
    }

    /**
     * 테스트 완료 후 정리해야 할 데이터인지 확인
     */
    public static boolean isTestData(String data) {
        return data != null && (
            data.startsWith("E2E_TEST_") ||
            data.startsWith("e2e_test_") ||
            data.contains("E2E 테스트") ||
            data.contains("자동 생성")
        );
    }

    /**
     * 랜덤 색상 코드 생성 (HEX)
     */
    public static String generateColorHex() {
        return String.format("#%06X", random.nextInt(0xFFFFFF + 1));
    }

    /**
     * 랜덤 이모지 생성
     */
    public static String generateEmoji() {
        String[] emojis = {"😊", "🎉", "❤️", "👍", "⭐", "🌟", "💖", "🎈", "🌈", "✨"};
        return emojis[random.nextInt(emojis.length)];
    }

    /**
     * 파일 이름 생성
     */
    public static String generateFileName(String extension) {
        return "test_file_" + generateTimestamp() + "." + extension;
    }

    /**
     * 테스트용 태그 생성
     */
    public static String[] generateTags(int count) {
        String[] allTags = {"테스트", "E2E", "자동화", "Playwright", "개발", "QA", "버그", "기능", "성능", "보안"};
        String[] result = new String[Math.min(count, allTags.length)];

        for (int i = 0; i < result.length; i++) {
            result[i] = allTags[random.nextInt(allTags.length)];
        }

        return result;
    }
}