import React from "react";
import { RefreshCw, Star } from "lucide-react";

interface LoadingSpinnerProps {
  variant?: "default" | "gradient";
  size?: "sm" | "md" | "lg";
  message?: string;
  className?: string;
}

const sizeClasses = {
  sm: "w-4 h-4",
  md: "w-6 h-6", 
  lg: "w-8 h-8",
};

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  variant = "default",
  size = "md",
  message,
  className,
}) => {
  if (variant === "gradient") {
    return (
      <div className={`flex flex-col items-center justify-center py-16 ${className || ""}`}>
        <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-purple-600 rounded-xl flex items-center justify-center mx-auto mb-4">
          <Star className="w-6 h-6 text-white animate-pulse" />
        </div>
        {message && <p className="text-gray-600">{message}</p>}
      </div>
    );
  }

  return (
    <div className={`flex items-center justify-center space-x-2 ${className || ""}`}>
      <RefreshCw className={`${sizeClasses[size]} text-purple-600 animate-spin`} />
      {message && <span className="text-sm text-gray-600">{message}</span>}
    </div>
  );
};