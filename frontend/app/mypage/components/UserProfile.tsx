import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Edit } from "lucide-react";
import { User } from "@/lib/api";

interface UserProfileProps {
  user: User;
  onNicknameChange: (newNickname: string) => Promise<void>;
}

export const UserProfile: React.FC<UserProfileProps> = ({
  user,
  onNicknameChange,
}) => {
  const [nicknameInput, setNicknameInput] = useState(user.userName);
  const [isNicknameChangeSubmitting, setIsNicknameChangeSubmitting] =
    useState(false);
  const [isNicknameDialogOpen, setIsNicknameDialogOpen] = useState(false);

  const getInitials = (name?: string) => {
    if (!name) return "U";
    return name.charAt(0).toUpperCase();
  };

  const handleNicknameChange = async () => {
    if (!nicknameInput.trim() || nicknameInput === user?.userName) {
      alert("새 닉네임을 입력하거나 현재 닉네임과 다른 닉네임을 입력해주세요.");
      return;
    }
    setIsNicknameChangeSubmitting(true);
    try {
      await onNicknameChange(nicknameInput);
      alert("닉네임이 성공적으로 변경되었습니다!");
      setIsNicknameDialogOpen(false);
    } catch (error) {
      alert("닉네임 변경에 실패했습니다. 이미 사용 중인 닉네임일 수 있습니다.");
    } finally {
      setIsNicknameChangeSubmitting(false);
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
                onOpenChange={setIsNicknameDialogOpen}
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
                    <Input
                      value={nicknameInput}
                      onChange={(e) => setNicknameInput(e.target.value)}
                      placeholder="새 닉네임을 입력하세요"
                    />
                    <Button
                      className="w-full"
                      onClick={handleNicknameChange}
                      disabled={isNicknameChangeSubmitting}
                    >
                      {isNicknameChangeSubmitting ? "변경 중..." : "변경하기"}
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
