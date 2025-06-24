import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Edit, Check, AlertCircle } from "lucide-react";
import { User, userApi } from "@/lib/api";
import { validateNickname } from "@/util/inputValidation";

interface UserProfileProps {
  user: User;
  onNicknameChange: (newNickname: string) => Promise<void>;
  onLogout: () => Promise<void>;
}

export const UserProfile: React.FC<UserProfileProps> = ({
  user,
  onNicknameChange,
  onLogout,
}) => {
  const [nicknameInput, setNicknameInput] = useState(user.userName);
  const [nicknameMessage, setNicknameMessage] = useState("");
  const [isNicknameFormatValid, setIsNicknameFormatValid] = useState(false);
  const [isNicknameAvailable, setIsNicknameAvailable] = useState<
    boolean | null
  >(null);
  const [isChecking, setIsChecking] = useState(false);
  const [isNicknameChangeSubmitting, setIsNicknameChangeSubmitting] =
    useState(false);
  const [isNicknameDialogOpen, setIsNicknameDialogOpen] = useState(false);

  const getInitials = (name?: string) => {
    if (!name) return "U";
    return name.charAt(0).toUpperCase();
  };

  const handleNicknameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newNickname = e.target.value;
    setNicknameInput(newNickname);
    setIsNicknameAvailable(null); // 닉네임이 바뀌면 중복확인 결과 초기화

    const { valid, message } = validateNickname(newNickname);
    setIsNicknameFormatValid(valid);
    setNicknameMessage(message);
  };

  const handleCheckNickname = async () => {
    if (!isNicknameFormatValid) return;
    if (nicknameInput === user.userName) {
      setNicknameMessage("현재 닉네임과 같습니다.");
      setIsNicknameAvailable(false);
      return;
    }

    setIsChecking(true);
    try {
      const response = await userApi.checkUserName(nicknameInput.trim());
      if (response.success) {
        const isAvailable = response.data ?? false;
        setIsNicknameAvailable(isAvailable);
        setNicknameMessage(
          isAvailable
            ? "사용 가능한 닉네임입니다."
            : "이미 사용중인 닉네임입니다."
        );
      } else {
        setIsNicknameAvailable(false);
        setNicknameMessage(
          response.error || "닉네임 확인 중 오류가 발생했습니다."
        );
      }
    } catch (error) {
      console.error(error);
      setIsNicknameAvailable(false);
      setNicknameMessage("닉네임 확인 중 오류가 발생했습니다.");
    } finally {
      setIsChecking(false);
    }
  };

  const handleNicknameSubmit = async () => {
    if (!isNicknameFormatValid || !isNicknameAvailable) return;

    // 닉네임 변경 전 확인 알림
    const confirmChange = window.confirm(
      "닉네임을 변경하면 보안을 위해 자동으로 로그아웃됩니다.\n다시 로그인해야 하며, 변경을 진행하시겠습니까?"
    );

    if (!confirmChange) return;

    setIsNicknameChangeSubmitting(true);
    try {
      const response = await userApi.updateUserName(
        user.userId,
        nicknameInput.trim()
      );
      if (response.success) {
        await onNicknameChange(nicknameInput.trim());
        alert(
          "닉네임이 성공적으로 변경되었습니다!\n보안을 위해 로그아웃됩니다."
        );
        setIsNicknameDialogOpen(false);
        // 다이얼로그가 닫힐 때 상태 초기화
        setNicknameMessage("");
        setIsNicknameAvailable(null);
        await onLogout();
      } else {
        alert(response.error || "닉네임 변경에 실패했습니다.");
        setIsNicknameAvailable(false);
      }
    } catch (error) {
      console.error(error);
      alert("닉네임 변경 중 오류가 발생했습니다.");
    } finally {
      setIsNicknameChangeSubmitting(false);
    }
  };

  const handleDialogOpenChange = (open: boolean) => {
    setIsNicknameDialogOpen(open);
    if (!open) {
      // 다이얼로그가 닫힐 때 상태 초기화
      setNicknameInput(user.userName);
      setNicknameMessage("");
      setIsNicknameFormatValid(false);
      setIsNicknameAvailable(null);
    }
  };

  return (
    <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg mb-8">
      <CardContent className="p-6 md:p-8">
        <div className="flex flex-col md:flex-row items-center md:space-x-8">
          <Avatar className="w-24 h-24 md:w-32 md:h-32 mb-4 md:mb-0">
            <AvatarImage
              src={user.thumbnailImage || undefined}
              alt={user.userName}
            />
            <AvatarFallback className="text-4xl">
              {getInitials(user.userName)}
            </AvatarFallback>
          </Avatar>
          <div className="flex-1 text-center md:text-left">
            <div className="flex flex-col md:flex-row md:items-center md:space-x-4 mb-4">
              <h2 className="text-3xl font-bold text-gray-800">
                {user.userName}
              </h2>
              <Dialog
                open={isNicknameDialogOpen}
                onOpenChange={handleDialogOpenChange}
              >
                <DialogTrigger asChild>
                  <Button variant="outline" size="sm" className="mt-2 md:mt-0">
                    <Edit className="w-4 h-4 mr-2" />
                    닉네임 변경
                  </Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>닉네임 변경</DialogTitle>
                  </DialogHeader>
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <Label
                        htmlFor="nickname"
                        className="text-sm font-medium text-gray-700"
                      >
                        닉네임
                      </Label>
                      <div className="flex space-x-2">
                        <div className="flex-1 relative">
                          <Input
                            id="nickname"
                            type="text"
                            placeholder="새 닉네임을 입력하세요"
                            value={nicknameInput}
                            onChange={handleNicknameChange}
                            className={`pr-10 ${
                              isNicknameAvailable === true
                                ? "border-green-500 focus-visible:ring-green-500"
                                : isNicknameAvailable === false
                                ? "border-red-500 focus-visible:ring-red-500"
                                : ""
                            }`}
                          />
                          {isNicknameAvailable === true && (
                            <Check className="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-green-500" />
                          )}
                          {(isNicknameAvailable === false ||
                            (!isNicknameFormatValid &&
                              nicknameInput.length > 0)) && (
                            <AlertCircle className="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-red-500" />
                          )}
                        </div>
                        <Button
                          variant="outline"
                          onClick={handleCheckNickname}
                          disabled={!isNicknameFormatValid || isChecking}
                          className="px-4"
                        >
                          {isChecking ? "확인중..." : "중복확인"}
                        </Button>
                      </div>
                      {nicknameMessage && (
                        <p
                          className={`text-sm ${
                            isNicknameAvailable === true
                              ? "text-green-600"
                              : isNicknameAvailable === false
                              ? "text-red-600"
                              : isNicknameFormatValid
                              ? "text-blue-600"
                              : "text-red-600"
                          }`}
                        >
                          {nicknameMessage}
                        </p>
                      )}
                    </div>
                    <Button
                      className="w-full"
                      onClick={handleNicknameSubmit}
                      disabled={
                        !isNicknameAvailable || isNicknameChangeSubmitting
                      }
                    >
                      {isNicknameChangeSubmitting
                        ? "변경 중..."
                        : "닉네임 변경"}
                    </Button>
                  </div>
                </DialogContent>
              </Dialog>
            </div>
            <p className="text-gray-500 mb-2">{user.kakaoNickname}</p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};
