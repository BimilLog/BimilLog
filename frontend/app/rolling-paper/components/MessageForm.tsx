import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Send, Snowflake, Waves, IceCream2, Mail } from "lucide-react";
import { getDecoInfo, decoTypeMap } from "@/lib/api";
import { useToast } from "@/hooks/useToast";
import { ToastContainer } from "@/components/molecules/toast";

interface MessageFormProps {
  nickname?: string;
  position?: { x: number; y: number };
  onSubmit: (data: {
    content: string;
    anonymousNickname: string;
    decoType: string;
  }) => void;
}

export const MessageForm: React.FC<MessageFormProps> = ({ onSubmit }) => {
  const [content, setContent] = useState("");
  const [anonymousNickname, setAnonymousNickname] = useState("");
  const [decoType, setDecoType] = useState("POTATO");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { showSuccess, showError, showWarning, toasts, removeToast } =
    useToast();

  const decoOptions = Object.entries(decoTypeMap).map(([key, info]) => ({
    value: key,
    label: `${info.emoji} ${info.name}`,
    info,
  }));

  const handleSubmit = async () => {
    if (!content.trim() || !anonymousNickname.trim()) {
      showWarning("입력 확인", "모든 필드를 입력해주세요.");
      return;
    }

    setIsSubmitting(true);
    try {
      onSubmit({
        content: content.trim(),
        anonymousNickname: anonymousNickname.trim(),
        decoType,
      });
      setContent("");
      setAnonymousNickname("");
      showSuccess("메시지 작성 완료", "메시지가 성공적으로 추가되었습니다!");
    } catch (error) {
      console.error("Failed to add message:", error);
      showError(
        "메시지 작성 실패",
        "메시지 추가에 실패했습니다. 다시 시도해주세요."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const selectedDecoInfo = getDecoInfo(decoType);

  return (
    <>
      <div
        className="space-y-6 p-6 bg-gradient-to-br from-cyan-50 to-blue-50 rounded-2xl border-2 border-cyan-200"
        style={{
          backgroundImage: `
            radial-gradient(circle at 15px 15px, rgba(91,192,222,0.2) 1px, transparent 1px),
            radial-gradient(circle at 45px 45px, rgba(135,206,235,0.1) 1px, transparent 1px)
          `,
          backgroundSize: "30px 30px, 90px 90px",
        }}
      >
        {/* 미리보기 카드 */}
        <div
          className={`p-4 rounded-xl bg-gradient-to-br ${selectedDecoInfo.color} border-2 border-white shadow-lg relative overflow-hidden`}
          style={{
            backgroundImage: `
              radial-gradient(circle at 8px 8px, rgba(255,255,255,0.3) 1px, transparent 1px),
              radial-gradient(circle at 24px 24px, rgba(255,255,255,0.2) 1px, transparent 1px)
            `,
            backgroundSize: "16px 16px, 48px 48px",
          }}
        >
          <div className="flex items-center space-x-2 mb-2">
            <span className="text-2xl">{selectedDecoInfo.emoji}</span>
            <span className="text-sm font-semibold text-gray-800">
              {selectedDecoInfo.name}
            </span>
          </div>
          <p className="text-gray-800 text-sm font-medium">
            {content || "여기에 메시지가 표시됩니다..."}
          </p>
          {/* 반짝이는 효과 */}
          <div className="absolute top-1 right-1 w-2 h-2 bg-yellow-300 rounded-full animate-ping"></div>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-bold mb-2 text-cyan-800 flex items-center space-x-1">
              <Snowflake className="w-4 h-4" />
              <span>익명 닉네임</span>
            </label>
            <Input
              placeholder="시원한 마음의 친구"
              value={anonymousNickname}
              onChange={(e) => setAnonymousNickname(e.target.value)}
              maxLength={8}
              className="bg-white/80 border-cyan-200 focus:border-cyan-400 font-medium"
            />
            <p className="text-right text-xs text-cyan-600 mt-1">
              {anonymousNickname.length} / 8
            </p>
          </div>

          <div>
            <label className="block text-sm font-bold mb-2 text-cyan-800 flex items-center space-x-1">
              <Waves className="w-4 h-4" />
              <span>시원한 메시지</span>
            </label>
            <Textarea
              placeholder="마음을 담은 시원한 메시지를 남겨주세요..."
              value={content}
              onChange={(e) => setContent(e.target.value)}
              rows={4}
              maxLength={500}
              className="bg-white/80 border-cyan-200 focus:border-cyan-400 font-medium resize-none"
            />
            <p className="text-right text-xs text-cyan-600 mt-1">
              {content.length} / 500
            </p>
          </div>

          <div>
            <label className="block text-sm font-bold mb-2 text-cyan-800 flex items-center space-x-1">
              <IceCream2 className="w-4 h-4" />
              <span>장식 선택</span>
            </label>
            <Select value={decoType} onValueChange={setDecoType}>
              <SelectTrigger className="bg-white/80 border-cyan-200 font-medium">
                <SelectValue>
                  <div className="flex items-center space-x-2">
                    <span className="text-lg">{selectedDecoInfo.emoji}</span>
                    <span className="font-semibold">
                      {selectedDecoInfo.name}
                    </span>
                  </div>
                </SelectValue>
              </SelectTrigger>
              <SelectContent className="bg-white border-cyan-200">
                {decoOptions.map((d) => (
                  <SelectItem
                    key={d.value}
                    value={d.value}
                    className="font-medium"
                  >
                    <div className="flex items-center space-x-2">
                      <span className="text-lg">{d.info.emoji}</span>
                      <span>{d.info.name}</span>
                    </div>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <Button
            onClick={handleSubmit}
            className="w-full h-12 bg-gradient-to-r from-blue-500 via-cyan-500 to-teal-500 hover:from-blue-600 hover:via-cyan-600 hover:to-teal-600 text-white font-bold text-lg shadow-xl hover:shadow-2xl transition-all duration-300 hover:scale-105 rounded-xl"
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <div className="flex items-center space-x-2">
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                <span>등록 중...</span>
              </div>
            ) : (
              <div className="flex items-center space-x-2">
                <Send className="w-5 h-5" />
                <span>메시지 남기기</span>
              </div>
            )}
          </Button>
        </div>
      </div>

      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </>
  );
};
