package jaeik.bimillog.testutil.fixtures;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.paper.dto.MessageDeleteDTO;
import jaeik.bimillog.domain.paper.dto.MessageWriteDTO;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.post.dto.PostCreateDTO;
import jakarta.persistence.EntityManager;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * <h2>테스트 데이터 Fixtures</h2>
 * <p>테스트에서 자주 사용되는 데이터 생성 유틸리티</p>
 * <p>엔티티, DTO, 인증 객체 등 다양한 테스트 데이터 생성 메서드 제공</p>
 *
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>엔티티 생성 (Post, Comment, Notification, RollingPaper 등)</li>
 *   <li>DTO 생성 (요청/응답 DTO)</li>
 *   <li>인증 객체 생성 (CustomUserDetails, MemberDetail)</li>
 *   <li>쿠키 및 토큰 생성</li>
 *   <li>Member 영속화 헬퍼 메서드</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class TestFixtures {

    /**
     * 게시글 작성 요청 DTO 생성
     * @param title 제목
     * @param content 내용
     * @return PostCreateDTO
     */
    public static PostCreateDTO createPostRequest(String title, String content) {
        return PostCreateDTO.builder()
                .title(title)
                .content(content)
                .password(1234)
                .build();
    }

    /**
     * 롤링페이퍼 메시지 삭제 요청 DTO 생성
     * @param messageId 삭제할 메시지 ID
     * @return MessageDeleteDTO
     */
    public static MessageDeleteDTO createMessageDeleteDTO(Long messageId) {
        return new MessageDeleteDTO(messageId);
    }

    /**
     * 롤링페이퍼 메시지 작성 요청 DTO 생성
     * @param ownerId 롤링페이퍼 소유자 ID
     * @param content 메시지 내용
     * @param positionX X 위치
     * @param positionY Y 위치
     * @return MessageWriteDTO
     */
    public static MessageWriteDTO createMessageWriteDTO(Long ownerId, String content, int positionX, int positionY) {
        MessageWriteDTO dto = new MessageWriteDTO();
        ReflectionTestUtils.setField(dto, "ownerId", ownerId);
        ReflectionTestUtils.setField(dto, "decoType", DecoType.POTATO);
        ReflectionTestUtils.setField(dto, "anonymity", "테스트회원");
        ReflectionTestUtils.setField(dto, "content", content);
        ReflectionTestUtils.setField(dto, "x", positionX);
        ReflectionTestUtils.setField(dto, "y", positionY);
        return dto;
    }


    /**
     * 리플렉션을 통한 private 필드 값 설정 (테스트 전용)
     * @param target 대상 객체
     * @param fieldName 필드명
     * @param value 설정할 값
     */
    public static void setFieldValue(Object target, String fieldName, Object value) {
        ReflectionTestUtils.setField(target, fieldName, value);
    }

    /**
     * Member의 연관 엔티티(Setting, SocialToken)를 먼저 persist한 후 Member를 persist
     * <p>Member 엔티티는 SocialToken에 Cascade 설정이 없어서 수동으로 영속화가 필요합니다.</p>
     * <p>Setting은 Cascade.PERSIST가 있지만, 명시적으로 먼저 persist하여 일관성을 유지합니다.</p>
     *
     * @param em EntityManager
     * @param member 영속화할 Member 엔티티
     */
    public static void persistMemberWithDependencies(EntityManager em, Member member) {
        if (member.getSetting() != null) {
            em.persist(member.getSetting());
        }
        if (member.getSocialToken() != null) {
            em.persist(member.getSocialToken());
        }
        em.persist(member);
    }

}
