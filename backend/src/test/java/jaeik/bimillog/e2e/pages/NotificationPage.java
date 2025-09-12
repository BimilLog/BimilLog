package jaeik.bimillog.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * <h2>알림 페이지 객체</h2>
 * <p>알림 관련 UI 요소와 상호작용을 캡슐화합니다.</p>
 * <p>SSE 연결, 알림 수신, 읽음/삭제 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class NotificationPage extends BasePage {
    
    public NotificationPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }
    
    /**
     * <h3>알림 벨 클릭</h3>
     * <p>헤더의 알림 벨 아이콘을 클릭하여 알림 목록을 엽니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void openNotificationList() {
        try {
            Locator bellButton = page.locator("button:has(svg.lucide-bell)").first();
            if (!bellButton.isVisible()) {
                return;
            }
            
            bellButton.click();
            wait(500);

            if (!page.locator(".notification-list, [role='dialog']").isVisible()) {
                page.locator("text=알림").isVisible();
            }
        } catch (Exception e) {
            System.err.println("알림 목록 열기 실패: " + e.getMessage());
        }
    }
    
    /**
     * <h3>알림 개수 확인</h3>
     * <p>현재 표시된 알림의 총 개수를 반환합니다.</p>
     *
     * @return 알림 개수
     * @author Jaeik
     * @since 2.0.0
     */
    public int getNotificationCount() {
        try {
            Locator notifications = page.locator(".notification-item, [class*='notification'] > div").all()
                .stream()
                .filter(Locator::isVisible)
                .findFirst()
                .map(loc -> page.locator(".notification-item, [class*='divide-y'] > div"))
                .orElse(page.locator(".notification-item"));
            
            return notifications.count();
        } catch (Exception e) {
            System.err.println("알림 개수 확인 실패: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * <h3>읽지 않은 알림 개수</h3>
     * <p>알림 벨의 뱃지에 표시된 읽지 않은 알림 개수를 반환합니다.</p>
     *
     * @return 읽지 않은 알림 개수
     * @author Jaeik
     * @since 2.0.0
     */
    public int getUnreadCount() {
        try {
            Locator badge = page.locator("button:has(svg.lucide-bell) span.bg-red-500").first();
            if (!badge.isVisible()) {
                return 0;
            }
            
            String badgeText = badge.textContent().trim();
            if (badgeText.contains("+")) {
                return 99;
            }
            return Integer.parseInt(badgeText);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * <h3>개별 알림 읽음 처리</h3>
     * <p>특정 인덱스의 알림을 읽음으로 표시합니다.</p>
     * <p>5분 배치 대기열에 추가됩니다.</p>
     *
     * @param index 알림 인덱스 (0부터 시작)
     * @author Jaeik
     * @since 2.0.0
     */
    public void markAsRead(int index) {
        try {
            if (!isNotificationListOpen()) {
                openNotificationList();
            }
            
            Locator notification = page.locator("[class*='divide-y'] > div").nth(index);
            notification.hover();
            
            Locator readButton = notification.locator("button:has(svg.lucide-eye)").first();
            if (readButton.isVisible()) {
                readButton.click();
                wait(500);
            }

        } catch (Exception e) {
            System.err.println("읽음 처리 실패: " + e.getMessage());
        }
    }
    
    /**
     * <h3>개별 알림 삭제</h3>
     * <p>특정 인덱스의 알림을 삭제합니다.</p>
     * <p>5분 배치 대기열에 추가됩니다.</p>
     *
     * @param index 알림 인덱스
     * @author Jaeik
     * @since 2.0.0
     */
    public void deleteNotification(int index) {
        try {
            if (!isNotificationListOpen()) {
                openNotificationList();
            }
            
            Locator notification = page.locator("[class*='divide-y'] > div").nth(index);
            notification.hover();
            
            Locator deleteButton = notification.locator("button:has(svg.lucide-trash-2)").first();
            if (deleteButton.isVisible()) {
                deleteButton.click();
                wait(500);
            }

        } catch (Exception e) {
            System.err.println("알림 삭제 실패: " + e.getMessage());
        }
    }
    
    /**
     * <h3>모든 알림 읽음 처리</h3>
     * <p>모든 알림을 읽음으로 표시합니다.</p>
     * <p>즉시 처리됩니다.</p>
     *
     * @return 일괄 읽음 처리 성공 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean markAllAsRead() {
        try {
            if (!isNotificationListOpen()) {
                openNotificationList();
            }
            
            Locator markAllButton = page.locator("button:has-text('모두 읽음')").first();
            if (markAllButton.isVisible()) {
                markAllButton.click();
                wait(1000);
                
                return getUnreadCount() == 0;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("일괄 읽음 처리 실패: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * <h3>모든 알림 삭제</h3>
     * <p>모든 알림을 삭제합니다.</p>
     * <p>즉시 처리됩니다.</p>
     *
     * @return 일괄 삭제 성공 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean deleteAllNotifications() {
        try {
            if (!isNotificationListOpen()) {
                openNotificationList();
            }
            
            Locator deleteAllButton = page.locator("button:has-text('전체 삭제')").first();
            if (deleteAllButton.isVisible()) {
                deleteAllButton.click();
                wait(1000);
                
                return getNotificationCount() == 0;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("일괄 삭제 실패: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * <h3>알림 권한 요청</h3>
     * <p>브라우저 알림 권한을 요청합니다.</p>
     *
     * @return 권한 요청 성공 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean requestNotificationPermission() {
        try {
            if (!isNotificationListOpen()) {
                openNotificationList();
            }
            
            Locator permissionButton = page.locator("button:has(svg.lucide-bell)").nth(1);
            if (permissionButton.isVisible()) {
                permissionButton.click();
                wait(500);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("알림 권한 요청 실패: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * <h3>SSE 연결 상태 확인</h3>
     * <p>SSE 연결이 활성화되어 있는지 확인합니다.</p>
     *
     * @return SSE 연결 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean isSSEConnected() {
        try {
            Locator bellIcon = page.locator("button:has(svg.lucide-bell) svg").first();
            if (!bellIcon.isVisible()) {
                return false;
            }
            
            String iconClass = bellIcon.getAttribute("class");
            return iconClass != null && !iconClass.contains("text-gray-400");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * <h3>알림 생성 대기</h3>
     * <p>새로운 알림이 생성될 때까지 대기합니다.</p>
     *
     * @param maxWaitTime 최대 대기 시간 (밀리초)
     * @return 알림 생성 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean waitForNewNotification(int maxWaitTime) {
        try {
            int initialCount = getUnreadCount();
            long startTime = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - startTime < maxWaitTime) {
                wait(500);
                int currentCount = getUnreadCount();
                if (currentCount > initialCount) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * <h3>알림 내용 확인</h3>
     * <p>특정 인덱스의 알림 내용을 반환합니다.</p>
     *
     * @param index 알림 인덱스
     * @return 알림 내용 텍스트
     * @author Jaeik
     * @since 2.0.0
     */
    public String getNotificationContent(int index) {
        try {
            if (!isNotificationListOpen()) {
                openNotificationList();
            }
            
            Locator notification = page.locator("[class*='divide-y'] > div").nth(index);
            Locator content = notification.locator("p.text-sm").first();
            
            return content.textContent().trim();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * <h3>알림 타입 확인</h3>
     * <p>특정 인덱스의 알림 타입을 아이콘으로 판단합니다.</p>
     *
     * @param index 알림 인덱스
     * @return 알림 타입 (PAPER, COMMENT, POST_FEATURED, ADMIN, INITIATE)
     * @author Jaeik
     * @since 2.0.0
     */
    public String getNotificationType(int index) {
        try {
            if (!isNotificationListOpen()) {
                openNotificationList();
            }
            
            Locator notification = page.locator("[class*='divide-y'] > div").nth(index);
            
            if (notification.locator("svg.lucide-leaf").isVisible()) return "PAPER";
            if (notification.locator("svg.lucide-message-square").isVisible()) return "COMMENT";
            if (notification.locator("svg.lucide-star").isVisible()) return "POST_FEATURED";
            if (notification.locator("svg.lucide-shield").isVisible()) return "ADMIN";
            
            return "INITIATE";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    /**
     * <h3>배치 대기 개수 확인 (개발 모드)</h3>
     * <p>배치 처리 대기 중인 읽음/삭제 개수를 확인합니다.</p>
     *
     * @return [읽음 대기 개수, 삭제 대기 개수]
     * @author Jaeik
     * @since 2.0.0
     */
    public int[] getPendingBatchCount() {
        try {
            if (!isNotificationListOpen()) {
                openNotificationList();
            }
            
            Locator batchInfo = page.locator("text=/배치 대기.*읽음.*삭제/").first();
            if (!batchInfo.isVisible()) {
                return new int[]{0, 0};
            }
            
            String text = batchInfo.textContent();
            String[] parts = text.split("읽음")[1].split("삭제");
            
            int readCount = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
            int deleteCount = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
            
            return new int[]{readCount, deleteCount};
        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }
    
    /**
     * <h3>알림 목록 열림 상태 확인</h3>
     * <p>알림 목록이 현재 열려있는지 확인합니다.</p>
     *
     * @return 알림 목록 열림 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean isNotificationListOpen() {
        return page.locator("text=알림").isVisible() || 
               page.locator("[role='dialog']:has-text('알림')").isVisible();
    }
    
    /**
     * <h3>알림 목록 닫기</h3>
     * <p>열려있는 알림 목록을 닫습니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void closeNotificationList() {
        try {
            if (!isNotificationListOpen()) {
                return;
            }
            
            page.keyboard().press("Escape");
            wait(500);

            isNotificationListOpen();
        } catch (Exception e) {
        }
    }
}