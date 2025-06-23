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
import { rollingPaperApi, getDecoInfo, decoTypeMap } from "@/lib/api";

interface MessageFormProps {
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

  const decoOptions = Object.entries(decoTypeMap).map(([key, info]) => ({
    value: key,
    label: `${info.emoji} ${info.name}`,
    info,
  }));

  const handleSubmit = async () => {
    if (!content.trim() || !anonymousNickname.trim()) {
      alert("모든 필드를 입력해주세요.");
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await rollingPaperApi.createMessage(
        "my_nickname_placeholder", // TODO: 실제 사용자 닉네임으로 대체 필요
        {
          content: content.trim(),
          anonymity: anonymousNickname.trim(),
          decoType,
          width: 0, // 임시 값, 실제로는 클릭된 셀의 x, y를 사용해야 함
          height: 0,
        }
      );

      if (response.success && response.data) {
        onSubmit({
          content: response.data.content,
          anonymousNickname: response.data.anonymity,
          decoType: response.data.decoType,
        });
        setContent("");
        setAnonymousNickname("");
        alert("메시지가 성공적으로 등록되었습니다!");
      } else {
        alert("메시지 등록에 실패했습니다. 다시 시도해주세요.");
      }
    } catch (error) {
      console.error("Failed to create message:", error);
      alert("메시지 등록에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const selectedDecoInfo = getDecoInfo(decoType);

  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-2">익명 닉네임</label>
        <Input
          placeholder="익명의 친구"
          value={anonymousNickname}
          onChange={(e) => setAnonymousNickname(e.target.value)}
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-2">메시지</label>
        <Textarea
          placeholder="따뜻한 메시지를 남겨주세요..."
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={4}
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-2">
          데코레이션 선택
        </label>
        <Select value={decoType} onValueChange={setDecoType}>
          <SelectTrigger>
            <SelectValue>
              <div className="flex items-center space-x-2">
                <span>{selectedDecoInfo.emoji}</span>
                <span>{selectedDecoInfo.name}</span>
              </div>
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            {decoOptions.map((d) => (
              <SelectItem key={d.value} value={d.value}>
                <div className="flex items-center space-x-2">
                  <span>{d.info.emoji}</span>
                  <span>{d.info.name}</span>
                </div>
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Button
        onClick={handleSubmit}
        className="w-full bg-gradient-to-r from-pink-500 to-purple-600"
        disabled={isSubmitting}
      >
        {isSubmitting ? "등록 중..." : "메시지 남기기"}
      </Button>
    </div>
  );
};
