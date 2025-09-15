"use client";

import React, { useState } from 'react';
import { Card, Button, Input } from '@/components';
import { useGrowthTracker } from '@/hooks/features/useGrowthTracker';
import {
  Target,
  TrendingUp,
  Trophy,
  Plus,
  Edit2,
  Trash2,
  CheckCircle,
  Circle,
  Calendar,
  Flame,
  Download,
  BookOpen,
  Users,
  Brain,
  Heart,
  X,
  Star,
} from 'lucide-react';
import { type GrowthGoal } from '@/lib/utils/growth-tracker';

const CATEGORY_ICONS = {
  writing: <Edit2 className="w-5 h-5" />,
  reading: <BookOpen className="w-5 h-5" />,
  social: <Users className="w-5 h-5" />,
  learning: <Brain className="w-5 h-5" />,
  habit: <Heart className="w-5 h-5" />,
};

const CATEGORY_NAMES = {
  writing: '글쓰기',
  reading: '독서',
  social: '소셜',
  learning: '학습',
  habit: '습관',
};

const CATEGORY_COLORS = {
  writing: '#3b82f6',
  reading: '#10b981',
  social: '#f59e0b',
  learning: '#8b5cf6',
  habit: '#ef4444',
};

export const GrowthTracker = React.memo(() => {
  const {
    activeGoals,
    stats,
    streaks,
    todayLog,
    isLoading,
    addGoal,
    updateProgress,
    removeGoal,
    addReflection,
    getGoalProgress,
    getNextMilestone,
    getRecentAchievements,
    exportData,
    clearData,
  } = useGrowthTracker();

  const [showAddGoal, setShowAddGoal] = useState(false);
  const [selectedGoal, setSelectedGoal] = useState<GrowthGoal | null>(null);
  const [progressInput, setProgressInput] = useState('');
  const [progressNote, setProgressNote] = useState('');
  const [reflectionInput, setReflectionInput] = useState('');
  const [selectedMood, setSelectedMood] = useState<'great' | 'good' | 'neutral' | 'bad' | 'terrible'>('neutral');

  // 새 목표 폼 상태
  const [newGoalForm, setNewGoalForm] = useState({
    title: '',
    description: '',
    category: 'habit' as GrowthGoal['category'],
    targetValue: '',
    unit: '일',
    endDate: '',
  });

  const handleAddGoal = () => {
    if (newGoalForm.title && newGoalForm.targetValue && newGoalForm.endDate) {
      addGoal({
        title: newGoalForm.title,
        description: newGoalForm.description,
        category: newGoalForm.category,
        targetValue: parseInt(newGoalForm.targetValue),
        unit: newGoalForm.unit,
        startDate: new Date().toISOString(),
        endDate: new Date(newGoalForm.endDate).toISOString(),
        isActive: true,
      });

      // 폼 초기화
      setNewGoalForm({
        title: '',
        description: '',
        category: 'habit',
        targetValue: '',
        unit: '일',
        endDate: '',
      });
      setShowAddGoal(false);
    }
  };

  const handleUpdateProgress = (goalId: string) => {
    const value = parseFloat(progressInput);
    if (!isNaN(value) && value > 0) {
      updateProgress(goalId, value, progressNote);
      setProgressInput('');
      setProgressNote('');
      setSelectedGoal(null);
    }
  };

  const handleAddReflection = () => {
    if (reflectionInput.trim()) {
      addReflection(reflectionInput, selectedMood);
      setReflectionInput('');
      setSelectedMood('neutral');
    }
  };

  const recentAchievements = getRecentAchievements(3);

  if (isLoading) {
    return (
      <div className="space-y-4">
        <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
          <Target className="w-5 h-5" />
          개인 성장 트래커
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[...Array(6)].map((_, i) => (
            <Card key={i} className="p-4 animate-pulse">
              <div className="h-32 bg-gray-200 rounded" />
            </Card>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold flex items-center gap-2">
          <Target className="w-5 h-5" />
          개인 성장 트래커
        </h2>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setShowAddGoal(true)}
            className="flex items-center gap-1"
          >
            <Plus className="w-4 h-4" />
            목표 추가
          </Button>
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

      {/* 통계 카드 */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <Card className="p-4 bg-gradient-to-br from-blue-50 to-indigo-50">
            <div className="flex items-center justify-between mb-2">
              <Target className="w-5 h-5 text-blue-500" />
              <span className="text-2xl font-bold">{stats.activeGoals}</span>
            </div>
            <p className="text-sm text-gray-600">진행 중</p>
          </Card>

          <Card className="p-4 bg-gradient-to-br from-green-50 to-emerald-50">
            <div className="flex items-center justify-between mb-2">
              <Trophy className="w-5 h-5 text-green-500" />
              <span className="text-2xl font-bold">{stats.completedGoals}</span>
            </div>
            <p className="text-sm text-gray-600">완료</p>
          </Card>

          <Card className="p-4 bg-gradient-to-br from-orange-50 to-yellow-50">
            <div className="flex items-center justify-between mb-2">
              <Flame className="w-5 h-5 text-orange-500" />
              <span className="text-2xl font-bold">
                {streaks.find(s => s.type === 'daily')?.currentStreak || 0}
              </span>
            </div>
            <p className="text-sm text-gray-600">연속일</p>
          </Card>

          <Card className="p-4 bg-gradient-to-br from-purple-50 to-pink-50">
            <div className="flex items-center justify-between mb-2">
              <TrendingUp className="w-5 h-5 text-purple-500" />
              <span className="text-2xl font-bold">
                {Math.round(stats.averageProgress)}%
              </span>
            </div>
            <p className="text-sm text-gray-600">평균 진행률</p>
          </Card>
        </div>
      )}

      {/* 활성 목표 */}
      {activeGoals.length > 0 && (
        <div className="space-y-4">
          <h3 className="font-medium text-gray-700">진행 중인 목표</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {activeGoals.map(goal => {
              const progress = getGoalProgress(goal);
              const nextMilestone = getNextMilestone(goal);

              return (
                <Card key={goal.id} className="p-4">
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <div
                        className="w-10 h-10 rounded-full flex items-center justify-center"
                        style={{ backgroundColor: `${CATEGORY_COLORS[goal.category]}20` }}
                      >
                        {CATEGORY_ICONS[goal.category]}
                      </div>
                      <div>
                        <h4 className="font-medium">{goal.title}</h4>
                        <p className="text-xs text-gray-500">
                          {CATEGORY_NAMES[goal.category]}
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={() => removeGoal(goal.id)}
                      className="text-gray-400 hover:text-red-500"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>

                  <p className="text-sm text-gray-600 mb-3">{goal.description}</p>

                  <div className="space-y-2">
                    <div className="flex justify-between text-sm">
                      <span>진행도</span>
                      <span className="font-medium">
                        {goal.currentValue} / {goal.targetValue} {goal.unit}
                      </span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2">
                      <div
                        className="h-2 rounded-full transition-all"
                        style={{
                          width: `${progress}%`,
                          backgroundColor: CATEGORY_COLORS[goal.category],
                        }}
                      />
                    </div>
                  </div>

                  {nextMilestone && (
                    <div className="mt-3 p-2 bg-gray-50 rounded text-xs text-gray-600">
                      다음 마일스톤: {nextMilestone.label}
                    </div>
                  )}

                  <div className="mt-3 flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setSelectedGoal(goal)}
                      className="flex-1"
                    >
                      진행도 업데이트
                    </Button>
                  </div>

                  {/* 마일스톤 표시 */}
                  <div className="mt-3 flex justify-between">
                    {goal.milestones.map((milestone, index) => (
                      <div
                        key={milestone.id}
                        className="flex flex-col items-center"
                        title={milestone.label}
                      >
                        {milestone.achievedAt ? (
                          <CheckCircle
                            className="w-5 h-5"
                            style={{ color: CATEGORY_COLORS[goal.category] }}
                          />
                        ) : (
                          <Circle className="w-5 h-5 text-gray-300" />
                        )}
                        <span className="text-xs text-gray-500 mt-1">
                          {(index + 1) * 25}%
                        </span>
                      </div>
                    ))}
                  </div>
                </Card>
              );
            })}
          </div>
        </div>
      )}

      {/* 최근 달성 */}
      {recentAchievements.length > 0 && (
        <Card className="p-4">
          <h3 className="font-medium mb-3 flex items-center gap-2">
            <Star className="w-4 h-4" />
            최근 달성
          </h3>
          <div className="space-y-2">
            {recentAchievements.map(({ goal, milestone, achievedAt }) => (
              <div
                key={`${goal.id}-${milestone.id}`}
                className="flex items-center justify-between p-2 bg-gray-50 rounded"
              >
                <div className="flex items-center gap-2">
                  <CheckCircle className="w-4 h-4 text-green-500" />
                  <div>
                    <p className="text-sm font-medium">{goal.title}</p>
                    <p className="text-xs text-gray-500">{milestone.label}</p>
                  </div>
                </div>
                <span className="text-xs text-gray-500">
                  {new Date(achievedAt).toLocaleDateString('ko-KR')}
                </span>
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* 오늘의 반성 */}
      <Card className="p-4">
        <h3 className="font-medium mb-3 flex items-center gap-2">
          <Calendar className="w-4 h-4" />
          오늘의 반성
        </h3>
        {todayLog?.reflection ? (
          <div className="p-3 bg-gray-50 rounded">
            <p className="text-sm">{todayLog.reflection}</p>
            {todayLog.mood && (
              <p className="text-xs text-gray-500 mt-2">
                기분: {todayLog.mood === 'great' ? '😄' :
                      todayLog.mood === 'good' ? '🙂' :
                      todayLog.mood === 'neutral' ? '😐' :
                      todayLog.mood === 'bad' ? '😕' : '😢'}
              </p>
            )}
          </div>
        ) : (
          <div className="space-y-3">
            <textarea
              value={reflectionInput}
              onChange={(e) => setReflectionInput(e.target.value)}
              placeholder="오늘 하루를 돌아보며..."
              className="w-full p-3 border rounded-lg resize-none h-24"
            />
            <div className="flex items-center gap-2">
              <span className="text-sm text-gray-600">기분:</span>
              {['great', 'good', 'neutral', 'bad', 'terrible'].map((mood) => (
                <button
                  key={mood}
                  onClick={() => setSelectedMood(mood as 'great' | 'good' | 'neutral' | 'bad' | 'terrible')}
                  className={`text-2xl ${selectedMood === mood ? 'ring-2 ring-purple-500 rounded' : ''}`}
                >
                  {mood === 'great' ? '😄' :
                   mood === 'good' ? '🙂' :
                   mood === 'neutral' ? '😐' :
                   mood === 'bad' ? '😕' : '😢'}
                </button>
              ))}
            </div>
            <Button onClick={handleAddReflection} className="w-full">
              반성 저장
            </Button>
          </div>
        )}
      </Card>

      {/* 목표 추가 모달 */}
      {showAddGoal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <Card className="max-w-md w-full p-6">
            <h3 className="text-lg font-semibold mb-4">새 목표 추가</h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">목표 제목</label>
                <Input
                  value={newGoalForm.title}
                  onChange={(e) => setNewGoalForm({ ...newGoalForm, title: e.target.value })}
                  placeholder="예: 매일 일기 쓰기"
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">설명</label>
                <textarea
                  value={newGoalForm.description}
                  onChange={(e) => setNewGoalForm({ ...newGoalForm, description: e.target.value })}
                  placeholder="목표에 대한 설명..."
                  className="w-full p-2 border rounded-lg resize-none h-20"
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">카테고리</label>
                <select
                  value={newGoalForm.category}
                  onChange={(e) => setNewGoalForm({ ...newGoalForm, category: e.target.value as GrowthGoal['category'] })}
                  className="w-full p-2 border rounded-lg"
                >
                  {Object.entries(CATEGORY_NAMES).map(([key, name]) => (
                    <option key={key} value={key}>{name}</option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">목표값</label>
                  <Input
                    type="number"
                    value={newGoalForm.targetValue}
                    onChange={(e) => setNewGoalForm({ ...newGoalForm, targetValue: e.target.value })}
                    placeholder="30"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">단위</label>
                  <Input
                    value={newGoalForm.unit}
                    onChange={(e) => setNewGoalForm({ ...newGoalForm, unit: e.target.value })}
                    placeholder="일"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">목표 날짜</label>
                <Input
                  type="date"
                  value={newGoalForm.endDate}
                  onChange={(e) => setNewGoalForm({ ...newGoalForm, endDate: e.target.value })}
                  min={new Date().toISOString().split('T')[0]}
                />
              </div>

              <div className="flex gap-2">
                <Button onClick={handleAddGoal} className="flex-1">
                  추가
                </Button>
                <Button
                  variant="outline"
                  onClick={() => setShowAddGoal(false)}
                  className="flex-1"
                >
                  취소
                </Button>
              </div>
            </div>
          </Card>
        </div>
      )}

      {/* 진행도 업데이트 모달 */}
      {selectedGoal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <Card className="max-w-md w-full p-6">
            <h3 className="text-lg font-semibold mb-4">진행도 업데이트</h3>
            <p className="text-sm text-gray-600 mb-4">{selectedGoal.title}</p>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">
                  진행 ({selectedGoal.unit})
                </label>
                <Input
                  type="number"
                  value={progressInput}
                  onChange={(e) => setProgressInput(e.target.value)}
                  placeholder={`예: 1 ${selectedGoal.unit}`}
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">메모 (선택)</label>
                <textarea
                  value={progressNote}
                  onChange={(e) => setProgressNote(e.target.value)}
                  placeholder="오늘의 기록..."
                  className="w-full p-2 border rounded-lg resize-none h-20"
                />
              </div>

              <div className="flex gap-2">
                <Button
                  onClick={() => handleUpdateProgress(selectedGoal.id)}
                  className="flex-1"
                >
                  저장
                </Button>
                <Button
                  variant="outline"
                  onClick={() => {
                    setSelectedGoal(null);
                    setProgressInput('');
                    setProgressNote('');
                  }}
                  className="flex-1"
                >
                  취소
                </Button>
              </div>
            </div>
          </Card>
        </div>
      )}

      {/* 목표가 없을 때 */}
      {activeGoals.length === 0 && !showAddGoal && (
        <Card className="p-8 text-center">
          <Target className="w-12 h-12 mx-auto mb-4 text-gray-400" />
          <p className="text-gray-500 mb-4">아직 설정한 목표가 없습니다.</p>
          <Button
            onClick={() => setShowAddGoal(true)}
            className="mx-auto flex items-center gap-2"
          >
            <Plus className="w-4 h-4" />
            첫 목표 만들기
          </Button>
        </Card>
      )}
    </div>
  );
});

GrowthTracker.displayName = 'GrowthTracker';