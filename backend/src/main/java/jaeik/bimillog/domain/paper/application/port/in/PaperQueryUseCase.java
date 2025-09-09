package jaeik.bimillog.domain.paper.application.port.in;

import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.entity.MessageDetail;
import jaeik.bimillog.domain.paper.entity.VisitMessageDetail;
import jaeik.bimillog.infrastructure.exception.CustomException;

import java.util.List;
import java.util.Optional;

/**
 * <h2>롤링페이퍼 조회 유스케이스</h2>
 * <p>
 * 인바운드 포트: 롤링페이퍼 관련 조회 작업의 추상화된 인터페이스를 정의합니다.
 * 헥사고날 아키텍처에서 외부 요청을 내부 도메인 로직으로 전달하는 계약을 담당합니다.
 * </p>
 * <p>
 * 사용자들이 롤링페이퍼를 통해 소통하는 두 가지 핵심 상황을 지원합니다:
 * </p>
 * <p>
 * 1. 롤링페이퍼 소유자 상황: 마이페이지에서 "내게 온 메시지들을 확인하고 싶어"
 * 2. 메시지 작성자 상황: "친구 롤링페이퍼에 메시지를 남기고 싶은데 어디에 써야 할까?"
 * </p>
 * <p>
 * PaperQueryController에서 웹 요청을 받아 이 포트로 전달하고,
 * PaperQueryService가 구현체로서 실제 비즈니스 로직을 수행하며,
 * Comment 도메인과의 크로스 도메인 통신도 이 포트를 통해 이루어집니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperQueryUseCase {

    /**
     * <h3>내 롤링페이퍼 조회</h3>
     * <p>사용자 ID를 통해 자신의 롤링페이퍼에 작성된 모든 메시지를 조회합니다.</p>
     * <p>
     * 사용자가 생일, 기념일, 시험 등을 앞두고 지인들에게 롤링페이퍼 링크를 공유한 후,
     * 며칠 뒤 "과연 누가 메시지를 남겨줬을까? 어떤 따뜻한 말들이 기다리고 있을까?"라는 궁금함과 설렘으로
     * 마이페이지에 접속해서 "내 롤링페이퍼 보기" 버튼을 클릭하는 순간,
     * PaperQueryController를 통해 호출되는 메서드입니다.
     * 모든 메시지의 내용과 작성자명을 포함한 완전한 정보를 제공하여
     * 사용자가 받은 진심 어린 메시지들을 모두 확인할 수 있게 합니다.
     * </p>
     *
     * @param userId 현재 로그인한 사용자 ID
     * @return 내 롤링페이퍼의 메시지 목록 (완전한 정보 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    List<MessageDetail> getMyPaper(Long userId);

    /**
     * <h3>다른 사용자 롤링페이퍼 방문 조회</h3>
     * <p>다른 사용자의 롤링페이퍼를 방문하여 메시지 목록을 조회합니다.</p>
     * <p>민감한 정보(내용, 작성자명)는 제외하고 장식 정보만 제공합니다.</p>
     * <p>PaperQueryController에서 타 사용자의 롤링페이퍼를 방문하기 위해 호출되는 메서드</p>
     *
     * @param userName 방문할 사용자명
     * @return 방문한 롤링페이퍼의 메시지 목록 (익명화된 정보)
     * @throws CustomException 사용자가 존재하지 않는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    List<VisitMessageDetail> visitPaper(String userName);

    /**
     * <h3>메시지 ID로 메시지 조회</h3>
     * <p>주어진 메시지 ID에 해당하는 메시지를 조회합니다.</p>
     * <p>Comment 도메인에서 댓글이 속한 메시지 정보를 확인하기 위해 호출되는 메서드</p>
     *
     * @param messageId 조회할 메시지의 ID
     * @return 메시지 엔티티 (Optional)
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Message> findMessageById(Long messageId);
}