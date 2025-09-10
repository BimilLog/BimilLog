package jaeik.bimillog.domain.paper.application.port.out;

import jaeik.bimillog.domain.paper.application.service.PaperCommandService;
import jaeik.bimillog.domain.paper.application.service.PaperQueryService;
import jaeik.bimillog.domain.user.entity.User;

import java.util.Optional;

/**
 * <h2>Paper에서 User 도메인 접근 포트</h2>
 * <p>Paper 도메인에서 User 도메인의 데이터에 접근하기 위한 포트입니다.</p>
 * <p>사용자 조회, 사용자 존재성 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperToUserPort {

    /**
     * <h3>사용자 이름으로 사용자 조회</h3>
     * <p>특정 사용자명에 해당하는 사용자 엔티티를 조회합니다.</p>
     * <p>롤링페이퍼 방문 시 대상 사용자의 존재성을 확인하고 사용자 정보를 제공하기 위해 사용됩니다.</p>
     * <p>{@link PaperCommandService}에서 메시지 작성 전 대상 사용자 검증 시 호출됩니다.</p>
     *
     * @param userName 조회할 사용자의 이름
     * @return Optional<User> 사용자 엔티티 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<User> findByUserName(String userName);

    /**
     * <h3>사용자 이름 존재 여부 확인</h3>
     * <p>특정 사용자명이 시스템에 존재하는지 여부를 확인합니다.</p>
     * <p>전체 엔티티 조회 없이 존재성만 빠르게 확인하기 위해 사용됩니다.</p>
     * <p>{@link PaperQueryService}에서 롤링페이퍼 접근 전 사용자 존재성 간단 검증 시 호출됩니다.</p>
     *
     * @param userName 확인할 사용자의 이름
     * @return boolean 사용자 이름이 존재하면 true, 그렇지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByUserName(String userName);
}