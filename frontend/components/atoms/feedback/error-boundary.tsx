'use client';

import { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw, Home, ChevronDown, ChevronUp, Bug } from 'lucide-react';
import { Button } from '../actions/button';
import { logger } from '@/lib/utils/logger';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
  showReportButton?: boolean;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface State {
  hasError: boolean;
  error?: Error;
  errorInfo?: ErrorInfo;
  showDetails: boolean;
  isReporting: boolean;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      hasError: false,
      showDetails: false,
      isReporting: false
    };
  }

  static getDerivedStateFromError(error: Error): Partial<State> {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    this.setState({ errorInfo });

    // 로깅
    logger.error('ErrorBoundary caught an error:', error, errorInfo);

    // 중요한 에러는 운영환경에서도 로깅
    logger.critical('Application Error:', {
      message: error.message,
      stack: error.stack,
      componentStack: errorInfo.componentStack,
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href
    });

    // 커스텀 에러 핸들러 호출
    if (this.props.onError) {
      this.props.onError(error, errorInfo);
    }
  }

  handleReset = () => {
    this.setState({
      hasError: false,
      error: undefined,
      errorInfo: undefined,
      showDetails: false,
      isReporting: false
    });
    window.location.reload();
  };

  handleGoHome = () => {
    this.setState({
      hasError: false,
      error: undefined,
      errorInfo: undefined,
      showDetails: false,
      isReporting: false
    });
    window.location.href = '/';
  };

  toggleDetails = () => {
    this.setState(prevState => ({ showDetails: !prevState.showDetails }));
  };

  handleReport = async () => {
    if (!this.state.error || this.state.isReporting) return;

    this.setState({ isReporting: true });

    try {
      // 에러 리포트 로직 (필요시 서버로 전송)
      const errorReport = {
        message: this.state.error.message,
        stack: this.state.error.stack,
        componentStack: this.state.errorInfo?.componentStack,
        timestamp: new Date().toISOString(),
        userAgent: navigator.userAgent,
        url: window.location.href,
        userId: null, // 인증된 사용자의 경우 ID 추가
      };

      logger.info('Error report generated:', errorReport);

      // 실제 구현시 서버로 전송
      // await fetch('/api/error-report', {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify(errorReport)
      // });

      // 임시로 클립보드에 복사
      await navigator.clipboard.writeText(JSON.stringify(errorReport, null, 2));
      alert('에러 정보가 클립보드에 복사되었습니다.');

    } catch (err) {
      logger.error('Error reporting failed:', err);
      alert('에러 리포트 생성에 실패했습니다.');
    } finally {
      this.setState({ isReporting: false });
    }
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return <>{this.props.fallback}</>;
      }

      return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 p-4">
          <div className="max-w-lg w-full">
            <div className="bg-white/80 backdrop-blur-sm border-0 shadow-lg hover:shadow-xl transition-shadow rounded-lg p-6 text-center">
              <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-100 mb-4">
                <AlertTriangle className="w-8 h-8 text-red-600" />
              </div>

              <h2 className="text-2xl font-bold bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent mb-2">
                오류가 발생했습니다
              </h2>

              <p className="text-gray-600 mb-6 leading-relaxed">
                예상치 못한 오류가 발생했습니다.<br />
                페이지를 새로고침하거나 홈으로 이동해서 다시 시도해주세요.
              </p>

              {/* 에러 상세 정보 토글 */}
              {(process.env.NODE_ENV === 'development' || this.state.error) && (
                <div className="mb-6">
                  <Button
                    onClick={this.toggleDetails}
                    variant="ghost"
                    size="sm"
                    className="mb-3 text-gray-500 hover:text-gray-700"
                  >
                    {this.state.showDetails ? (
                      <>
                        <ChevronUp className="w-4 h-4 mr-1" />
                        세부 정보 숨기기
                      </>
                    ) : (
                      <>
                        <ChevronDown className="w-4 h-4 mr-1" />
                        세부 정보 보기
                      </>
                    )}
                  </Button>

                  {this.state.showDetails && this.state.error && (
                    <div className="p-4 bg-gray-100 rounded-lg text-left border-2 border-gray-200">
                      <h4 className="font-semibold text-gray-800 mb-2">오류 메시지:</h4>
                      <p className="text-sm font-mono text-red-600 mb-3 break-all">
                        {this.state.error.message}
                      </p>

                      {process.env.NODE_ENV === 'development' && this.state.error.stack && (
                        <>
                          <h4 className="font-semibold text-gray-800 mb-2">스택 트레이스:</h4>
                          <pre className="text-xs font-mono text-gray-600 whitespace-pre-wrap break-all overflow-x-auto max-h-32 overflow-y-auto">
                            {this.state.error.stack}
                          </pre>
                        </>
                      )}
                    </div>
                  )}
                </div>
              )}

              {/* 액션 버튼들 */}
              <div className="space-y-3">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <Button
                    onClick={this.handleReset}
                    variant="default"
                    size="default"
                    className="w-full"
                  >
                    <RefreshCw className="w-4 h-4 mr-2" />
                    새로고침
                  </Button>

                  <Button
                    onClick={this.handleGoHome}
                    variant="outline"
                    size="default"
                    className="w-full"
                  >
                    <Home className="w-4 h-4 mr-2" />
                    홈으로 이동
                  </Button>
                </div>

                {/* 에러 리포트 버튼 */}
                {this.props.showReportButton !== false && (
                  <Button
                    onClick={this.handleReport}
                    variant="ghost"
                    size="sm"
                    disabled={this.state.isReporting}
                    className="w-full text-gray-500 hover:text-gray-700"
                  >
                    <Bug className="w-4 h-4 mr-2" />
                    {this.state.isReporting ? '리포트 생성 중...' : '에러 리포트 복사'}
                  </Button>
                )}
              </div>

              {/* 추가 도움말 */}
              <div className="mt-6 pt-4 border-t border-gray-200">
                <p className="text-xs text-gray-500 leading-relaxed">
                  문제가 계속 발생하면 브라우저 캐시를 삭제하거나<br />
                  다른 브라우저를 사용해보세요.
                </p>
              </div>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}