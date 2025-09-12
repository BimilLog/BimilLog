package jaeik.bimillog.e2e.config;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * <h2>E2E 테스트 서버 관리자</h2>
 * <p>Frontend와 Backend 서버를 자동으로 시작하고 종료하는 유틸리티 클래스입니다.</p>
 * <p>서버 프로세스 생명주기 관리, Health check 수행</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class ServerManager {
    private static Process frontendProcess;
    private static Process backendProcess;
    private static Process redisProcess;
    private static final int FRONTEND_PORT = 3000;
    private static final int BACKEND_PORT = 8080;
    private static final int MAX_WAIT_SECONDS = 60;

    /**
     * <h3>모든 서버 시작</h3>
     * <p>Redis, Backend, Frontend 순서로 서버를 시작합니다.</p>
     * <p>각 서버의 health check를 수행하여 준비 상태를 확인합니다.</p>
     *
     * @throws RuntimeException 서버 시작 실패 시
     * @author Jaeik
     * @since 2.0.0
     */
    public static void startServers() {
        try {
            System.out.println("Starting servers for E2E tests...");
            
            startRedis();
            startBackend();
            startFrontend();
            
            System.out.println("All servers started successfully!");
        } catch (Exception e) {
            stopServers();
            throw new RuntimeException("Failed to start servers", e);
        }
    }

    /**
     * <h3>Redis 컨테이너 시작</h3>
     * <p>Docker Compose를 사용하여 Redis 컨테이너를 시작합니다.</p>
     *
     * @throws IOException 프로세스 시작 실패 시
     * @author Jaeik
     * @since 2.0.0
     */
    private static void startRedis() throws IOException {
        System.out.println("Starting Redis container...");
        
        ProcessBuilder pb = new ProcessBuilder(
            "docker-compose", "up", "-d", "redis"
        );
        pb.directory(new File("backend"));
        pb.environment().put("REDIS_PASSWORD", "76043341aa");
        redisProcess = pb.start();
        
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Redis container started");
    }

    /**
     * <h3>Backend 서버 시작</h3>
     * <p>Spring Boot 애플리케이션을 local 프로파일로 시작합니다.</p>
     *
     * @throws IOException 프로세스 시작 실패 시
     * @author Jaeik
     * @since 2.0.0
     */
    private static void startBackend() throws IOException {
        System.out.println("Starting Backend server...");
        
        ProcessBuilder pb = new ProcessBuilder(
            "./gradlew", "bootRun",
            "--args=--spring.profiles.active=local"
        );
        pb.directory(new File("."));
        pb.inheritIO();
        backendProcess = pb.start();
        
        waitForServer(
            "http://localhost:" + BACKEND_PORT + "/actuator/health"
        );
        
        System.out.println("Backend server started on port " + BACKEND_PORT);
    }

    /**
     * <h3>Frontend 서버 시작</h3>
     * <p>Next.js 개발 서버를 시작합니다.</p>
     *
     * @throws IOException 프로세스 시작 실패 시
     * @author Jaeik
     * @since 2.0.0
     */
    private static void startFrontend() throws IOException {
        System.out.println("Starting Frontend server...");
        
        ProcessBuilder pb = new ProcessBuilder("npm", "run", "dev");
        pb.directory(new File("../frontend"));
        pb.inheritIO();
        frontendProcess = pb.start();
        
        waitForServer(
            "http://localhost:" + FRONTEND_PORT
        );
        
        System.out.println("Frontend server started on port " + FRONTEND_PORT);
    }

    /**
     * <h3>서버 준비 상태 대기</h3>
     * <p>지정된 URL로 health check를 수행하여 서버가 준비될 때까지 대기합니다.</p>
     *
     * @param urlString health check URL
     * @throws RuntimeException 타임아웃 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    private static void waitForServer(String urlString) {
        long startTime = System.currentTimeMillis();
        long maxWaitTime = ServerManager.MAX_WAIT_SECONDS * 1000L;
        
        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                
                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode < 400) {
                    conn.disconnect();
                    return;
                }
                conn.disconnect();
            } catch (Exception e) {
                // Server not ready yet
            }
            
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for server", e);
            }
        }
        
        throw new RuntimeException(
            "Server at " + urlString + " did not start within " + ServerManager.MAX_WAIT_SECONDS + " seconds"
        );
    }

    /**
     * <h3>모든 서버 종료</h3>
     * <p>실행 중인 모든 서버 프로세스를 종료합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public static void stopServers() {
        System.out.println("Stopping servers...");
        
        if (frontendProcess != null && frontendProcess.isAlive()) {
            frontendProcess.destroyForcibly();
            System.out.println("Frontend server stopped");
        }
        
        if (backendProcess != null && backendProcess.isAlive()) {
            backendProcess.destroyForcibly();
            System.out.println("Backend server stopped");
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker-compose", "down"
            );
            pb.directory(new File("backend"));
            Process stopRedis = pb.start();
            stopRedis.waitFor(10, TimeUnit.SECONDS);
            System.out.println("Redis container stopped");
        } catch (Exception e) {
            System.err.println("Failed to stop Redis: " + e.getMessage());
        }
    }

    /**
     * <h3>서버 실행 상태 확인</h3>
     * <p>모든 서버가 실행 중인지 확인합니다.</p>
     *
     * @return 모든 서버가 실행 중이면 true
     * @author Jaeik
     * @since 2.0.0
     */
    public static boolean areServersRunning() {
        return isServerResponding("http://localhost:" + FRONTEND_PORT) &&
               isServerResponding("http://localhost:" + BACKEND_PORT + "/actuator/health");
    }

    /**
     * <h3>서버 응답 확인</h3>
     * <p>지정된 URL로 요청을 보내 서버가 응답하는지 확인합니다.</p>
     *
     * @param urlString 확인할 서버 URL
     * @return 서버가 응답하면 true
     * @author Jaeik
     * @since 2.0.0
     */
    private static boolean isServerResponding(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            return responseCode >= 200 && responseCode < 400;
        } catch (Exception e) {
            return false;
        }
    }
}