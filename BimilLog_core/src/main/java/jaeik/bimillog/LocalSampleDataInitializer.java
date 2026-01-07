package jaeik.bimillog;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.repository.MemberRepository;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * <h2>로컬 샘플 데이터 초기화</h2>
 * <p>
 * 로컬 프로필에서 애플리케이션 기동 시 data.sql 스크립트를 한 번 실행한다.
 * </p>
 */
@Component
@Profile("local")
public class LocalSampleDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalSampleDataInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;

    public LocalSampleDataInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate,
                                      MessageRepository messageRepository, MemberRepository memberRepository) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.messageRepository = messageRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long postCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post", Long.class);
        if (postCount != null && postCount > 0) {
            log.info("[LocalSampleDataInitializer] 샘플 데이터가 이미 존재하여 초기화를 건너뜁니다.");
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
            log.info("[LocalSampleDataInitializer] data.sql 스크립트 실행이 완료되었습니다.");

            // 메시지 데이터는 JPA를 통해 생성하여 자동 암호화 적용
            createMessages();
            log.info("[LocalSampleDataInitializer] 샘플 데이터 초기화가 완료되었습니다.");
        } catch (Exception ex) {
            log.error("[LocalSampleDataInitializer] 샘플 데이터 초기화 중 오류가 발생했습니다.", ex);
        }
    }

    /**
     * <h3>메시지 더미 데이터 생성</h3>
     * <p>JPA를 통해 메시지를 생성하여 MessageEncryptConverter가 자동으로 암호화를 적용하도록 합니다.</p>
     */
    private void createMessages() {
        List<Message> messages = new ArrayList<>();

        // data.sql에서 생성된 회원 조회
        Member member1 = memberRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Member 1 not found"));
        Member member2 = memberRepository.findById(2L)
                .orElseThrow(() -> new RuntimeException("Member 2 not found"));
        Member member3 = memberRepository.findById(3L)
                .orElseThrow(() -> new RuntimeException("Member 3 not found"));
        Member member4 = memberRepository.findById(4L)
                .orElseThrow(() -> new RuntimeException("Member 4 not found"));

        // Member 2의 메시지 (5개)
        messages.add(Message.createMessage(member2, DecoType.STRAWBERRY, "농부", "안녕하세요! 항상 응원합니다.", 0, 0));
        messages.add(Message.createMessage(member2, DecoType.CARROT, "당근", "좋은 하루 되세요!", 0, 1));
        messages.add(Message.createMessage(member2, DecoType.COFFEE, "커피", "화이팅!", 0, 2));
        messages.add(Message.createMessage(member2, DecoType.CAT, "고양이", "열심히 하세요~", 1, 0));
        messages.add(Message.createMessage(member2, DecoType.STAR, "별빛", "반짝반짝", 1, 1));

        // Member 3의 메시지 (5개)
        messages.add(Message.createMessage(member3, DecoType.APPLE, "사과", "건강하세요!", 0, 3));
        messages.add(Message.createMessage(member3, DecoType.DOG, "멍멍", "멍멍! 좋은 하루!", 0, 4));
        messages.add(Message.createMessage(member3, DecoType.MOON, "달빛", "달빛이 아름다워요", 0, 5));
        messages.add(Message.createMessage(member3, DecoType.BALLOON, "풍선", "풍선처럼 행복하세요", 1, 3));
        messages.add(Message.createMessage(member3, DecoType.SUNFLOWER, "해바라", "해바라기처럼 밝게!", 1, 4));

        // Member 4의 메시지 (5개)
        messages.add(Message.createMessage(member4, DecoType.DRAGON, "용용", "용처럼 힘차게 날아오르세요!", 2, 0));
        messages.add(Message.createMessage(member4, DecoType.ANGEL, "천사", "천사같은 당신 응원해요", 2, 1));
        messages.add(Message.createMessage(member4, DecoType.PHOENIX, "불새", "불사조처럼 다시 일어나세요", 2, 2));
        messages.add(Message.createMessage(member4, DecoType.STAR, "왕관", "당신은 최고입니다", 2, 3));
        messages.add(Message.createMessage(member4, DecoType.RAINBOW, "무지개", "무지개같은 행복을", 2, 4));

        // Member 1의 메시지 (10개) - 다른 사용자들이 작성
        messages.add(Message.createMessage(member1, DecoType.STRAWBERRY, "감사하는사람", "감사합니다! 덕분에 많이 배웠어요.", 0, 5));
        messages.add(Message.createMessage(member1, DecoType.CARROT, "응원합니다", "항상 응원하고 있습니다!", 0, 6));
        messages.add(Message.createMessage(member1, DecoType.COFFEE, "팬입니다", "진심으로 응원합니다", 0, 7));
        messages.add(Message.createMessage(member1, DecoType.CAT, "최고개발자", "최고의 개발자시네요!", 1, 8));
        messages.add(Message.createMessage(member1, DecoType.STAR, "고마워요", "정말 고마워요", 2, 9));
        messages.add(Message.createMessage(member1, DecoType.APPLE, "존경합니다", "존경하는 마음을 전합니다", 3, 5));
        messages.add(Message.createMessage(member1, DecoType.DOG, "항상응원", "항상 응원할게요!", 4, 6));
        messages.add(Message.createMessage(member1, DecoType.MOON, "멋진개발자", "멋진 개발자가 되세요", 5, 7));
        messages.add(Message.createMessage(member1, DecoType.BALLOON, "건강하세요", "건강하세요~", 6, 8));
        messages.add(Message.createMessage(member1, DecoType.SUNFLOWER, "감사드려요", "감사드립니다!", 6, 9));

        messageRepository.saveAll(messages);
        log.info("[LocalSampleDataInitializer] {} 개의 메시지가 생성되었습니다.", messages.size());
    }
}
