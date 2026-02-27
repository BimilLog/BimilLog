//package jaeik.bimillog.springboot.mysql.performance;
//
//import org.junit.jupiter.api.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.redis.core.Cursor;
//import org.springframework.data.redis.core.ScanOptions;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.test.context.ActiveProfiles;
//
//import javax.sql.DataSource;
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicLong;
//
///**
// *
// * 데이터 규모:
// *   setting    :  100,000 rows
// *   member     :  100,000 rows
// *   post       :  100,000 rows
// *   friendship :   15,000,000 rows (100K × 150)
// *   post_like  :   10,000,000 rows (100K × 100) — 글 추천
// *   comment    :   10,000,000 rows (100K × 100) — 댓글 작성
// *   comment_like:  10,000,000 rows (100K × 100) — 댓글 추천
// *
// * 실행: LOCAL_MYSQL_PASSWORD 변수 gradlew localIntegrationTest --tests "*.friendPerformanceSeedRunner"
// */
//@DisplayName("친구 성능 테스트 시드 러너")
//@SpringBootTest(properties = {
//        "spring.task.scheduling.enabled=false",
//        "spring.scheduling.enabled=false"
//})
//@Tag("local-integration")
//@Tag("performance")
//@ActiveProfiles("local-integration")
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//public class friendPerformanceSeedRunner {
//
//    private static final int MEMBER_COUNT             = 100_000;
//    private static final int FRIENDS_PER_MEMBER       = 300;   // friendship rows ≈ 100K × 150 = 15M
//    private static final int LIKE_PER_MEMBER          = 100;   // post_like rows  = 100K × 100 = 10M
//    private static final int COMMENT_PER_MEMBER       = 100;   // comment rows    = 100K × 100 = 10M
//    private static final int COMMENT_LIKE_PER_MEMBER  = 100;   // comment_like rows = 100K × 100 = 10M
//    private static final int JDBC_BATCH_SIZE          = 10_000;
//
//    private static final String DIGITS =
//            "(SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 " +
//            " UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9)";
//    private static final String NUM_GEN =
//            "(SELECT a.n+b.n*10+c.n*100+d.n*1000+e.n*10000+1 AS seq " +
//            "FROM " + DIGITS + " a CROSS JOIN " + DIGITS + " b CROSS JOIN " + DIGITS + " c " +
//            "CROSS JOIN " + DIGITS + " d CROSS JOIN " + DIGITS + " e) nums";
//
//    private static final Logger log = LoggerFactory.getLogger(friendPerformanceSeedRunner.class);
//
//    private long memberStart;
//    private long postStart;
//    private long commentStart;
//
//    @Autowired private DataSource dataSource;
//    @Autowired private StringRedisTemplate stringRedisTemplate;
//
//    // ─────────────────────────────────────────────────────────────
//
//    @Test
//    @DisplayName("시드 데이터 준비")
//    void seed() throws Exception {
//        log.info("=== 성능 테스트 데이터 준비 시작 ===");
//
//        log.info("▶ Step 0/7: 기존 데이터 초기화...");
//        long t0 = System.currentTimeMillis();
//        deleteRedisByPattern("friend:*");
//        deleteRedisByPattern("interaction:*");
//        cleanPerfData();
//        log.info("  초기화 완료 {}ms", System.currentTimeMillis() - t0);
//
//        try (Connection conn = dataSource.getConnection()) {
//            conn.setAutoCommit(false);
//
//            log.info("▶ Step 1/7: setting {} rows 삽입...", fmt(MEMBER_COUNT));
//            long t = System.currentTimeMillis();
//            insertSettings(conn);
//            log.info("  완료 {}ms", System.currentTimeMillis() - t);
//
//            log.info("▶ Step 2/7: member {} rows 삽입...", fmt(MEMBER_COUNT));
//            t = System.currentTimeMillis();
//            insertMembers(conn);
//            log.info("  완료 {}ms  memberStart={}", System.currentTimeMillis() - t, memberStart);
//
//            log.info("▶ Step 3/7: post {} rows 삽입...", fmt(MEMBER_COUNT));
//            t = System.currentTimeMillis();
//            insertPosts(conn);
//            log.info("  완료 {}ms  postStart={}", System.currentTimeMillis() - t, postStart);
//
//            log.info("▶ Step 4/7: friendship ~15,000,000 rows 삽입...");
//            t = System.currentTimeMillis();
//            long inserted = insertFriendships(conn);
//            log.info("  완료 {}ms  삽입={}rows", System.currentTimeMillis() - t, fmt(inserted));
//
//            log.info("▶ Step 5/7: post_like 10,000,000 rows 삽입 (글 추천)...");
//            t = System.currentTimeMillis();
//            inserted = insertPostLikes(conn);
//            log.info("  완료 {}ms  삽입={}rows", System.currentTimeMillis() - t, fmt(inserted));
//
//            log.info("▶ Step 6/7: comment 10,000,000 rows 삽입 (댓글 작성)...");
//            t = System.currentTimeMillis();
//            inserted = insertComments(conn);
//            log.info("  완료 {}ms  삽입={}rows  commentStart={}", System.currentTimeMillis() - t, fmt(inserted), commentStart);
//
//            log.info("▶ Step 7/7: comment_like 10,000,000 rows 삽입 (댓글 추천)...");
//            t = System.currentTimeMillis();
//            inserted = insertCommentLikes(conn);
//            log.info("  완료 {}ms  삽입={}rows", System.currentTimeMillis() - t, fmt(inserted));
//
//            conn.commit();
//        }
//
//        log.info("=== 데이터 준비 완료 ===");
//    }
//
//    // ─────────────────────────────────────────────────────────────
//    //  초기화
//    // ─────────────────────────────────────────────────────────────
//
//    private void cleanPerfData() throws SQLException {
//        try (Connection conn = dataSource.getConnection()) {
//            conn.setAutoCommit(false);
//
//            // PerfUser member 범위 조회
//            long minMemberId, maxMemberId;
//            try (Statement stmt = conn.createStatement();
//                 ResultSet rs = stmt.executeQuery(
//                         "SELECT MIN(member_id), MAX(member_id) FROM member WHERE member_name LIKE 'PerfUser\\_%'")) {
//                rs.next();
//                if (rs.getObject(1) == null) {
//                    log.info("  PerfUser 데이터 없음, 초기화 스킵");
//                    return;
//                }
//                minMemberId = rs.getLong(1);
//                maxMemberId = rs.getLong(2);
//            }
//
//            // post 범위 조회
//            long minPostId = 0, maxPostId = 0;
//            try (Statement stmt = conn.createStatement();
//                 ResultSet rs = stmt.executeQuery(
//                         "SELECT MIN(post_id), MAX(post_id) FROM post WHERE member_name LIKE 'PerfUser\\_%'")) {
//                rs.next();
//                if (rs.getObject(1) != null) {
//                    minPostId = rs.getLong(1);
//                    maxPostId = rs.getLong(2);
//                }
//            }
//
//            // comment 범위 조회
//            long minCommentId = 0, maxCommentId = 0;
//            try (Statement stmt = conn.createStatement();
//                 ResultSet rs = stmt.executeQuery(
//                         "SELECT MIN(comment_id), MAX(comment_id) FROM comment " +
//                         "WHERE member_id BETWEEN " + minMemberId + " AND " + maxMemberId)) {
//                rs.next();
//                if (rs.getObject(1) != null) {
//                    minCommentId = rs.getLong(1);
//                    maxCommentId = rs.getLong(2);
//                }
//            }
//
//            // FK 체크 비활성화 후 일괄 삭제
//            try (Statement stmt = conn.createStatement()) {
//                stmt.execute("SET foreign_key_checks = 0");
//            }
//
//            if (minCommentId > 0) {
//                exec(conn, "DELETE FROM comment_like WHERE comment_id BETWEEN " + minCommentId + " AND " + maxCommentId, "comment_like");
//                exec(conn, "DELETE FROM comment_closure WHERE descendant_id BETWEEN " + minCommentId + " AND " + maxCommentId, "comment_closure");
//                exec(conn, "DELETE FROM comment WHERE comment_id BETWEEN " + minCommentId + " AND " + maxCommentId, "comment");
//            }
//            exec(conn, "DELETE FROM post_like WHERE member_id BETWEEN " + minMemberId + " AND " + maxMemberId, "post_like");
//            exec(conn, "DELETE FROM friendship WHERE member_id BETWEEN " + minMemberId + " AND " + maxMemberId, "friendship");
//            if (minPostId > 0) {
//                exec(conn, "DELETE FROM post WHERE post_id BETWEEN " + minPostId + " AND " + maxPostId, "post");
//            }
//
//            // setting ID 수집 후 member, setting 삭제
//            List<Long> settingIds = new ArrayList<>();
//            try (Statement stmt = conn.createStatement();
//                 ResultSet rs = stmt.executeQuery(
//                         "SELECT setting_id FROM member WHERE member_id BETWEEN " + minMemberId + " AND " + maxMemberId)) {
//                while (rs.next()) settingIds.add(rs.getLong(1));
//            }
//            exec(conn, "DELETE FROM member WHERE member_id BETWEEN " + minMemberId + " AND " + maxMemberId, "member");
//            conn.commit();
//
//            int settingDeleted = 0;
//            for (int i = 0; i < settingIds.size(); i += JDBC_BATCH_SIZE) {
//                List<Long> batch = settingIds.subList(i, Math.min(i + JDBC_BATCH_SIZE, settingIds.size()));
//                String inClause = batch.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("0");
//                try (Statement stmt = conn.createStatement()) {
//                    settingDeleted += stmt.executeUpdate("DELETE FROM setting WHERE setting_id IN (" + inClause + ")");
//                }
//            }
//            conn.commit();
//            log.info("  setting 삭제: {}rows", fmt(settingDeleted));
//
//            try (Statement stmt = conn.createStatement()) {
//                stmt.execute("SET foreign_key_checks = 1");
//                conn.commit();
//            }
//        }
//    }
//
//    private void exec(Connection conn, String sql, String label) throws SQLException {
//        try (Statement stmt = conn.createStatement()) {
//            long deleted = stmt.executeLargeUpdate(sql);
//            log.info("  {} 삭제: {}rows", label, fmt(deleted));
//            conn.commit();
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────
//    //  삽입 헬퍼
//    // ─────────────────────────────────────────────────────────────
//
//    private void insertSettings(Connection conn) throws SQLException {
//        try (Statement stmt = conn.createStatement()) {
//            stmt.execute("INSERT INTO setting (message_notification, comment_notification, " +
//                         "post_featured_notification, friend_send_notification) " +
//                         "SELECT 1,1,1,1 FROM " + NUM_GEN + " WHERE seq <= " + MEMBER_COUNT);
//            conn.commit();
//        }
//    }
//
//    private void insertMembers(Connection conn) throws SQLException {
//        long settingStart;
//        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT LAST_INSERT_ID()")) {
//            rs.next(); settingStart = rs.getLong(1);
//        }
//        try (Statement stmt = conn.createStatement()) {
//            stmt.execute("INSERT IGNORE INTO member (setting_id, social_id, provider, member_name, role) " +
//                         "SELECT " + settingStart + "+seq-1, CONCAT('perf_',seq), 'KAKAO', " +
//                         "CONCAT('PerfUser_',seq), 'USER' FROM " + NUM_GEN + " WHERE seq <= " + MEMBER_COUNT);
//            conn.commit();
//        }
//        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT LAST_INSERT_ID()")) {
//            rs.next(); memberStart = rs.getLong(1);
//        }
//    }
//
//    private void insertPosts(Connection conn) throws SQLException {
//        try (Statement stmt = conn.createStatement()) {
//            stmt.execute("INSERT INTO post (member_id, title, content, views, like_count, comment_count, " +
//                         "member_name, is_weekly, is_legend, is_notice, created_at) " +
//                         "SELECT " + memberStart + "+seq-1, CONCAT('PerfPost_',seq), " +
//                         "'performance test content', 0, 0, 0, CONCAT('PerfUser_',seq), " +
//                         "0, 0, 0, NOW() FROM " + NUM_GEN + " WHERE seq <= " + MEMBER_COUNT);
//            conn.commit();
//        }
//        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT LAST_INSERT_ID()")) {
//            rs.next(); postStart = rs.getLong(1);
//        }
//    }
//
//    /**
//     * friendship ~15,000,000 rows
//     * member i (0-indexed) ↔ (i + j) % MEMBER_COUNT, j in 1..150
//     * 저장 규칙: min → member_id, max → friend_id
//     */
//    private long insertFriendships(Connection conn) throws SQLException {
//        AtomicLong inserted = new AtomicLong(0);
//        int half = FRIENDS_PER_MEMBER / 2;
//        try (PreparedStatement ps = conn.prepareStatement(
//                "INSERT IGNORE INTO friendship (member_id, friend_id) VALUES (?, ?)")) {
//            int batchCount = 0;
//            for (int i = 0; i < MEMBER_COUNT; i++) {
//                for (int j = 1; j <= half; j++) {
//                    int partner = (i + j) % MEMBER_COUNT;
//                    ps.setLong(1, memberStart + Math.min(i, partner));
//                    ps.setLong(2, memberStart + Math.max(i, partner));
//                    ps.addBatch();
//                    if (++batchCount % JDBC_BATCH_SIZE == 0) {
//                        ps.executeBatch(); conn.commit();
//                        inserted.addAndGet(JDBC_BATCH_SIZE);
//                        if (inserted.get() % 1_000_000 == 0)
//                            log.info("  friendship 진행: {}rows", fmt(inserted.get()));
//                    }
//                }
//            }
//            flushBatch(ps, conn, inserted, batchCount);
//        }
//        return inserted.get();
//    }
//
//    /**
//     * post_like 10,000,000 rows (글 추천)
//     * member i → post of member (i + j + FRIENDS_PER_MEMBER) % MEMBER_COUNT, j in 1..100
//     */
//    private long insertPostLikes(Connection conn) throws SQLException {
//        AtomicLong inserted = new AtomicLong(0);
//        try (PreparedStatement ps = conn.prepareStatement(
//                "INSERT IGNORE INTO post_like (member_id, post_id, created_at) VALUES (?, ?, NOW())")) {
//            int batchCount = 0;
//            for (int i = 0; i < MEMBER_COUNT; i++) {
//                for (int j = 1; j <= LIKE_PER_MEMBER; j++) {
//                    int authorIdx = (i + j + FRIENDS_PER_MEMBER) % MEMBER_COUNT;
//                    ps.setLong(1, memberStart + i);
//                    ps.setLong(2, postStart + authorIdx);
//                    ps.addBatch();
//                    if (++batchCount % JDBC_BATCH_SIZE == 0) {
//                        ps.executeBatch(); conn.commit();
//                        inserted.addAndGet(JDBC_BATCH_SIZE);
//                        if (inserted.get() % 1_000_000 == 0)
//                            log.info("  post_like 진행: {}rows", fmt(inserted.get()));
//                    }
//                }
//            }
//            flushBatch(ps, conn, inserted, batchCount);
//        }
//        return inserted.get();
//    }
//
//    /**
//     * comment 10,000,000 rows (댓글 작성)
//     * member i → post of member (i + j + FRIENDS_PER_MEMBER + 100) % MEMBER_COUNT, j in 1..100
//     * comment_closure는 interaction score 쿼리에 불필요하므로 미삽입
//     */
//    private long insertComments(Connection conn) throws SQLException {
//        AtomicLong inserted = new AtomicLong(0);
//        try (PreparedStatement ps = conn.prepareStatement(
//                "INSERT INTO comment (member_id, post_id, content, deleted, created_at) VALUES (?, ?, 'perf', 0, NOW())")) {
//            int batchCount = 0;
//            for (int i = 0; i < MEMBER_COUNT; i++) {
//                for (int j = 1; j <= COMMENT_PER_MEMBER; j++) {
//                    int postAuthorIdx = (i + j + FRIENDS_PER_MEMBER + 100) % MEMBER_COUNT;
//                    ps.setLong(1, memberStart + i);
//                    ps.setLong(2, postStart + postAuthorIdx);
//                    ps.addBatch();
//                    if (++batchCount % JDBC_BATCH_SIZE == 0) {
//                        ps.executeBatch(); conn.commit();
//                        inserted.addAndGet(JDBC_BATCH_SIZE);
//                        if (inserted.get() % 1_000_000 == 0)
//                            log.info("  comment 진행: {}rows", fmt(inserted.get()));
//                    }
//                }
//            }
//            flushBatch(ps, conn, inserted, batchCount);
//        }
//        // commentStart = 첫 번째 삽입된 댓글 ID
//        // 삽입 순서: i=0,j=1 → i=0,j=2 → ... 이므로 member i의 댓글 = commentStart + i*COMMENT_PER_MEMBER + (j-1)
//        try (Statement s = conn.createStatement();
//             ResultSet rs = s.executeQuery(
//                     "SELECT MIN(comment_id) FROM comment WHERE member_id = " + memberStart)) {
//            rs.next(); commentStart = rs.getLong(1);
//        }
//        return inserted.get();
//    }
//
//    /**
//     * comment_like 10,000,000 rows (댓글 추천)
//     * member i → 댓글 작성자 member k = (i + j + FRIENDS_PER_MEMBER + 200) % MEMBER_COUNT, j in 1..100
//     * 해당 member k의 첫 번째 댓글: commentStart + k * COMMENT_PER_MEMBER
//     */
//    private long insertCommentLikes(Connection conn) throws SQLException {
//        AtomicLong inserted = new AtomicLong(0);
//        try (PreparedStatement ps = conn.prepareStatement(
//                "INSERT IGNORE INTO comment_like (member_id, comment_id, created_at) VALUES (?, ?, NOW())")) {
//            int batchCount = 0;
//            for (int i = 0; i < MEMBER_COUNT; i++) {
//                for (int j = 1; j <= COMMENT_LIKE_PER_MEMBER; j++) {
//                    int commentAuthorIdx = (i + j + FRIENDS_PER_MEMBER + 200) % MEMBER_COUNT;
//                    ps.setLong(1, memberStart + i);
//                    ps.setLong(2, commentStart + (long) commentAuthorIdx * COMMENT_PER_MEMBER);
//                    ps.addBatch();
//                    if (++batchCount % JDBC_BATCH_SIZE == 0) {
//                        ps.executeBatch(); conn.commit();
//                        inserted.addAndGet(JDBC_BATCH_SIZE);
//                        if (inserted.get() % 1_000_000 == 0)
//                            log.info("  comment_like 진행: {}rows", fmt(inserted.get()));
//                    }
//                }
//            }
//            flushBatch(ps, conn, inserted, batchCount);
//        }
//        return inserted.get();
//    }
//
//    private void flushBatch(PreparedStatement ps, Connection conn, AtomicLong inserted, int batchCount)
//            throws SQLException {
//        if (batchCount % JDBC_BATCH_SIZE != 0) {
//            int[] r = ps.executeBatch(); conn.commit(); inserted.addAndGet(r.length);
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────
//    //  Redis 헬퍼
//    // ─────────────────────────────────────────────────────────────
//
//    private void deleteRedisByPattern(String pattern) {
//        List<String> keys = new ArrayList<>();
//        try (Cursor<String> cursor = stringRedisTemplate.scan(
//                ScanOptions.scanOptions().match(pattern).count(200).build())) {
//            cursor.forEachRemaining(keys::add);
//        }
//        if (!keys.isEmpty()) stringRedisTemplate.delete(keys);
//        log.info("  Redis 삭제 패턴={}, {}키", pattern, fmt(keys.size()));
//    }
//
//    private static String fmt(long n) { return String.format("%,d", n); }
//}
