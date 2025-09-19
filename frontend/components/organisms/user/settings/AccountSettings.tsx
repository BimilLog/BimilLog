import React from "react";
import { Button } from "@/components";
import { SettingsSection } from "@/components/molecules";
import { LogOut, AlertTriangle } from "lucide-react";
import { Spinner as FlowbiteSpinner } from "flowbite-react";

interface AccountSettingsProps {
  withdrawing: boolean;
  onWithdraw: () => void;
  className?: string;
}

export const AccountSettings: React.FC<AccountSettingsProps> = ({
  withdrawing,
  onWithdraw,
  className,
}) => (
  <SettingsSection
    icon={<LogOut className="w-5 h-5 stroke-red-600 fill-red-100" />}
    title="계정 관리"
    description="계정과 관련된 설정을 관리할 수 있습니다."
    className={className}
  >
    <div className="p-4 bg-red-50 border border-red-100 rounded-lg">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 stroke-red-600 fill-red-100 flex-shrink-0" />
          <div>
            <h3 className="font-medium text-red-800">회원 탈퇴</h3>
            <p className="text-sm text-red-700 mt-1">계정을 완전히 삭제합니다.</p>
          </div>
        </div>
        <Button
          variant="destructive"
          size="sm"
          onClick={onWithdraw}
          disabled={withdrawing}
          className="bg-red-600 hover:bg-red-700"
        >
          {withdrawing ? (
            <div className="flex items-center justify-center">
              <FlowbiteSpinner color="white" size="xs" aria-label="처리 중..." className="mr-1" />
              <span>처리 중...</span>
            </div>
          ) : (
            "탈퇴"
          )}
        </Button>
      </div>
    </div>
  </SettingsSection>
);