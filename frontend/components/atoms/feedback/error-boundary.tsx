'use client';

import { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';
import { Button } from '../actions/button';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: undefined });
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return <>{this.props.fallback}</>;
      }

      return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
          <div className="max-w-md w-full mx-4">
            <div className="bg-white/80 backdrop-blur-sm border-0 shadow-lg rounded-lg p-6 text-center">
              <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-100 mb-4">
                <AlertTriangle className="w-8 h-8 text-red-600" />
              </div>
              
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                오류가 발생했습니다
              </h2>
              
              <p className="text-gray-600 mb-6">
                예상치 못한 오류가 발생했습니다. 
                페이지를 새로고침하거나 잠시 후 다시 시도해주세요.
              </p>

              {process.env.NODE_ENV === 'development' && this.state.error && (
                <div className="mb-6 p-4 bg-gray-100 rounded-lg text-left">
                  <p className="text-sm font-mono text-gray-700 break-all">
                    {this.state.error.toString()}
                  </p>
                </div>
              )}

              <Button
                onClick={this.handleReset}
                className="w-full bg-gradient-to-r from-pink-500 to-purple-600 text-white hover:from-pink-600 hover:to-purple-700"
              >
                <RefreshCw className="w-4 h-4 mr-2" />
                페이지 새로고침
              </Button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}