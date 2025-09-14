import { AuthLayout } from "@/components/organisms/auth";

interface AuthLoadingScreenProps {
  message?: string;
  subMessage?: string;
}

export function AuthLoadingScreen({ 
  message = "로딩 중...", 
  subMessage 
}: AuthLoadingScreenProps) {
  return (
    <AuthLayout>
      <div className="text-center">
        <div className="w-16 h-16 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center mx-auto mb-6">
          <div className="w-8 h-8 border-4 border-white border-t-transparent rounded-full animate-spin" />
        </div>
        <h2 className="text-2xl bg-gradient-to-r from-purple-600 to-pink-600 bg-clip-text text-transparent font-bold mb-2">
          {message}
        </h2>
        {subMessage && (
          <p className="text-brand-muted">{subMessage}</p>
        )}
      </div>
    </AuthLayout>
  );
}