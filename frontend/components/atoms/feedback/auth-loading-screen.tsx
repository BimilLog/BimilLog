import { Spinner as FlowbiteSpinner } from "flowbite-react";

interface AuthLoadingScreenProps {
  message?: string;
  subMessage?: string;
}

export function AuthLoadingScreen({
  message = "로딩 중...",
  subMessage
}: AuthLoadingScreenProps) {
  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm">
      <div className="flex flex-col items-center">
        <FlowbiteSpinner
          color="pink"
          size="xl"
          aria-label={message}
        />
        <h2 className="mt-6 text-2xl bg-gradient-to-r from-purple-600 to-pink-600 bg-clip-text text-transparent font-bold mb-2">
          {message}
        </h2>
        {subMessage && (
          <p className="text-brand-muted">{subMessage}</p>
        )}
      </div>
    </div>
  );
}