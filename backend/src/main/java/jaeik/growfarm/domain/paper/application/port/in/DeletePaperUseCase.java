package jaeik.growfarm.domain.paper.application.port.in;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;

/**
 * <h2>롤링페이퍼 삭제 유스케이스</h2>
 * <p>
 * Primary Port: 롤링페이퍼 메시지 삭제 관련 유스케이스를 정의
 * 기존 PaperDeleteService의 모든 기능을 포함
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
public interface DeletePaperUseCase {

    /**
     * <h3>내 롤링페이퍼 메시지 삭제</h3>
     * <p>
     * 기존 PaperDeleteService.deleteMessageInMyPaper() 메서드와 동일한 기능
     * - 메시지 소유권 검증 (userId 일치 확인)
     * - 메시지 삭제 수행
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param messageDTO  삭제할 메시지 정보
     * @throws jaeik.growfarm.global.exception.CustomException 삭제 권한이 없는 경우 (MESSAGE_DELETE_FORBIDDEN)
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteMessageInMyPaper(CustomUserDetails userDetails, MessageDTO messageDTO);
}