"use client";

import React from 'react';
import { Card } from '@/components';
import { useActivityInsights } from '@/hooks/features/useActivityInsights';
import {
  Activity,
  TrendingUp,
  Calendar,
  Clock,
  Award,
  BarChart3,
  Download,
  Trash2,
  Flame,
} from 'lucide-react';

export const ActivityInsights = React.memo(() => {
  const {
    stats,
    weeklyInsights,
    dailyActivity,
    isLoading,
    typeStats,
    hourlyPattern,
    exportData,
    clearData,
  } = useActivityInsights();

  if (isLoading) {
    return (
      <div className="space-y-4">
        <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
          <Activity className="w-5 h-5" />
          활동 인사이트
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[...Array(6)].map((_, i) => (
            <Card key={i} className="p-4 animate-pulse">
              <div className="h-20 bg-gray-200 rounded" />
            </Card>
          ))}
        </div>
      </div>
    );
  }

  if (!stats) {
    return (
      <div className="space-y-4">
        <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
          <Activity className="w-5 h-5" />
          활동 인사이트
        </h2>
        <Card className="p-8 text-center">
          <Activity className="w-12 h-12 mx-auto mb-4 text-gray-400" />
          <p className="text-gray-500">아직 활동 데이터가 없습니다.</p>
          <p className="text-sm text-gray-400 mt-2">
            사이트를 이용하면 자동으로 활동이 기록됩니다.
          </p>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold flex items-center gap-2">
          <Activity className="w-5 h-5" />
          활동 인사이트
        </h2>
        <div className="flex gap-2">
          <button
            onClick={exportData}
            className="p-2 text-gray-600 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
            title="데이터 내보내기"
          >
            <Download className="w-4 h-4" />
          </button>
          <button
            onClick={clearData}
            className="p-2 text-gray-600 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
            title="데이터 초기화"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* 주요 통계 카드 */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Card className="p-4 bg-gradient-to-br from-pink-50 to-purple-50">
          <div className="flex items-center justify-between mb-2">
            <Flame className="w-5 h-5 text-orange-500" />
            <span className="text-2xl font-bold">{stats.activeStreak}</span>
          </div>
          <p className="text-sm text-gray-600">연속 활동일</p>
          <p className="text-xs text-gray-500 mt-1">
            최고: {stats.longestStreak}일
          </p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-blue-50 to-indigo-50">
          <div className="flex items-center justify-between mb-2">
            <BarChart3 className="w-5 h-5 text-blue-500" />
            <span className="text-2xl font-bold">{stats.totalEvents}</span>
          </div>
          <p className="text-sm text-gray-600">전체 활동</p>
          <p className="text-xs text-gray-500 mt-1">
            최근 90일
          </p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-green-50 to-emerald-50">
          <div className="flex items-center justify-between mb-2">
            <TrendingUp className="w-5 h-5 text-green-500" />
            <span className="text-2xl font-bold">
              {weeklyInsights ? `${weeklyInsights.growthRate > 0 ? '+' : ''}${weeklyInsights.growthRate.toFixed(0)}%` : '0%'}
            </span>
          </div>
          <p className="text-sm text-gray-600">주간 성장률</p>
          <p className="text-xs text-gray-500 mt-1">
            지난주 대비
          </p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-purple-50 to-pink-50">
          <div className="flex items-center justify-between mb-2">
            <Clock className="w-5 h-5 text-purple-500" />
            <span className="text-2xl font-bold">{stats.mostActiveHour}시</span>
          </div>
          <p className="text-sm text-gray-600">활발한 시간</p>
          <p className="text-xs text-gray-500 mt-1">
            {stats.mostActiveDay}
          </p>
        </Card>
      </div>

      {/* 활동 타입별 통계 */}
      {typeStats && typeStats.length > 0 && (
        <Card className="p-4">
          <h3 className="font-medium mb-4 flex items-center gap-2">
            <Award className="w-4 h-4" />
            활동 유형별 통계
          </h3>
          <div className="space-y-3">
            {typeStats.map((item) => (
              <div key={item.type} className="flex items-center gap-3">
                <div className="flex-1">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-sm font-medium">{item.type}</span>
                    <span className="text-sm text-gray-500">{item.count}회</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className="h-2 rounded-full transition-all duration-500"
                      style={{
                        backgroundColor: item.color,
                        width: `${(item.count / Math.max(...typeStats.map(t => t.count))) * 100}%`,
                      }}
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* 최근 30일 활동 히트맵 */}
      <Card className="p-4">
        <h3 className="font-medium mb-4 flex items-center gap-2">
          <Calendar className="w-4 h-4" />
          최근 30일 활동
        </h3>
        <div className="grid grid-cols-7 gap-1">
          {['일', '월', '화', '수', '목', '금', '토'].map(day => (
            <div key={day} className="text-xs text-center text-gray-500 mb-1">
              {day}
            </div>
          ))}
          {dailyActivity.map((day) => {
            const intensity = day.eventCount === 0 ? 0 :
              day.eventCount < 5 ? 1 :
              day.eventCount < 10 ? 2 :
              day.eventCount < 20 ? 3 : 4;

            const colors = [
              'bg-gray-100',
              'bg-green-200',
              'bg-green-400',
              'bg-green-600',
              'bg-green-800',
            ];

            return (
              <div
                key={day.date}
                className={`aspect-square rounded ${colors[intensity]} transition-all hover:ring-2 hover:ring-green-400 cursor-pointer`}
                title={`${day.date}: ${day.eventCount}개 활동`}
              />
            );
          })}
        </div>
        <div className="flex items-center gap-2 mt-4 text-xs text-gray-500">
          <span>적음</span>
          <div className="flex gap-1">
            {['bg-gray-100', 'bg-green-200', 'bg-green-400', 'bg-green-600', 'bg-green-800'].map(color => (
              <div key={color} className={`w-3 h-3 rounded ${color}`} />
            ))}
          </div>
          <span>많음</span>
        </div>
      </Card>

      {/* 시간대별 활동 패턴 */}
      <Card className="p-4">
        <h3 className="font-medium mb-4 flex items-center gap-2">
          <Clock className="w-4 h-4" />
          시간대별 활동 패턴
        </h3>
        <div className="flex items-end gap-1 h-32">
          {hourlyPattern.map((hour) => {
            const maxCount = Math.max(...hourlyPattern.map(h => h.count));
            const height = maxCount > 0 ? (hour.count / maxCount) * 100 : 0;

            return (
              <div
                key={hour.hour}
                className="flex-1 relative group"
                title={`${hour.label}: ${hour.count}회`}
              >
                <div
                  className="absolute bottom-0 w-full bg-gradient-to-t from-purple-500 to-pink-500 rounded-t transition-all hover:opacity-80"
                  style={{ height: `${height}%` }}
                />
                {hour.hour % 3 === 0 && (
                  <span className="absolute -bottom-5 left-1/2 transform -translate-x-1/2 text-xs text-gray-500">
                    {hour.hour}
                  </span>
                )}
              </div>
            );
          })}
        </div>
        <div className="mt-6 text-xs text-gray-500 text-center">시간 (24시간)</div>
      </Card>

      {/* 주간 인사이트 */}
      {weeklyInsights && (
        <Card className="p-4 bg-gradient-to-r from-purple-50 to-pink-50">
          <h3 className="font-medium mb-3 flex items-center gap-2">
            <TrendingUp className="w-4 h-4" />
            이번 주 요약
          </h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div>
              <p className="text-gray-500">총 활동</p>
              <p className="font-semibold">{weeklyInsights.totalEvents}회</p>
            </div>
            <div>
              <p className="text-gray-500">일 평균</p>
              <p className="font-semibold">{weeklyInsights.dailyAverage.toFixed(1)}회</p>
            </div>
            <div>
              <p className="text-gray-500">가장 활발한 요일</p>
              <p className="font-semibold">{weeklyInsights.mostActiveDay}</p>
            </div>
            <div>
              <p className="text-gray-500">성장률</p>
              <p className={`font-semibold ${weeklyInsights.growthRate >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                {weeklyInsights.growthRate > 0 && '+'}{weeklyInsights.growthRate.toFixed(0)}%
              </p>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
});

ActivityInsights.displayName = 'ActivityInsights';