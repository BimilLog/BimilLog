package jaeik.bimillog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * <h2>데이터 초기화 컨트롤러</h2>
 * <p>
 * 개발 및 테스트 환경에서 샘플 데이터를 초기화하는 컨트롤러
 * </p>
 * <p>
 * data.sql 파일을 실행하여 사용자, 게시글, 댓글, 롤링페이퍼 메시지 등을 생성한다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
public class Datainit {

    @Autowired
    private DataSource dataSource;

    /**
     * <h3>샘플 데이터 초기화</h3>
     * <p>
     * data.sql 스크립트를 실행하여 샘플 데이터를 생성한다.
     * </p>
     * <ul>
     *   <li>사용자 3명 생성 (일반 사용자 2명, 관리자 1명)</li>
     *   <li>각 유형별 게시글 10개씩 생성 (ERROR, IMPROVEMENT, POST/COMMENT 신고)</li>
     *   <li>각 게시글마다 댓글 3-4개씩 생성</li>
     *   <li>게시글 및 댓글 추천 데이터 생성</li>
     *   <li>롤링페이퍼 메시지 각 사용자당 5개씩 생성</li>
     *   <li>각 신고 유형별 신고 데이터 생성</li>
     * </ul>
     *
     * @return 실행 결과 메시지
     * @throws SQLException SQL 실행 중 오류 발생 시
     * @throws IOException 파일 읽기 중 오류 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/data")
    public String initSampleData() throws SQLException, IOException {
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("data.sql"));
            return """
                    ✅ 샘플 데이터 초기화가 완료되었습니다!
                    생성된 데이터:
                    - 사용자 3명 (비밀농부1, 익명작가, 롤링메신저)
                    - 게시글 30개 (ERROR 10개, IMPROVEMENT 10개, 신고 10개)
                    - 댓글 22개 (로그인 사용자 + 비로그인 사용자)
                    - 게시글 추천 11개
                    - 댓글 추천 10개
                    - 롤링페이퍼 메시지 15개
                    - 신고 데이터 12개 (각 유형별)""";
        } catch (Exception e) {
            return "❌ 데이터 초기화 중 오류가 발생했습니다: " + e.getMessage() + e.fillInStackTrace() + e.getLocalizedMessage();
        }
    }

    /**
     * <h3>데이터 초기화 상태 확인</h3>
     * <p>
     * 현재 데이터베이스의 데이터 상태를 확인한다.
     * </p>
     *
     * @return 데이터 상태 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/check-data")
    public String checkDataStatus() {
        try (Connection conn = dataSource.getConnection()) {
            // 간단한 데이터 개수 확인 쿼리들
            String checkQuery = """
                SELECT
                    (SELECT COUNT(*) FROM user) as user_count,
                    (SELECT COUNT(*) FROM post) as post_count,
                    (SELECT COUNT(*) FROM comment) as comment_count,
                    (SELECT COUNT(*) FROM message) as message_count,
                    (SELECT COUNT(*) FROM post_like) as post_like_count,
                    (SELECT COUNT(*) FROM comment_like) as comment_like_count,
                    (SELECT COUNT(*) FROM report) as report_count
                """;
                
            var stmt = conn.createStatement();
            var rs = stmt.executeQuery(checkQuery);
            
            if (rs.next()) {
                return String.format("""
                    📊 현재 데이터베이스 상태:
                    
                    👥 사용자: %d명
                    📝 게시글: %d개
                    💬 댓글: %d개
                    💌 롤링페이퍼 메시지: %d개
                    👍 게시글 추천: %d개
                    💗 댓글 추천: %d개
                    🚨 신고: %d개
                    """, 
                    rs.getInt("user_count"),
                    rs.getInt("post_count"), 
                    rs.getInt("comment_count"),
                    rs.getInt("message_count"),
                    rs.getInt("post_like_count"),
                    rs.getInt("comment_like_count"),
                    rs.getInt("report_count")
                );
            }
            return "데이터 조회 실패";
        } catch (Exception e) {
            return "❌ 데이터 상태 확인 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
}
