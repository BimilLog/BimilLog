"use client";

import React from "react";
import { useForm } from "react-hook-form";
import { Button, Input, Textarea } from "@/components";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/molecules/select";
import { Send, Snowflake, Waves, IceCream2 } from "lucide-react";
import { Spinner as FlowbiteSpinner } from "flowbite-react";
import { getDecoInfo, decoTypeMap, type DecoType } from "@/lib/api";
import { DecoIcon } from "@/components";
import { logger } from '@/lib/utils/logger';

interface MessageFormData {
  content: string;
  anonymousNickname: string;
  decoType: string;
}

interface MessageFormProps {
  nickname?: string;
  position?: { x: number; y: number };
  onSubmit: (data: MessageFormData) => void;
  onSuccess?: (message: string) => void;
  onError?: (message: string) => void;
}

export const MessageForm = React.memo<MessageFormProps>(({ 
  onSubmit, 
  onSuccess, 
  onError 
}) => {
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<MessageFormData>({
    mode: "onChange",
    defaultValues: {
      content: "",
      anonymousNickname: "",
      decoType: "POTATO",
    },
  });

  const content = watch("content");
  const anonymousNickname = watch("anonymousNickname");
  const decoType = watch("decoType");

  // 데코 타입 옵션 생성: decoTypeMap 객체를 Select 옵션으로 변환
  const decoOptions = Object.entries(decoTypeMap).map(([key, info]) => ({
    value: key,
    label: info.name,
    info,
  }));

  // 폼 제출 처리: 입력값 트림 후 상위 컴포넌트로 전달, 성공 시 폼 리셋
  const onSubmitForm = async (data: MessageFormData) => {
    try {
      onSubmit({
        content: data.content.trim(),
        anonymousNickname: data.anonymousNickname.trim(),
        decoType: data.decoType,
      });
      reset();
      onSuccess?.("메시지가 성공적으로 추가되었습니다!");
    } catch (error) {
      logger.error("Failed to add message:", error);
      onError?.("메시지 추가에 실패했습니다. 다시 시도해주세요.");
    }
  };

  // 선택된 데코 타입의 정보 가져오기 (색상, 이름 등)
  const selectedDecoInfo = getDecoInfo(decoType);

  return (
    <form onSubmit={handleSubmit(onSubmitForm)}>
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
        {/* 미리보기 카드: 실시간으로 입력 중인 메시지 모습 미리보기 */}
        <div
          className={`p-4 rounded-xl bg-gradient-to-br ${selectedDecoInfo.color} border-2 border-white shadow-brand-lg relative overflow-hidden`}
          style={{
            backgroundImage: `
              radial-gradient(circle at 8px 8px, rgba(255,255,255,0.3) 1px, transparent 1px),
              radial-gradient(circle at 24px 24px, rgba(255,255,255,0.2) 1px, transparent 1px)
            `,
            backgroundSize: "16px 16px, 48px 48px",
          }}
        >
          <div className="flex items-center space-x-2 mb-2">
            <DecoIcon decoType={decoType as DecoType} size="lg" showBackground={false} />
            <span className="text-sm font-semibold text-brand-primary">
              {selectedDecoInfo.name}
            </span>
          </div>
          <p className="text-brand-primary text-sm font-medium">
            {/* 입력된 내용이 있으면 표시, 없으면 플레이스홀더 표시 */}
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
            {/* 익명 닉네임 입력: 1-8자 제한, 메시지 작성자 표시용 */}
            <Input
              placeholder="시원한 마음의 친구"
              {...register("anonymousNickname", {
                required: "닉네임을 입력해주세요",
                maxLength: {
                  value: 8,
                  message: "닉네임은 8자 이하로 입력해주세요",
                },
                minLength: {
                  value: 1,
                  message: "닉네임을 입력해주세요",
                },
              })}
              maxLength={8}
              className="bg-white/80 border-cyan-200 focus:border-cyan-400 font-medium"
            />
            <div className="flex justify-between items-center mt-1">
              {/* 에러 메시지가 있으면 표시, 없으면 글자수 카운터만 오른쪽 정렬 */}
              {errors.anonymousNickname && (
                <p className="text-xs text-red-500">
                  {errors.anonymousNickname.message}
                </p>
              )}
              <p className="text-right text-xs text-cyan-600 ml-auto">
                {anonymousNickname.length} / 8
              </p>
            </div>
          </div>

          <div>
            <label className="block text-sm font-bold mb-2 text-cyan-800 flex items-center space-x-1">
              <Waves className="w-4 h-4" />
              <span>시원한 메시지</span>
            </label>
            <Textarea
              placeholder="마음을 담은 시원한 메시지를 남겨주세요..."
              {...register("content", {
                required: "메시지를 입력해주세요",
                maxLength: {
                  value: 500,
                  message: "메시지는 500자 이하로 입력해주세요",
                },
                minLength: {
                  value: 1,
                  message: "메시지를 입력해주세요",
                },
              })}
              rows={4}
              maxLength={500}
              className="bg-white/80 border-cyan-200 focus:border-cyan-400 font-medium resize-none"
            />
            <div className="flex justify-between items-center mt-1">
              {errors.content && (
                <p className="text-xs text-red-500">
                  {errors.content.message}
                </p>
              )}
              <p className="text-right text-xs text-cyan-600 ml-auto">
                {content.length} / 500
              </p>
            </div>
          </div>

          <div>
            <label className="block text-sm font-bold mb-2 text-cyan-800 flex items-center space-x-1">
              <IceCream2 className="w-4 h-4" />
              <span>장식 선택</span>
            </label>
            {/* 장식 선택: setValue로 폼 상태 업데이트 (react-hook-form 방식) */}
            <Select
              value={decoType}
              onValueChange={(value) => setValue("decoType", value)}
            >
              <SelectTrigger className="bg-white/80 border-cyan-200 font-medium">
                <SelectValue>
                  <div className="flex items-center space-x-2">
                    <DecoIcon decoType={decoType as DecoType} size="md" showBackground={false} />
                    <span className="font-semibold">
                      {selectedDecoInfo.name}
                    </span>
                  </div>
                </SelectValue>
              </SelectTrigger>
              <SelectContent className="bg-white border-cyan-200">
                {/* 모든 데코 타입 옵션을 아이콘과 함께 표시 */}
                {decoOptions.map((d) => (
                  <SelectItem
                    key={d.value}
                    value={d.value}
                    className="font-medium"
                  >
                    <div className="flex items-center space-x-2">
                      <DecoIcon decoType={d.value as DecoType} size="md" showBackground={false} />
                      <span>{d.info.name}</span>
                    </div>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <Button
            type="submit"
            className="w-full h-12 bg-gradient-to-r from-blue-500 via-cyan-500 to-teal-500 hover:from-blue-600 hover:via-cyan-600 hover:to-teal-600 text-white font-bold text-lg shadow-brand-xl hover:shadow-brand-2xl transition-all duration-300 hover:scale-105 rounded-xl"
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <div className="flex items-center justify-center space-x-2">
                <FlowbiteSpinner color="white" size="sm" aria-label="등록 중..." />
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
    </form>
  );
});
MessageForm.displayName = "MessageForm";