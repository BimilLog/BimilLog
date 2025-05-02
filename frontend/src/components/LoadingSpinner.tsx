import React from "react";
import Lottie from "lottie-react";
import loadingData from "../assets/loading.json";

interface LoadingSpinnerProps {
  width?: number;
  height?: number;
}

const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  width = 150,
  height = 150,
}) => {
  return (
    <div
      style={{
        width,
        height,
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
      }}
    >
      <Lottie animationData={loadingData} loop={true} />
    </div>
  );
};

export default LoadingSpinner;
