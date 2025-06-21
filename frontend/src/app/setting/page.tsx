"use client";

import { useState, useEffect } from "react";
import useAuthStore from "@/util/authStore";
import { useRouter } from "next/navigation";
import fetchClient from "@/util/fetchClient";

const API_BASE = "https://grow-farm.com/api";



// UI용 확장 타입 (DTO와 UI 전용 필드 분리)
interface SettingUIState {
  AllNotification: boolean; // UI 전용 필드, 서버에 전송되지 않음
  farmNotification: boolean;
  commentNotification: boolean;
  postFeaturedNotification: boolean;
  commentFeaturedNotification: boolean;
}

const defaultSettings: SettingUIState = {
  AllNotification: false,
  farmNotification: false, // 누군가가 농장에 농작물 심었을 때
  commentNotification: false, // 누군가가 내 글에 댓글달았을 때
  postFeaturedNotification: false, // 내 글이 인기글이 되었을 때
  commentFeaturedNotification: false, // 내 댓글이 인기 댓글이 되었을 때
};

const SettingPage = () => {
  const { user } = useAuthStore();
  const router = useRouter();
  const [settings, setSettings] = useState<SettingUIState>(defaultSettings);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  // 설정 데이터 불러오기
  useEffect(() => {
    if (!user) {
      router.push("/");
      return;
    }

    const fetchSettings = async () => {
      setIsLoading(true);

      try {
        const response = await fetchClient(`${API_BASE}/user/setting`);

        if (response.ok) {
          const data = await response.json();
          console.log("서버에서 받은 설정 데이터:", data);

          // 서버에서 받은 데이터를 UI 상태로 변환
          const uiSettings: SettingUIState = {
            // 서버 응답 필드명에 맞게 접근 (is 접두사 없음)
            farmNotification: Boolean(data.farmNotification),
            commentNotification: Boolean(data.commentNotification),
            postFeaturedNotification: Boolean(data.postFeaturedNotification),
            commentFeaturedNotification: Boolean(
              data.commentFeaturedNotification
            ),
            // 모든 알림이 켜져있는지 확인하여 알림전체 상태 설정
            AllNotification:
              Boolean(data.farmNotification) &&
              Boolean(data.commentNotification) &&
              Boolean(data.postFeaturedNotification) &&
              Boolean(data.commentFeaturedNotification),
          };

          console.log("변환된 UI 설정 데이터:", uiSettings);
          setSettings(uiSettings);
        } else {
          console.error("설정 가져오기 실패");
        }
      } catch (error) {
        console.error("설정 가져오기 오류:", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchSettings();
  }, [user, router]);

  // 전체 알림 토글 처리 (UI 전용)
  const handleAllNotificationToggle = (checked: boolean) => {
    setSettings({
      AllNotification: checked,
      // 모든 알림을 켜면 모든 개별 알림도 켜지게, 끄면 모든 개별 알림도 꺼지게
      farmNotification: checked,
      commentNotification: checked,
      postFeaturedNotification: checked,
      commentFeaturedNotification: checked,
    });
  };

  // 개별 알림 토글 처리
  const handleToggleChange = (
    settingKey: keyof SettingUIState,
    checked: boolean
  ) => {
    // 개별 알림 상태만 변경
    setSettings((prev) => ({
      ...prev,
      [settingKey]: checked,
    }));

    // 전체 알림 상태 업데이트 (UI 전용)
    if (settingKey !== "AllNotification") {
      const updatedSettings = {
        ...settings,
        [settingKey]: checked,
      };

      // 모든 알림이 켜져있는지 확인
      const allTurnedOn =
        updatedSettings.farmNotification &&
        updatedSettings.commentNotification &&
        updatedSettings.postFeaturedNotification &&
        updatedSettings.commentFeaturedNotification;

      // 모든 알림이 꺼져있는지 확인
      const allTurnedOff =
        !updatedSettings.farmNotification &&
        !updatedSettings.commentNotification &&
        !updatedSettings.postFeaturedNotification &&
        !updatedSettings.commentFeaturedNotification;

      // 전체 알림 상태 업데이트
      if (allTurnedOn || allTurnedOff) {
        setSettings((prev) => ({
          ...prev,
          [settingKey]: checked,
          AllNotification: allTurnedOn,
        }));
      }
    }
  };

  // 설정 저장 처리
  const handleSaveSettings = async () => {
    if (!user) return;

    setIsSaving(true);

    try {
      // SettingDTO 형식에 맞게 데이터 구성 (AllNotification 제외)
      // 서버 필드명 형식에 맞게 변환 (is 접두사 없이)
      const settingData = {
        farmNotification: settings.farmNotification,
        commentNotification: settings.commentNotification,
        postFeaturedNotification: settings.postFeaturedNotification,
        commentFeaturedNotification: settings.commentFeaturedNotification,
      };

      console.log("서버에 전송할 설정 데이터:", settingData);

      try {
        const response = await fetchClient(`${API_BASE}/user/setting`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(settingData),
        });

        if (response.ok) {
          const data = await response.json();
          console.log("서버 저장 응답:", data);

          // 서버에서 받은 데이터를 UI 상태로 변환
          const uiSettings: SettingUIState = {
            farmNotification: Boolean(data.farmNotification),
            commentNotification: Boolean(data.commentNotification),
            postFeaturedNotification: Boolean(data.postFeaturedNotification),
            commentFeaturedNotification: Boolean(
              data.commentFeaturedNotification
            ),
            // 모든 알림이 켜져있는지 확인하여 알림전체 상태 설정
            AllNotification:
              Boolean(data.farmNotification) &&
              Boolean(data.commentNotification) &&
              Boolean(data.postFeaturedNotification) &&
              Boolean(data.commentFeaturedNotification),
          };

          setSettings(uiSettings);
          alert("설정이 저장되었습니다.");
        } else {
          console.error("설정 저장 실패");
          alert("설정 저장에 실패했습니다. 다시 시도해주세요.");
        }
      } catch (fetchError) {
        console.error("서버 통신 오류:", fetchError);
        alert("서버 연결에 실패했습니다. 네트워크 상태를 확인해주세요.");
      }
    } catch (error) {
      console.error("설정 저장 오류:", error);
      alert("설정 저장 중 오류가 발생했습니다. 다시 시도해주세요.");
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <main className="flex-shrink-0">
      <div className="container px-5 py-5">
        <h1 className="fw-bold mb-4">설정</h1>

        {isLoading ? (
          <div className="text-center my-5">
            <div className="spinner-border" role="status">
              <span className="visually-hidden">로딩 중...</span>
            </div>
          </div>
        ) : (
          <div className="card shadow-sm">
            <div className="card-header bg-light">
              <h5 className="mb-0">모바일 알림 설정</h5>
            </div>
            <div className="card-body">
              {/* 알림 설정 항목들 */}
              <div className="list-group list-group-flush">
                {/* 전체 알림 */}
                <div className="list-group-item px-0">
                  <div className="form-check form-switch">
                    <input
                      className="form-check-input"
                      type="checkbox"
                      id="AllNotification"
                      checked={settings.AllNotification}
                      onChange={(e) =>
                        handleAllNotificationToggle(e.target.checked)
                      }
                    />
                    <label
                      className="form-check-label fw-bold"
                      htmlFor="AllNotification"
                    >
                      알림 전체
                    </label>
                  </div>
                  <p className="text-muted mb-0">
                    모든 알림을 한번에 켜거나 끕니다.
                  </p>
                </div>

                {/* 농장 알림 */}
                <div className="list-group-item px-0">
                  <div className="form-check form-switch">
                    <input
                      className="form-check-input"
                      type="checkbox"
                      id="farmNotification"
                      checked={settings.farmNotification}
                      onChange={(e) =>
                        handleToggleChange("farmNotification", e.target.checked)
                      }
                    />
                    <label
                      className="form-check-label"
                      htmlFor="farmNotification"
                    >
                      농장 활동 알림
                    </label>
                  </div>
                  <p className="text-muted mb-0">
                    누군가 내 농장에 농작물을 심었을 때 알림을 받습니다.
                  </p>
                </div>

                {/* 댓글 알림 */}
                <div className="list-group-item px-0">
                  <div className="form-check form-switch">
                    <input
                      className="form-check-input"
                      type="checkbox"
                      id="commentNotification"
                      checked={settings.commentNotification}
                      onChange={(e) =>
                        handleToggleChange(
                          "commentNotification",
                          e.target.checked
                        )
                      }
                    />
                    <label
                      className="form-check-label"
                      htmlFor="commentNotification"
                    >
                      댓글 알림
                    </label>
                  </div>
                  <p className="text-muted mb-0">
                    내 글에 댓글이 달렸을 때 알림을 받습니다.
                  </p>
                </div>

                {/* 인기글 알림 */}
                <div className="list-group-item px-0">
                  <div className="form-check form-switch">
                    <input
                      className="form-check-input"
                      type="checkbox"
                      id="postFeaturedNotification"
                      checked={settings.postFeaturedNotification}
                      onChange={(e) =>
                        handleToggleChange(
                          "postFeaturedNotification",
                          e.target.checked
                        )
                      }
                    />
                    <label
                      className="form-check-label"
                      htmlFor="postFeaturedNotification"
                    >
                      인기글 알림
                    </label>
                  </div>
                  <p className="text-muted mb-0">
                    내 글이 인기글로 선정되었을 때 알림을 받습니다.
                  </p>
                </div>

                {/* 인기 댓글 알림 */}
                <div className="list-group-item px-0">
                  <div className="form-check form-switch">
                    <input
                      className="form-check-input"
                      type="checkbox"
                      id="commentFeaturedNotification"
                      checked={settings.commentFeaturedNotification}
                      onChange={(e) =>
                        handleToggleChange(
                          "commentFeaturedNotification",
                          e.target.checked
                        )
                      }
                    />
                    <label
                      className="form-check-label"
                      htmlFor="commentFeaturedNotification"
                    >
                      인기 댓글 알림
                    </label>
                  </div>
                  <p className="text-muted mb-0">
                    내 댓글이 인기 댓글로 선정되었을 때 알림을 받습니다.
                  </p>
                </div>
              </div>
            </div>
            <div className="card-footer">
              <button
                className="btn btn-primary"
                onClick={handleSaveSettings}
                disabled={isSaving}
              >
                {isSaving ? (
                  <>
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      role="status"
                      aria-hidden="true"
                    ></span>
                    저장 중...
                  </>
                ) : (
                  "설정 저장"
                )}
              </button>
            </div>
          </div>
        )}
      </div>
    </main>
  );
};

export default SettingPage;
