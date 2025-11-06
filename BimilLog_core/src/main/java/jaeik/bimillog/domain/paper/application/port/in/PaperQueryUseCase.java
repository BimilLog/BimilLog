package jaeik.bimillog.domain.paper.application.port.in;

import jaeik.bimillog.domain.paper.entity.MessageDetail;
import jaeik.bimillog.domain.paper.entity.VisitMessageDetail;
import jaeik.bimillog.domain.paper.in.web.PaperQueryController;
import jaeik.bimillog.infrastructure.exception.CustomException;

import java.util.List;

/**
 * <h2>롤링페이퍼 조회 유스케이스</h2>
 * <p>롤링페이퍼 도메인의 조회 작업을 담당하는 유스케이스입니다.</p>
 * <p>내 롤링페이퍼 조회, 타인 롤링페이퍼 방문</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperQueryUseCase {

    /**
     * <h3>내 롤링페이퍼 조회</h3>
     * <p>사용자 ID를 통해 자신의 롤링페이퍼에 작성된 모든 메시지를 조회합니다.</p>
     * <p>모든 메시지의 내용과 작성자명을 포함한 완전한 정보를 제공합니다.</p>
     * <p>{@link PaperQueryController}에서 내 롤링페이퍼 조회 요청 시 호출됩니다.</p>
     *
     * @param memberId 현재 로그인한 사용자 ID
     * @return 내 롤링페이퍼의 메시지 목록 (완전한 정보 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    List<MessageDetail> getMyPaper(Long memberId);

    /**
     * <h3>다른 사용자 롤링페이퍼 방문 조회</h3>
     * <p>다른 사용자의 롤링페이퍼를 방문하여 메시지 목록을 조회합니다.</p>
     * <p>민감한 정보(내용, 작성자명)는 제외하고 장식 정보만 제공합니다.</p>
     * <p>{@link PaperQueryController}에서 타 사용자의 롤링페이퍼 방문 요청 시 호출됩니다.</p>
     *
     * @param memberName 방문할 사용자명
     * @return 방문한 롤링페이퍼의 메시지 목록 (익명화된 정보)
     * @throws CustomException 사용자가 존재하지 않는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    List<VisitMessageDetail> visitPaper(String memberName);
}