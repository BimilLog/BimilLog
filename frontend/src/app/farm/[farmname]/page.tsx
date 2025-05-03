"use client";

import { useParams } from "next/navigation";
import { useEffect, useState, useRef, useCallback } from "react";
import useAuthStore from "@/util/authStore";
import { CropDTO, CropType } from "@/components/types/schema";
import Script from "next/script";
import LoadingSpinner from "@/components/LoadingSpinner";

const API_BASE = "http://localhost:8080";

// Kakao SDK TypeScript declarations
declare global {
  interface Window {
    Kakao: {
      init: (apiKey: string) => void;
      isInitialized: () => boolean;
      Share: {
        createDefaultButton: (settings: {
          container: string | HTMLElement;
          objectType: string;
          templateId?: number;
          templateArgs?: Record<string, unknown>;
          installTalk?: boolean;
          callback?: (response: unknown) => void;
          serverCallbackArgs?: Record<string, unknown>;
        }) => void;
        sendDefault: (settings: {
          objectType: string;
          content: {
            title: string;
            description?: string;
            imageUrl: string;
            link: {
              mobileWebUrl: string;
              webUrl: string;
            };
          };
          buttons?: Array<{
            title: string;
            link: {
              mobileWebUrl: string;
              webUrl: string;
            };
          }>;
        }) => void;
      };
    };
  }
}

// 농작물 타입에 따른 이모티콘 매핑
const cropTypeToEmoji: Record<string, string> = {
  POTATO: "🥔",
  CARROT: "🥕",
  CABBAGE: "🥬",
  TOMATO: "🍅",
  STRAWBERRY: "🍓",
  WATERMELON: "🍉",
  PUMPKIN: "🎃",
  APPLE: "🍎",
  GRAPE: "🍇",
  BANANA: "🍌",
  GOBLIN: "👺",
  SLIME: "🧪",
  ORC: "👹",
  DRAGON: "🐉",
  PHOENIX: "🔥",
  WEREWOLF: "🐺",
  ZOMBIE: "🧟",
  KRAKEN: "🐙",
  CYCLOPS: "👁️",
  // 기본값
  DEFAULT: "🌱",
};

// 농작물 타입에 따른 한글 이름 매핑
const cropTypeToKorean: Record<string, string> = {
  POTATO: "감자",
  CARROT: "당근",
  CABBAGE: "양배추",
  TOMATO: "토마토",
  STRAWBERRY: "딸기",
  WATERMELON: "수박",
  PUMPKIN: "호박",
  APPLE: "사과",
  GRAPE: "포도",
  BANANA: "바나나",
  GOBLIN: "고블린",
  SLIME: "슬라임",
  ORC: "오크",
  DRAGON: "드래곤",
  PHOENIX: "피닉스",
  WEREWOLF: "늑대인간",
  ZOMBIE: "좀비",
  KRAKEN: "크라켄",
  CYCLOPS: "사이클롭스",
  // 기본값
  DEFAULT: "작물",
};

// 농작물 타입에 따른 이미지 경로 매핑
const cropTypeToImage: Record<string, string> = {
  POTATO: "/crops/potato.png",
  CARROT: "/crops/carrot.png",
  CABBAGE: "/crops/cabbage.png",
  TOMATO: "/crops/tomato.png",
  STRAWBERRY: "/crops/strawberry.png",
  WATERMELON: "/crops/watermelon.png",
  PUMPKIN: "/crops/pumpkin.png",
  APPLE: "/crops/apple.png",
  GRAPE: "/crops/grape.png",
  BANANA: "/crops/banana.png",
  GOBLIN: "/crops/goblin.png",
  SLIME: "/crops/slime.png",
  ORC: "/crops/orc.png",
  DRAGON: "/crops/dragon.png",
  PHOENIX: "/crops/phoenix.png",
  WEREWOLF: "/crops/werewolf.png",
  ZOMBIE: "/crops/zombie.png",
  KRAKEN: "/crops/kraken.png",
  CYCLOPS: "/crops/cyclops.png",
  // 기본값
  DEFAULT: "/crops/potato.png",
};

// CropType을 배열로 변환하여 선택 가능하게 함
const cropTypeOptions = Object.keys(cropTypeToEmoji)
  .filter((key) => key !== "DEFAULT")
  .map((key) => ({
    value: key,
    label: `${cropTypeToEmoji[key]} ${cropTypeToKorean[key]}`,
    image: cropTypeToImage[key],
  }));

export default function FarmPage() {
  const params = useParams();
  // URL 파라미터에서 farmname 추출 후 문자열로 처리
  const rawFarmName = Array.isArray(params.farmname)
    ? params.farmname[0]
    : params.farmname ?? "";
  const farmName = decodeURIComponent(rawFarmName);

  // 현재 로그인한 사용자 정보
  const { user } = useAuthStore();
  // 농작물 데이터 상태
  const [crops, setCrops] = useState<CropDTO[]>([]);
  // 로딩 상태
  const [isLoading, setIsLoading] = useState(true);
  // 선택된 농작물 (클릭 시 정보 표시용)
  const [selectedCrop, setSelectedCrop] = useState<CropDTO | null>(null);

  // 작물 심기 모달 상태
  const [showPlantModal, setShowPlantModal] = useState(false);
  // 작물 심기 폼 상태
  const [plantForm, setPlantForm] = useState({
    x: 0,
    y: 0,
    message: "",
    cropType: "POTATO" as CropType,
    nickname: "",
  });
  // 작물 심기 처리 중 상태
  const [isPlanting, setIsPlanting] = useState(false);
  // 선택된 좌표 상태
  const [selectedPosition, setSelectedPosition] = useState<{
    x: number;
    y: number;
  } | null>(null);

  // 공유 옵션 상태
  const [showShareOptions, setShowShareOptions] = useState(false);
  const shareDropdownRef = useRef<HTMLDivElement>(null);

  // 자신의 농장인지 확인
  const isMyFarm = user && user.farmName === farmName;

  // Kakao SDK 초기화
  const javaScriptKey = process.env.NEXT_PUBLIC_KAKAO_JAVA_SCRIPT_KEY;

  const initKakao = useCallback(() => {
    if (!javaScriptKey) {
      console.error("Kakao JavaScript Key is not defined.");
      return;
    }

    if (window.Kakao && !window.Kakao.isInitialized()) {
      window.Kakao.init(javaScriptKey);
      console.log("Kakao SDK initialized");
    }
  }, [javaScriptKey]);

  // 링크 복사 함수
  const copyLinkToClipboard = () => {
    const currentUrl = window.location.href;
    navigator.clipboard
      .writeText(currentUrl)
      .then(() => {
        alert("링크가 클립보드에 복사되었습니다.");
        setShowShareOptions(false);
      })
      .catch((err) => {
        console.error("링크 복사 실패:", err);
        alert("링크 복사에 실패했습니다. 다시 시도해주세요.");
      });
  };

  // 카카오톡 공유하기 함수
  const shareToKakao = () => {
    if (!window.Kakao || !window.Kakao.isInitialized()) {
      console.error("Kakao SDK is not initialized");
      alert(
        "카카오톡 공유 기능을 사용할 수 없습니다. 잠시 후 다시 시도해주세요."
      );
      return;
    }

    // 현재 URL
    const currentUrl = window.location.href;

    // 피드 템플릿 A형으로 메시지 보내기
    window.Kakao.Share.sendDefault({
      objectType: "feed",
      content: {
        title: farmName + " 농장에 놀러오세요!", // B영역: 농장 이름
        imageUrl:
          "https://postfiles.pstatic.net/MjAyNTA0MThfNzcg/MDAxNzQ0OTc4MDY3NjU2.b2ZRY2ZhuqdeFe8R70IoJZ0gGm4XTFZgKrZqNqQYinkg.vorO6lPc33dEIhZqQ7PbrwjOH7qn9-RfkOJAEVA2I2cg.JPEG/farmImage.jpeg?type=w773", // A영역: 이미지 (명시적 URL 적용),
        link: {
          mobileWebUrl: currentUrl,
          webUrl: currentUrl,
        },
      },
      buttons: [
        {
          title: "웹으로 보기",
          link: {
            mobileWebUrl: currentUrl,
            webUrl: currentUrl,
          },
        },
      ],
    });

    setShowShareOptions(false);
  };

  // 공유 옵션 토글
  const toggleShareOptions = () => {
    setShowShareOptions((prev) => !prev);
  };

  // 외부 클릭 시 공유 옵션 닫기
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        shareDropdownRef.current &&
        !shareDropdownRef.current.contains(event.target as Node)
      ) {
        setShowShareOptions(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  // Kakao SDK 초기화
  useEffect(() => {
    initKakao();
  }, [initKakao]);

  // 농장 데이터 로드
  useEffect(() => {
    const fetchCrops = async () => {
      setIsLoading(true);

      try {
        let response;

        // 본인 농장인 경우
        if (isMyFarm && user?.userId) {
          // userId를 쿼리 파라미터로 추가
          const queryParams = new URLSearchParams();
          queryParams.append("userId", user.userId.toString());

          response = await fetch(`${API_BASE}/farm/myFarm`, {
            method: "POST",
            credentials: "include",
          });
        }
        // 타인 농장인 경우
        else {
          response = await fetch(
            `${API_BASE}/farm/${encodeURIComponent(farmName)}`,
            {
              method: "GET",
              credentials: "include",
            }
          );
        }

        if (response.ok) {
          const cropsData: CropDTO[] = await response.json();
          setCrops(cropsData);
        } else {
          console.error(
            "농작물 데이터를 가져오는데 실패했습니다:",
            await response.text()
          );
        }
      } catch (error) {
        console.error("API 호출 중 오류 발생:", error);
      } finally {
        setIsLoading(false);
      }
    };

    if (farmName) {
      fetchCrops();
    }
  }, [farmName, isMyFarm, user]);

  // 특정 좌표에 있는 농작물 찾기
  const getCropAtPosition = (x: number, y: number) => {
    return crops.find((crop) => crop.width === x && crop.height === y);
  };

  // 좌표 클릭 핸들러 - 농작물이 있으면 정보 표시, 타인 농장에서 작물이 없으면 심기 가능
  const handlePositionClick = (x: number, y: number) => {
    const crop = getCropAtPosition(x, y);

    if (crop) {
      // 자신의 농장일 경우에만 작물 정보 표시
      if (isMyFarm) {
        setSelectedCrop(crop);
      } else {
        // 타인 농장에서 작물 클릭 시 정보를 표시하지 않음
        // 대신 간단한 알림을 표시하거나 무시할 수 있음
        setSelectedPosition(null);
      }
    } else if (!isMyFarm) {
      // 타인 농장이고 작물이 없는 경우 작물 심기 위해 좌표 선택
      setSelectedPosition({ x, y });
      setPlantForm((prev) => ({ ...prev, x, y }));
      setSelectedCrop(null);
    } else {
      // 본인 농장이고 작물이 없는 좌표 클릭 시 선택 해제
      setSelectedCrop(null);
      setSelectedPosition(null);
    }
  };

  // 팝업 닫기 핸들러
  const closePopup = () => {
    setSelectedCrop(null);
  };

  // 작물 심기 모달 열기
  const openPlantModal = () => {
    if (selectedPosition) {
      setShowPlantModal(true);
    } else {
      alert("먼저 작물을 심을 위치를 선택해주세요.");
    }
  };

  // 작물 심기 모달 닫기
  const closePlantModal = () => {
    setShowPlantModal(false);
  };

  // 작물 심기 폼 입력 처리
  const handlePlantFormChange = (
    e: React.ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >
  ) => {
    const { name, value } = e.target;
    setPlantForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  // 작물 심기 제출 처리
  const handlePlantSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    setIsPlanting(true);

    try {
      // 작물 데이터 준비
      const cropData: Partial<CropDTO> = {
        farmName: farmName,
        cropType: plantForm.cropType as CropType,
        message: plantForm.message,
        width: plantForm.x,
        height: plantForm.y,
        nickname: plantForm.nickname || "익명",
      };

      // 작물 심기 API 호출
      const response = await fetch(
        `${API_BASE}/farm/${encodeURIComponent(farmName)}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          credentials: "include",
          body: JSON.stringify(cropData),
        }
      );

      if (response.ok) {
        // 성공적으로 작물을 심었을 때
        const responseText = await response.text();
        alert(responseText || "농작물이 심어졌습니다.");

        // 모달 닫고 상태 초기화
        setShowPlantModal(false);
        setSelectedPosition(null);

        // 작물 목록 갱신
        // 서버에서 id가 생성되므로 간단하게 전체 목록을 다시 불러오는 방법 사용
        const refreshResponse = await fetch(
          `${API_BASE}/farm/${encodeURIComponent(farmName)}`,
          {
            method: "GET",
            credentials: "include",
          }
        );

        if (refreshResponse.ok) {
          const refreshedCrops: CropDTO[] = await refreshResponse.json();
          setCrops(refreshedCrops);
        }

        // 폼 초기화
        setPlantForm({
          x: 0,
          y: 0,
          message: "",
          cropType: "POTATO" as CropType,
          nickname: "",
        });
      } else {
        const errorText = await response.text();
        alert(`작물 심기에 실패했습니다: ${errorText}`);
      }
    } catch (error) {
      console.error("작물 심기 중 오류 발생:", error);
      alert("작물 심기 중 오류가 발생했습니다.");
    } finally {
      setIsPlanting(false);
    }
  };

  // 농작물 삭제 핸들러
  const handleDeleteCrop = async (cropId: number) => {
    // 삭제 확인
    if (!confirm("정말 농작물을 삭제하시겠습니까?")) {
      return;
    }

    try {
      const response = await fetch(`${API_BASE}/farm/myFarm/${cropId}`, {
        method: "POST",
        credentials: "include",
      });

      if (response.ok) {
        // 성공적으로 삭제됨
        alert("농작물이 삭제되었습니다.");

        // 작물 목록에서 삭제된 작물 제거
        setCrops((prevCrops) => prevCrops.filter((crop) => crop.id !== cropId));

        // 팝업 닫기
        setSelectedCrop(null);
      } else {
        const errorText = await response.text();
        alert(`농작물 삭제에 실패했습니다: ${errorText}`);
      }
    } catch (error) {
      console.error("농작물 삭제 중 오류 발생:", error);
      alert("농작물 삭제 중 오류가 발생했습니다.");
    }
  };

  return (
    <main className="flex-shrink-0">
      {/* Kakao SDK 스크립트 */}
      <Script
        src="https://t1.kakaocdn.net/kakao_js_sdk/2.5.0/kakao.min.js"
        integrity="sha384-kYPsUbBPlktXsY6/oNHSUDZoTX6+YI51f63jCPEIPFP09ttByAdxd2mEjKuhdqn4"
        crossOrigin="anonymous"
        onLoad={initKakao}
      />
      <div className="container px-5 py-5">
        <div className="row">
          <div className="col-lg-12">
            <div className="py-3 bg-light">
              <div className="text-center">
                <h2 className="fw-bolder">{farmName} 농장</h2>
              </div>
            </div>
            <div
              className="card-body"
              style={{
                backgroundColor: "black",
                padding: "3px",
                position: "relative",
              }}
            >
              {isLoading ? (
                <div className="d-flex justify-content-center p-5">
                  <LoadingSpinner width={150} height={150} />
                </div>
              ) : (
                <>
                  <img
                    src="/farmImage.jpeg"
                    alt="농장 이미지"
                    className="img-fluid rounded"
                    style={{
                      width: "100%",
                      height: "auto",
                      objectFit: "cover",
                    }}
                  />
                  <div
                    style={{
                      position: "absolute",
                      top: "7px",
                      left: "7px",
                      right: "7px",
                      bottom: "7px",
                      display: "grid",
                      gridTemplateColumns: "repeat(10, 1fr)",
                      gridTemplateRows: "repeat(5, 1fr)",
                      paddingTop: "9vw", // 최소 8px, 보통 2vw, 최대 24px
                      pointerEvents: "none",
                    }}
                  >
                    {Array.from({ length: 50 }).map((_, index) => {
                      const x = index % 10; // 0-9 (가로)
                      const y = Math.floor(index / 10); // 0-4 (세로)
                      const crop = getCropAtPosition(x, y);
                      const isSelected =
                        selectedPosition?.x === x && selectedPosition?.y === y;

                      return (
                        <div
                          key={index}
                          className={`grid-cell d-flex align-items-center justify-content-center ${
                            isSelected ? "selected-cell" : ""
                          }`}
                          style={{
                            border: isSelected
                              ? "2px solid rgba(255, 255, 255, 0.8)"
                              : "0.5px solid rgba(255, 255, 255, 0)",
                            borderRadius: "4px",
                            backgroundColor: isSelected
                              ? "rgba(255, 255, 255, 0.3)"
                              : "rgba(0, 0, 0, 0)",
                            pointerEvents: "auto", // 개별 셀은 클릭 가능
                            transition: "all 0.2s ease-in-out",
                            padding: "2px",
                            overflow: "hidden",
                            position: "relative",
                          }}
                          onClick={() => handlePositionClick(x, y)}
                        >
                          {crop && (
                            <div
                              title={`${crop.nickname}의 ${crop.cropType}`}
                              style={{
                                width: "100%",
                                height: "100%",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                              }}
                            >
                              <img
                                src={
                                  cropTypeToImage[crop.cropType] ||
                                  cropTypeToImage.DEFAULT
                                }
                                alt={crop.cropType}
                                style={{
                                  width: "auto",
                                  height: "auto",
                                  maxWidth: "95%",
                                  maxHeight: "95%",
                                  objectFit: "contain",
                                }}
                              />
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>

                  {/* 농작물 정보 팝업 */}
                  {selectedCrop && (
                    <div
                      className="position-fixed top-50 start-50 translate-middle bg-white p-4 rounded shadow"
                      style={{
                        zIndex: 1050,
                        maxWidth: "90%",
                        width: "400px",
                      }}
                    >
                      <div className="d-flex justify-content-between align-items-center mb-3">
                        <div className="d-flex align-items-center">
                          <h5 className="mb-0">쪽지보기</h5>
                          <span className="ms-3">
                            <img
                              src={
                                cropTypeToImage[selectedCrop.cropType] ||
                                cropTypeToImage.DEFAULT
                              }
                              alt={selectedCrop.cropType}
                              style={{
                                width: "24px",
                                height: "24px",
                                marginRight: "4px",
                                verticalAlign: "middle",
                              }}
                            />
                            <span>
                              {cropTypeToKorean[selectedCrop.cropType] ||
                                selectedCrop.cropType}
                            </span>
                          </span>
                        </div>

                        <button
                          className="btn-close"
                          onClick={closePopup}
                          aria-label="Close"
                        ></button>
                      </div>

                      <div className="p-3 bg-light rounded mb-3">
                        <p className="mb-0">
                          <strong>작성자:</strong> {selectedCrop.nickname}
                        </p>
                      </div>

                      <div className="p-3 bg-light rounded mb-3">
                        <p className="mb-0">
                          <strong>내용:</strong> {selectedCrop.message}
                        </p>
                      </div>

                      {/* 삭제 버튼 (자신의 농장인 경우에만 표시) */}
                      {isMyFarm && (
                        <div className="d-grid">
                          <button
                            className="btn btn-danger"
                            onClick={() => handleDeleteCrop(selectedCrop.id)}
                          >
                            <i className="bi bi-trash me-2"></i>농작물 삭제
                          </button>
                        </div>
                      )}
                    </div>
                  )}

                  {/* 배경 오버레이 (팝업 표시 시) */}
                  {(selectedCrop || showPlantModal) && (
                    <div
                      className="position-fixed top-0 start-0 w-100 h-100"
                      style={{
                        backgroundColor: "rgba(0,0,0,0.5)",
                        zIndex: 1040,
                      }}
                      onClick={() => {
                        closePopup();
                        if (!isPlanting) closePlantModal();
                      }}
                    ></div>
                  )}
                </>
              )}
            </div>

            {/* 타인 농장일 경우 농작물 심기 버튼 */}
            {!isMyFarm && (
              <div className="mt-3 d-grid gap-2">
                <button
                  className="btn btn-primary"
                  onClick={openPlantModal}
                  disabled={!selectedPosition || isPlanting}
                >
                  {selectedPosition
                    ? `선택한 위치에 농작물 심기`
                    : "먼저 농작물을 심을 위치를 선택해주세요"}
                </button>
                {selectedPosition && (
                  <p className="text-muted text-center mb-0">
                    <small>
                      다른 위치를 선택하려면 그리드에서 빈 칸을 클릭하세요
                    </small>
                  </p>
                )}
              </div>
            )}

            {/* 자신의 농장일 경우 홍보하기 버튼 */}
            {isMyFarm && (
              <div className="mt-3 d-grid">
                <div className="position-relative" ref={shareDropdownRef}>
                  <button
                    className="btn btn-success"
                    onClick={toggleShareOptions}
                  >
                    <i className="bi bi-share me-2"></i>농장 홍보하기
                  </button>

                  {showShareOptions && (
                    <div
                      className="position-absolute start-0 mt-1 bg-white border rounded shadow-sm"
                      style={{ zIndex: 1000, minWidth: "200px" }}
                    >
                      <ul className="list-group list-group-flush">
                        <li
                          className="list-group-item list-group-item-action"
                          onClick={copyLinkToClipboard}
                          style={{ cursor: "pointer" }}
                        >
                          <i className="bi bi-clipboard me-2"></i> 링크 복사
                        </li>
                        <li
                          className="list-group-item list-group-item-action"
                          onClick={shareToKakao}
                          style={{ cursor: "pointer" }}
                        >
                          <i className="bi bi-chat-fill me-2 text-warning"></i>{" "}
                          카카오톡 공유
                        </li>
                      </ul>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* 작물 심기 모달 */}
            {showPlantModal && (
              <div
                className="position-fixed top-50 start-50 translate-middle bg-white p-4 rounded shadow"
                style={{
                  zIndex: 1050,
                  maxWidth: "90%",
                  width: "400px",
                }}
                onClick={(e) => e.stopPropagation()} // 배경 클릭 이벤트 전파 방지
              >
                <div className="d-flex justify-content-between align-items-center mb-3">
                  <h5 className="mb-0">작물 심기</h5>
                  <button
                    className="btn-close"
                    onClick={closePlantModal}
                    disabled={isPlanting}
                    aria-label="Close"
                  ></button>
                </div>

                <form onSubmit={handlePlantSubmit}>
                  <div className="mb-3">
                    <label htmlFor="cropType" className="form-label">
                      작물 종류
                    </label>
                    <div className="d-flex align-items-center mb-2">
                      <img
                        src={
                          cropTypeToImage[plantForm.cropType] ||
                          cropTypeToImage.DEFAULT
                        }
                        alt={plantForm.cropType}
                        style={{
                          width: "30px",
                          height: "30px",
                          marginRight: "10px",
                        }}
                      />
                      <span>
                        선택한 작물:{" "}
                        {cropTypeToKorean[plantForm.cropType] ||
                          plantForm.cropType}
                      </span>
                    </div>
                    <select
                      id="cropType"
                      name="cropType"
                      className="form-select"
                      value={plantForm.cropType}
                      onChange={handlePlantFormChange}
                      disabled={isPlanting}
                      required
                    >
                      {cropTypeOptions.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="mb-3">
                    <label htmlFor="nickname" className="form-label">
                      작성자
                    </label>
                    <input
                      type="text"
                      id="nickname"
                      name="nickname"
                      className="form-control"
                      placeholder="원하는 이름을 입력하세요."
                      value={plantForm.nickname}
                      onChange={handlePlantFormChange}
                      disabled={isPlanting}
                      maxLength={15}
                      required
                    />
                  </div>

                  <div className="mb-3">
                    <label htmlFor="message" className="form-label">
                      메시지
                    </label>
                    <textarea
                      id="message"
                      name="message"
                      className="form-control"
                      rows={3}
                      placeholder="친구에게 남길 메시지를 입력하세요"
                      value={plantForm.message}
                      onChange={handlePlantFormChange}
                      disabled={isPlanting}
                      maxLength={200}
                      required
                    />
                    <div className="form-text">
                      {plantForm.message.length}/200자
                    </div>
                  </div>

                  <div className="d-grid">
                    <button
                      type="submit"
                      className="btn btn-primary"
                      disabled={isPlanting}
                    >
                      {isPlanting ? "작물 심는 중..." : "작물 심기"}
                    </button>
                  </div>
                </form>
              </div>
            )}
          </div>
        </div>
      </div>

      <style jsx>{`
        .grid-cell:hover {
          background-color: rgba(255, 255, 255, 0.5) !important;
          transform: scale(1.05);
          border: 0.5px solid rgba(255, 255, 255, 0.8) !important;
          box-shadow: 0 0 8px rgba(255, 255, 255, 0.5);
          z-index: 10;
          cursor: pointer;
        }

        .selected-cell {
          transform: scale(1.05);
          box-shadow: 0 0 12px rgba(255, 255, 255, 0.8);
          background-color: rgba(255, 255, 255, 0.5) !important;

          z-index: 5;
        }
      `}</style>
    </main>
  );
}
