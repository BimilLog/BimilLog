package jaeik.bimillog.domain.post.application.port.in;


/**
 * <h2>PostCommandUseCase</h2>
 * <p>
 * Post 도메인의 명령형 비즈니스 로직을 처리하는 인바운드 포트입니다.
 * 헥사고날 아키텍처에서 CQRS 패턴의 Command 측면을 담당하며, 게시글의 상태 변경이 수반되는 모든 작업을 처리합니다.
 * </p>
 * <p>
 * 이 유스케이스는 게시글의 생명주기 전반을 관리합니다:
 * - 게시글 작성: 새로운 게시글 생성 및 저장
 * - 게시글 수정: 기존 게시글의 제목과 내용 변경
 * - 게시글 삭제: 게시글 제거 및 관련 데이터 정리
 * - 권한 검증: 수정과 삭제 작업에서 작성자 권한 확인
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 포트가 필요한 이유:
 * 1. 콘텐츠 관리 - 사용자의 게시글 작성, 수정, 삭제 니즈 충족
 * 2. 데이터 무결성 - 게시글 작업 시 관련 데이터의 일관성 보장
 * 3. 비즈니스 규칙 준수 - 작성자만 수정/삭제 가능한 규칙 적용
 * 4. 이벤트 발행 - 게시글 삭제 시 댓글 정리 등 연관 작업 트리거
 * </p>
 * <p>
 * PostCommandController에서 게시글 작성/수정/삭제 API 제공 시 호출됩니다.
 * 웹 페이지에서 사용자의 게시글 관리 요청을 처리할 때 사용됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCommandUseCase {

    /**
     * <h3>새로운 게시글 작성 및 등록</h3>
     * <p>사용자가 입력한 내용으로 새로운 게시글을 생성하고 데이터베이스에 저장합니다.</p>
     * <p>게시글 엔티티를 생성하며 작성자, 제목, 내용, 선택적 비밀번호를 설정합니다.</p>
     * <p>PostCommandController에서 게시글 작성 요청을 처리할 때 호출됩니다.</p>
     * <p>게시글 작성 후 반환된 ID는 상세 페이지 리다이렉트에 사용됩니다.</p>
     *
     * @param userId   게시글 작성자의 사용자 ID
     * @param title    게시글 제목
     * @param content  게시글 본문 내용 (HTML 태그 포함 가능)
     * @param password 게시글 단독 비밀번호 (선택사항, null 가능)
     * @return Long 생성된 게시글의 식별자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    Long writePost(Long userId, String title, String content, Integer password);

    /**
     * <h3>기존 게시글 내용 수정</h3>
     * <p>게시글 작성자만이 자신의 게시글을 수정할 수 있도록 권한을 검증합니다.</p>
     * <p>작성자 확인 후 게시글의 제목과 내용을 새로운 값으로 업데이트합니다.</p>
     * <p>PostCommandController에서 게시글 수정 요청을 처리할 때 호출됩니다.</p>
     * <p>게시글 수정 시 수정일시가 자동으로 업데이트됩니다.</p>
     *
     * @param userId  수정을 요청한 사용자의 ID (작성자 검증용)
     * @param postId  수정대상 게시글의 식별자 ID
     * @param title   새로운 게시글 제목
     * @param content 새로운 게시글 본문 내용
     * @author Jaeik
     * @since 2.0.0
     */
    void updatePost(Long userId, Long postId, String title, String content);

    /**
     * <h3>게시글 영구 삭제 및 연관 데이터 정리</h3>
     * <p>게시글 작성자만이 자신의 게시글을 삭제할 수 있도록 권한을 검증합니다.</p>
     * <p>작성자 확인 후 게시글을 데이터베이스에서 영구 제거하고 관련 데이터 정리를 수행합니다.</p>
     * <p>PostCommandController에서 게시글 삭제 요청을 처리할 때 호출됩니다.</p>
     * <p>게시글 삭제 시 PostDeleteEvent를 발행하여 댓글, 추천 등 연관 데이터 정리를 트리거합니다.</p>
     *
     * @param userId 삭제를 요청한 사용자의 ID (작성자 검증용)
     * @param postId 삭제대상 게시글의 식별자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deletePost(Long userId, Long postId);
}
