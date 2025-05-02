"use client";

import Link from "next/link";
import { useState, FormEvent, useEffect, useRef, useCallback } from "react";
import useAuthStore from "@/util/authStore";
import { useRouter } from "next/navigation";
import {
  NotificationDTO,
  NotificationType,
  UpdateNotificationDTO,
} from "./types/schema";

/**
 * 네비게이션 컴포넌트
 * 로그인 상태에 따라 다른 메뉴를 보여줍니다.
 */

// 로컬 스토리지 키
const DELETED_NOTIFICATIONS_KEY = "deleted_notifications";
const READ_NOTIFICATIONS_KEY = "read_notifications";
const SSE_CONNECTED_KEY = "sse_connected";

// 카카오 로그인 처리 함수
const handleLogin = () => {
  // 환경 변수를 활용한 카카오 로그인 URL 생성
  const authUrl = process.env.NEXT_PUBLIC_KAKAO_AUTH_URL;
  const clientId = process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID;
  const redirectUri = process.env.NEXT_PUBLIC_KAKAO_REDIRECT_URI;

  // 카카오 로그인 페이지로 이동
  window.location.href = `${authUrl}?response_type=code&client_id=${clientId}&redirect_uri=${redirectUri}`;
};

const Navigation = () => {
  const { user, logout } = useAuthStore();
  const [searchFarm, setSearchFarm] = useState(""); // 농장 검색어 상태
  const [isSearching, setIsSearching] = useState(false); // 검색 중 상태
  const router = useRouter();

  // 알림 관련 상태
  const [notifications, setNotifications] = useState<NotificationDTO[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const notificationRef = useRef<HTMLLIElement>(null);
  const eventSourceRef = useRef<EventSource | null>(null);
  const isConnectedRef = useRef<boolean>(false);

  // 배치 처리용 상태
  const [readIds, setReadIds] = useState<number[]>([]);
  const [deletedIds, setDeletedIds] = useState<number[]>([]);
  const batchIntervalRef = useRef<NodeJS.Timeout | null>(null);

  // 로컬 스토리지에서 상태 로드
  const loadStateFromLocalStorage = useCallback(() => {
    if (typeof window === "undefined") return; // SSR 환경에서는 실행하지 않음

    try {
      // 삭제된 알림 ID 로드
      const deletedIdsStr = localStorage.getItem(DELETED_NOTIFICATIONS_KEY);
      if (deletedIdsStr) {
        const deletedIdsFromStorage = JSON.parse(deletedIdsStr);
        setDeletedIds(deletedIdsFromStorage);
      }

      // 읽은 알림 ID 로드
      const readIdsStr = localStorage.getItem(READ_NOTIFICATIONS_KEY);
      if (readIdsStr) {
        const readIdsFromStorage = JSON.parse(readIdsStr);
        setReadIds(readIdsFromStorage);
      }

      // 연결 상태 확인
      const connectedStr = localStorage.getItem(SSE_CONNECTED_KEY);
      if (connectedStr) {
        isConnectedRef.current = JSON.parse(connectedStr);
      }
    } catch (error) {
      console.error("로컬 스토리지에서 상태 로드 오류:", error);
    }
  }, []);

  // 로컬 스토리지의 상태를 알림 목록에 적용
  const applyLocalStorageState = useCallback(
    (notifications: NotificationDTO[]) => {
      // 로컬 스토리지에서 읽음/삭제 상태 가져오기
      let storedReadIds: number[] = [];
      let storedDeletedIds: number[] = [];

      try {
        const readIdsStr = localStorage.getItem(READ_NOTIFICATIONS_KEY);
        if (readIdsStr) {
          storedReadIds = JSON.parse(readIdsStr);
        }

        const deletedIdsStr = localStorage.getItem(DELETED_NOTIFICATIONS_KEY);
        if (deletedIdsStr) {
          storedDeletedIds = JSON.parse(deletedIdsStr);
        }
      } catch (error) {
        console.error("로컬 스토리지에서 상태 로드 오류:", error);
        return notifications;
      }

      // 먼저 INITIATE 타입과 이미 삭제된 알림 제외
      const filteredNotifications = notifications
        .filter(
          (notification) => notification.type !== NotificationType.INITIATE
        )
        .filter((notification) => !storedDeletedIds.includes(notification.id));

      // 그 다음 읽음 상태 적용
      return filteredNotifications.map((notification) => ({
        ...notification,
        isRead: notification.isRead || storedReadIds.includes(notification.id),
      }));
    },
    []
  );

  // 로컬 스토리지에 상태 저장
  const saveStateToLocalStorage = useCallback(() => {
    if (typeof window === "undefined") return; // SSR 환경에서는 실행하지 않음

    try {
      localStorage.setItem(
        DELETED_NOTIFICATIONS_KEY,
        JSON.stringify(deletedIds)
      );
      localStorage.setItem(READ_NOTIFICATIONS_KEY, JSON.stringify(readIds));
      localStorage.setItem(
        SSE_CONNECTED_KEY,
        JSON.stringify(isConnectedRef.current)
      );
    } catch (error) {
      console.error("로컬 스토리지에 상태 저장 오류:", error);
    }
  }, [deletedIds, readIds]);

  // 알림 목록 가져오기
  const fetchNotifications = useCallback(async () => {
    if (!user) return;

    try {
      const response = await fetch("https://grom-farm.com/api/notification/list", {
        credentials: "include",
      });

      if (response.ok) {
        const data = (await response.json()) as NotificationDTO[];

        // 로컬 스토리지 상태 적용 (삭제/읽음 처리)
        const processedNotifications = applyLocalStorageState(data);

        // 알림 목록 업데이트
        setNotifications(processedNotifications);

        // 안 읽은 알림 개수 계산 및 업데이트
        const unread = processedNotifications.filter(
          (notification) => !notification.isRead
        ).length;
        setUnreadCount(unread);

        // 받아온 알림 중 로컬 스토리지에 이미 읽음/삭제 처리된 항목들을 확인
        const storedReadIds = [];
        const storedDeletedIds = [];
        try {
          const readIdsStr = localStorage.getItem(READ_NOTIFICATIONS_KEY);
          if (readIdsStr) storedReadIds.push(...JSON.parse(readIdsStr));

          const deletedIdsStr = localStorage.getItem(DELETED_NOTIFICATIONS_KEY);
          if (deletedIdsStr)
            storedDeletedIds.push(...JSON.parse(deletedIdsStr));
        } catch (error) {
          console.error("로컬 스토리지 로드 오류:", error);
        }

        // 서버에서 가져온 데이터 중 새로운 읽음/삭제 상태가 있는지 확인
        const serverReadIds = data
          .filter((notification) => notification.isRead)
          .map((notification) => notification.id);

        // 서버에서는 읽음 처리되었지만 로컬에서는 아직 안된 ID들을 배치 처리 대상에서 제외
        if (serverReadIds.length > 0) {
          setReadIds((prev) =>
            prev.filter((id) => !serverReadIds.includes(id))
          );
        }
      } else {
        console.error("알림 목록 가져오기 실패");
      }
    } catch (error) {
      console.error("알림 목록 가져오기 오류:", error);
    }
  }, [user, applyLocalStorageState]);

  // 배치 처리 함수 - 5분마다 호출
  const performBatchUpdate = useCallback(async () => {
    // 처리할 ID가 없으면 API 호출하지 않음
    if (readIds.length === 0 && deletedIds.length === 0) return;

    console.log(
      "배치 처리 실행 - 읽음:",
      readIds.length,
      "삭제:",
      deletedIds.length
    );

    // 현재 상태를 복사하고 상태 초기화
    const currentReadIds = [...readIds];
    const currentDeletedIds = [...deletedIds];

    // 상태 초기화
    setReadIds([]);
    setDeletedIds([]);

    try {
      // API 호출
      const response = await fetch(
        "https://grom-farm.com/api/notification/batch-update",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          credentials: "include",
          body: JSON.stringify({
            readIds: currentReadIds,
            deletedIds: currentDeletedIds,
          } as UpdateNotificationDTO),
        }
      );

      if (!response.ok) {
        console.error("배치 처리 실패:", await response.text());

        // 실패 시 상태 복원 (다음 배치에서 다시 시도)
        setReadIds((prev) => [...prev, ...currentReadIds]);
        setDeletedIds((prev) => [...prev, ...currentDeletedIds]);
      } else {
        console.log("배치 처리 성공:", {
          readIds: currentReadIds,
          deletedIds: currentDeletedIds,
        });

        // 성공적으로 서버에 상태가 반영되었으므로 이제 로컬 스토리지에서도 삭제 처리된 알림을 제거
        try {
          // 로컬 스토리지의 deletedIds 목록 갱신
          const storedDeletedIdsStr = localStorage.getItem(
            DELETED_NOTIFICATIONS_KEY
          );
          if (storedDeletedIdsStr) {
            // 현재 로컬 스토리지에 저장된 삭제된 ID 목록에서 방금 서버에 전송한 항목들 제거
            const storedDeletedIds = JSON.parse(storedDeletedIdsStr);
            const newDeletedIds = storedDeletedIds.filter(
              (id: number) => !currentDeletedIds.includes(id)
            );
            localStorage.setItem(
              DELETED_NOTIFICATIONS_KEY,
              JSON.stringify(newDeletedIds)
            );
          }

          // 로컬 스토리지의 readIds 목록 갱신
          const storedReadIdsStr = localStorage.getItem(READ_NOTIFICATIONS_KEY);
          if (storedReadIdsStr) {
            // 현재 로컬 스토리지에 저장된 읽은 ID 목록에서 방금 서버에 전송한 항목들 제거
            const storedReadIds = JSON.parse(storedReadIdsStr);
            const newReadIds = storedReadIds.filter(
              (id: number) => !currentReadIds.includes(id)
            );
            localStorage.setItem(
              READ_NOTIFICATIONS_KEY,
              JSON.stringify(newReadIds)
            );
          }
        } catch (error) {
          console.error("로컬 스토리지 업데이트 오류:", error);
        }
      }
    } catch (error) {
      console.error("배치 처리 오류:", error);

      // 오류 시 상태 복원 (다음 배치에서 다시 시도)
      setReadIds((prev) => [...prev, ...currentReadIds]);
      setDeletedIds((prev) => [...prev, ...currentDeletedIds]);
    }
  }, [readIds, deletedIds]);

  // SSE 연결 설정 함수
  const setupSSEConnection = useCallback(() => {
    // 이미 연결되어 있으면 중복 연결하지 않음
    if (isConnectedRef.current || eventSourceRef.current) {
      console.log("SSE 이미 연결됨, 중복 연결 방지");
      return; // 중복 연결 시 함수 종료
    }

    console.log("SSE 연결 시작");

    // SSE 연결 설정
    const eventSource = new EventSource(
      "https://grom-farm.com/api/notification/subscribe",
      {
        withCredentials: true,
      }
    );

    eventSource.onmessage = (event) => {
      console.log("기본 메시지 수신:", event.data);
    };

    // 이벤트 리스너 등록 - INITIATE는 화면에 표시하지 않음
    eventSource.addEventListener("INITIATE", (event) => {
      console.log("초기화 이벤트:", event.data);
      // INITIATE 이벤트는 화면에 알림으로 표시하지 않음

      // 첫 연결 성공 시 연결 상태 저장
      isConnectedRef.current = true;
      localStorage.setItem(SSE_CONNECTED_KEY, JSON.stringify(true));

      // 알림 목록 가져오기
      fetchNotifications();
    });

    // 각 NotificationType에 대한 이벤트 리스너 등록 (INITIATE 제외)
    Object.values(NotificationType)
      .filter((type) => type !== NotificationType.INITIATE)
      .forEach((type) => {
        eventSource.addEventListener(type, (event) => {
          try {
            const data = JSON.parse(event.data);

            // 필드명을 NotificationDTO에 맞게 조정
            const newNotification: NotificationDTO = {
              id: Date.now(), // 임시 ID
              data: data.message,
              url: data.url,
              type: type as NotificationType,
              isRead: false,
              createdAt: new Date().toISOString(),
            };

            // 이미 삭제 목록에 있는 ID는 무시
            const storedDeletedIds = JSON.parse(
              localStorage.getItem(DELETED_NOTIFICATIONS_KEY) || "[]"
            );
            if (!storedDeletedIds.includes(newNotification.id)) {
              setNotifications((prev) => [newNotification, ...prev]);
              setUnreadCount((prev) => prev + 1);
            }
          } catch (error) {
            console.error("알림 처리 중 오류:", error);
          }
        });
      });

    eventSource.onerror = (error) => {
      console.error("SSE 연결 오류:", error);
      // 연결 재시도 로직 대신, 에러 발생 시 연결을 닫고 상태를 초기화합니다.
      eventSource.close();
      eventSourceRef.current = null;

      // 연결 상태 초기화
      isConnectedRef.current = false;
      localStorage.setItem(SSE_CONNECTED_KEY, JSON.stringify(false));
    };

    eventSourceRef.current = eventSource;

    // 배치 처리 인터벌 설정 (5분)
    if (batchIntervalRef.current) {
      clearInterval(batchIntervalRef.current);
    }

    batchIntervalRef.current = setInterval(performBatchUpdate, 5 * 60 * 1000);

    return () => {
      // 마지막으로 한 번 더 배치 처리 실행
      performBatchUpdate();

      // 인터벌 정리
      if (batchIntervalRef.current) {
        clearInterval(batchIntervalRef.current);
      }

      // SSE 연결 정리
      eventSource.close();
      eventSourceRef.current = null;

      // 컴포넌트 언마운트 시 연결 상태 초기화
      isConnectedRef.current = false;
      localStorage.setItem(SSE_CONNECTED_KEY, JSON.stringify(false));
    };
  }, [fetchNotifications, performBatchUpdate]);

  // 페이지 로드 시 로컬 스토리지에서 상태 로드
  useEffect(() => {
    loadStateFromLocalStorage();
  }, [loadStateFromLocalStorage]);

  // deletedIds 또는 readIds가 변경될 때마다 로컬 스토리지에 저장
  useEffect(() => {
    saveStateToLocalStorage();
  }, [deletedIds, readIds, saveStateToLocalStorage]);

  // 유저 상태 변경 시 SSE 연결 및 알림 관리
  useEffect(() => {
    if (user && !isConnectedRef.current && !eventSourceRef.current) {
      // 최초 로그인 시 또는 새로고침 후 SSE 연결이 없을 때 연결 설정
      const cleanup = setupSSEConnection();
      fetchNotifications(); // 연결 후 알림 가져오기
      return cleanup; // 언마운트 시 정리 함수 반환
    } else if (!user) {
      // 로그아웃 시
      setNotifications([]);
      setUnreadCount(0);

      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }

      isConnectedRef.current = false;
      localStorage.setItem(SSE_CONNECTED_KEY, JSON.stringify(false));

      if (batchIntervalRef.current) {
        clearInterval(batchIntervalRef.current);
        batchIntervalRef.current = null;
      }
    } else if (user && !eventSourceRef.current && isConnectedRef.current) {
      // 로그인 상태지만 SSE 연결이 끊어진 경우 (페이지 새로고침 등)
      // 로컬 스토리지에 연결 정보는 있으나 실제 연결(eventSourceRef)이 없는 경우
      // 알림 목록만 다시 가져오고, SSE 재연결은 setupSSEConnection 내부 로직에 맡김
      fetchNotifications();
    }
  }, [user, setupSSEConnection, fetchNotifications]);

  // 단일 알림 읽음 처리 - 일반 케이스 (배치 처리)
  const handleMarkAsRead = (notification: NotificationDTO) => {
    if (notification.isRead) return; // 이미 읽은 알림은 처리하지 않음

    // 상태 업데이트 (사용자에게 즉시 반영)
    setNotifications((prevNotifications) =>
      prevNotifications.map((n) =>
        n.id === notification.id ? { ...n, isRead: true } : n
      )
    );

    setUnreadCount((prev) => Math.max(0, prev - 1));

    // 배치 처리를 위해 ID 저장
    setReadIds((prev) => [...prev, notification.id]);
  };

  // 단일 알림 삭제 처리 - 일반 케이스 (배치 처리)
  const handleDeleteNotification = (notificationId: number) => {
    // 상태 업데이트 (사용자에게 즉시 반영)
    setNotifications((prevNotifications) =>
      prevNotifications.filter((n) => n.id !== notificationId)
    );

    // 읽지 않은 알림이 삭제된 경우 카운트 업데이트
    const wasUnread = notifications.find(
      (n) => n.id === notificationId && !n.isRead
    );
    if (wasUnread) {
      setUnreadCount((prev) => Math.max(0, prev - 1));
    }

    // 배치 처리를 위해 ID 저장
    setDeletedIds((prev) => [...prev, notificationId]);

    // 이미 읽음 처리 대기 중인 알림이면 읽음 목록에서 제거
    setReadIds((prev) => prev.filter((id) => id !== notificationId));
  };

  // 알림 클릭 처리
  const handleNotificationClick = (notification: NotificationDTO) => {
    // 읽음 처리
    handleMarkAsRead(notification);

    // 해당 URL로 이동
    if (notification.url) {
      router.push(notification.url);
    }

    // 알림 목록 닫기
    setShowNotifications(false);
  };

  // 드롭다운 외부 클릭 감지
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        notificationRef.current &&
        !notificationRef.current.contains(event.target as Node)
      ) {
        setShowNotifications(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  // 로그아웃 처리
  const handleLogout = () => {
    // SSE 연결 정리
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }

    // 연결 상태 초기화
    isConnectedRef.current = false;
    localStorage.setItem(SSE_CONNECTED_KEY, JSON.stringify(false));

    // 인터벌 정리
    if (batchIntervalRef.current) {
      clearInterval(batchIntervalRef.current);
      batchIntervalRef.current = null;
    }

    // 원래 로그아웃 함수 호출
    logout();
  };

  const handleFarmSearch = (e: FormEvent) => {
    e.preventDefault();
    if (!searchFarm.trim()) return;

    setIsSearching(true);
    router.push(`/farm/${searchFarm}`);
    setIsSearching(false);
  };

  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-dark">
      <div className="container px-5">
        {/* 로고 */}
        <Link className="navbar-brand" href="/">
          농장 키우기
        </Link>

        {/* 햄버거 메뉴 (모바일) */}
        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#navbarSupportedContent"
          aria-controls="navbarSupportedContent"
          aria-expanded="false"
          aria-label="Toggle navigation"
          suppressHydrationWarning={true}
        >
          <span className="navbar-toggler-icon"></span>
        </button>

        {/* 네비게이션 메뉴 */}
        <div
          className="collapse navbar-collapse"
          id="navbarSupportedContent"
          suppressHydrationWarning={true}
        >
          {/* 데스크탑 검색바 */}
          <div
            className="d-none d-lg-block mx-lg-3 flex-grow-1"
            style={{ maxWidth: "400px" }}
          >
            <form className="d-flex" role="search" onSubmit={handleFarmSearch}>
              <div className="input-group input-group-sm">
                <input
                  type="search"
                  className="form-control form-control-sm"
                  placeholder="농장 이름을 입력하세요"
                  aria-label="농장 검색"
                  style={{ height: "31px" }}
                  value={searchFarm}
                  onChange={(e) => setSearchFarm(e.target.value)}
                  disabled={isSearching}
                />
                <button
                  className="btn btn-secondary btn-sm d-flex align-items-center justify-content-center"
                  type="submit"
                  style={{
                    height: "31px",
                    minWidth: "80px",
                    fontSize: "0.8rem",
                  }}
                  disabled={isSearching}
                >
                  {isSearching ? "검색 중..." : "농장 가기"}
                </button>
              </div>
            </form>
          </div>

          {/* 모바일 검색바 */}
          <form
            className="d-flex d-lg-none my-3 w-100"
            role="search"
            onSubmit={handleFarmSearch}
          >
            <div className="input-group input-group-sm">
              <input
                type="search"
                className="form-control form-control-sm"
                placeholder="농장 이름을 입력하세요"
                aria-label="농장 검색"
                value={searchFarm}
                onChange={(e) => setSearchFarm(e.target.value)}
                disabled={isSearching}
              />
              <button
                className="btn btn-secondary btn-sm d-flex align-items-center justify-content-center"
                type="submit"
                style={{ minWidth: "80px", fontSize: "0.8rem" }}
                disabled={isSearching}
              >
                {isSearching ? "검색 중..." : "농장 가기"}
              </button>
            </div>
          </form>

          {/* 메뉴 아이템들 */}
          <ul className="navbar-nav ms-auto mb-2 mb-lg-0">
            <li className="nav-item">
              <Link className="nav-link" href="/board">
                자유게시판
              </Link>
            </li>

            {/* 로그인 상태에 따른 메뉴 분기 */}
            {user ? (
              <>
                <li className="nav-item">
                  <Link className="nav-link" href={`/farm/${user?.farmName}`}>
                    내농장가기
                  </Link>
                </li>
                <li className="nav-item">
                  <Link className="nav-link" href="/ask">
                    건의하기
                  </Link>
                </li>
                <li className="nav-item">
                  <Link className="nav-link" href="/mypage">
                    마이페이지
                  </Link>
                </li>
                {/* 관리자 메뉴 - 관리자 권한이 있는 경우에만 표시 */}
                {user.role === "ADMIN" && (
                  <li className="nav-item">
                    <Link className="nav-link text-danger" href="/admin">
                      관리자
                    </Link>
                  </li>
                )}
                <li className="nav-item">
                  <button
                    className="nav-link"
                    onClick={handleLogout}
                    style={{
                      background: "none",
                      border: "none",
                      cursor: "pointer",
                    }}
                  >
                    로그아웃
                  </button>
                </li>
                {/* 알림 아이콘 */}
                <li className="nav-item dropdown" ref={notificationRef}>
                  <button
                    className="nav-link position-relative"
                    onClick={() => setShowNotifications(!showNotifications)}
                    style={{
                      background: "none",
                      border: "none",
                      cursor: "pointer",
                    }}
                  >
                    <i className="bi bi-bell-fill fs-5"></i>
                    {unreadCount > 0 && (
                      <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger">
                        {unreadCount > 99 ? "99+" : unreadCount}
                      </span>
                    )}
                  </button>

                  {/* 알림 드롭다운 */}
                  {showNotifications && (
                    <div
                      className="dropdown-menu dropdown-menu-end show p-0"
                      style={{
                        width: "300px",
                        maxHeight: "350px",
                        overflowY: "auto",
                      }}
                    >
                      <div className="list-group">
                        <div className="list-group-item bg-light d-flex justify-content-between align-items-center">
                          <h6 className="mb-0">알림</h6>
                          {unreadCount > 0 && (
                            <span className="badge bg-primary rounded-pill">
                              {unreadCount}개 안 읽음
                            </span>
                          )}
                        </div>

                        {notifications.length === 0 ? (
                          <div className="list-group-item text-center py-3">
                            <p className="text-muted mb-0">알림이 없습니다</p>
                          </div>
                        ) : (
                          notifications.map((notification) => (
                            <div
                              key={notification.id}
                              className={`list-group-item ${
                                !notification.isRead ? "bg-light" : ""
                              }`}
                            >
                              <div className="d-flex justify-content-between align-items-center mb-1">
                                <small className="text-muted">
                                  {new Date(
                                    notification.createdAt
                                  ).toLocaleString("ko-KR", {
                                    month: "numeric",
                                    day: "numeric",
                                    hour: "2-digit",
                                    minute: "2-digit",
                                  })}
                                </small>
                                <div>
                                  {!notification.isRead && (
                                    <span className="badge bg-primary rounded-pill me-1">
                                      New
                                    </span>
                                  )}
                                  {(notification.type ===
                                    NotificationType.COMMENT ||
                                    notification.type ===
                                      NotificationType.POST_FEATURED) && (
                                    <span className="badge bg-danger rounded-pill me-1">
                                      중요
                                    </span>
                                  )}
                                  <button
                                    className="btn btn-sm text-danger p-0 px-1"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleDeleteNotification(notification.id);
                                    }}
                                    title="삭제"
                                  >
                                    <i className="bi bi-x"></i>
                                  </button>
                                </div>
                              </div>
                              <p
                                className="mb-1 text-truncate"
                                style={{ cursor: "pointer" }}
                                onClick={() =>
                                  handleNotificationClick(notification)
                                }
                              >
                                {notification.data}
                              </p>
                              {notification.url && (
                                <small
                                  className="text-primary"
                                  style={{ cursor: "pointer" }}
                                  onClick={() =>
                                    handleNotificationClick(notification)
                                  }
                                ></small>
                              )}
                            </div>
                          ))
                        )}

                        <div className="list-group-item bg-light p-2 d-flex justify-content-between">
                          <button
                            className="btn btn-sm btn-outline-secondary"
                            onClick={() => {
                              const unreadNotifications = notifications.filter(
                                (n) => !n.isRead
                              );
                              if (unreadNotifications.length === 0) return;

                              // 상태 업데이트 (사용자에게 즉시 반영)
                              setNotifications((prevNotifications) =>
                                prevNotifications.map((n) => ({
                                  ...n,
                                  isRead: true,
                                }))
                              );
                              setUnreadCount(0);

                              // 읽음 처리할 ID 목록 추출
                              const unreadIds = unreadNotifications.map(
                                (n) => n.id
                              );

                              // 서버에 즉시 전송 (일괄 처리는 UX 상 즉시 반영이 필요함)
                              fetch(
                                "https://grom-farm.com/api/notification/batch-update",
                                {
                                  method: "POST",
                                  headers: {
                                    "Content-Type": "application/json",
                                  },
                                  credentials: "include",
                                  body: JSON.stringify({
                                    readIds: unreadIds,
                                    deletedIds: [],
                                  } as UpdateNotificationDTO),
                                }
                              )
                                .then((response) => {
                                  if (response.ok) {
                                    console.log("모두 읽음 처리 성공");
                                  } else {
                                    console.error("모두 읽음 처리 실패");
                                    // 실패 시에도 UI는 이미 변경되었으므로 배치 처리를 위해 ID 저장
                                    setReadIds((prev) => [
                                      ...prev,
                                      ...unreadIds,
                                    ]);
                                  }
                                })
                                .catch((error) => {
                                  console.error("모두 읽음 처리 오류:", error);
                                  // 오류 시 배치 처리를 위해 ID 저장
                                  setReadIds((prev) => [...prev, ...unreadIds]);
                                });
                            }}
                            disabled={unreadCount === 0}
                          >
                            모두 읽음 표시
                          </button>
                          <button
                            className="btn btn-sm btn-outline-danger"
                            onClick={() => {
                              // 모든 알림의 ID 목록 추출
                              const allIds = notifications.map((n) => n.id);
                              if (allIds.length === 0) return;

                              // 상태 업데이트 (사용자에게 즉시 반영)
                              setNotifications([]);
                              setUnreadCount(0);

                              // 서버에 즉시 전송 (일괄 처리는 UX 상 즉시 반영이 필요함)
                              fetch(
                                "https://grom-farm.com/api/notification/batch-update",
                                {
                                  method: "POST",
                                  headers: {
                                    "Content-Type": "application/json",
                                  },
                                  credentials: "include",
                                  body: JSON.stringify({
                                    readIds: [],
                                    deletedIds: allIds,
                                  } as UpdateNotificationDTO),
                                }
                              )
                                .then((response) => {
                                  if (response.ok) {
                                    console.log("모두 삭제 처리 성공");
                                  } else {
                                    console.error("모두 삭제 처리 실패");
                                    // 실패 시에도 UI는 이미 변경되었으므로 배치 처리를 위해 ID 저장
                                    setDeletedIds((prev) => [
                                      ...prev,
                                      ...allIds,
                                    ]);
                                  }
                                })
                                .catch((error) => {
                                  console.error("모두 삭제 처리 오류:", error);
                                  // 오류 시 배치 처리를 위해 ID 저장
                                  setDeletedIds((prev) => [...prev, ...allIds]);
                                });
                            }}
                            disabled={notifications.length === 0}
                          >
                            모두 삭제
                          </button>
                        </div>
                      </div>
                    </div>
                  )}
                </li>
                {/* 설정 아이콘 */}
                <li className="nav-item">
                  <Link className="nav-link" href="/setting">
                    <i className="bi bi-gear-fill fs-5"></i>
                  </Link>
                </li>
              </>
            ) : (
              <li className="nav-item">
                <button
                  className="nav-link"
                  onClick={handleLogin}
                  style={{
                    background: "none",
                    border: "none",
                    cursor: "pointer",
                  }}
                >
                  로그인
                </button>
              </li>
            )}
          </ul>
        </div>
      </div>
    </nav>
  );
};

export default Navigation;
