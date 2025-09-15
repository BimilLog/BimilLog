/**
 * 개인 성장 트래커 유틸리티
 * 사용자의 성장 목표 설정 및 진행도 추적
 */

export interface GrowthGoal {
  id: string;
  title: string;
  description: string;
  category: 'writing' | 'reading' | 'social' | 'learning' | 'habit';
  targetValue: number;
  currentValue: number;
  unit: string; // 예: "개", "시간", "일"
  startDate: string;
  endDate: string;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
  isActive: boolean;
  isCompleted: boolean;
  milestones: Milestone[];
}

export interface Milestone {
  id: string;
  value: number;
  label: string;
  achievedAt?: string;
}

export interface GrowthStreak {
  type: 'daily' | 'weekly' | 'monthly';
  currentStreak: number;
  longestStreak: number;
  lastActiveDate: string;
  startDate: string;
}

export interface GrowthStats {
  totalGoals: number;
  completedGoals: number;
  activeGoals: number;
  completionRate: number;
  averageProgress: number;
  streaks: GrowthStreak[];
  categoryProgress: Record<string, number>;
}

export interface DailyLog {
  date: string;
  goals: {
    goalId: string;
    progress: number;
    note?: string;
  }[];
  reflection?: string;
  mood?: 'great' | 'good' | 'neutral' | 'bad' | 'terrible';
  createdAt: string;
}

const GOALS_KEY = 'bimillog_growth_goals';
const DAILY_LOGS_KEY = 'bimillog_daily_logs';
const STREAKS_KEY = 'bimillog_growth_streaks';

/**
 * 목표 생성
 */
export function createGoal(goal: Omit<GrowthGoal, 'id' | 'createdAt' | 'updatedAt' | 'currentValue' | 'isCompleted' | 'milestones'>): GrowthGoal {
  const newGoal: GrowthGoal = {
    ...goal,
    id: crypto.randomUUID(),
    currentValue: 0,
    isCompleted: false,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    milestones: generateMilestones(goal.targetValue, goal.unit),
  };

  const goals = getGoals();
  goals.push(newGoal);
  saveGoals(goals);

  return newGoal;
}

/**
 * 마일스톤 자동 생성
 */
function generateMilestones(targetValue: number, unit: string): Milestone[] {
  const milestones: Milestone[] = [];
  const steps = [25, 50, 75, 100];

  steps.forEach(percentage => {
    const value = Math.round((targetValue * percentage) / 100);
    milestones.push({
      id: crypto.randomUUID(),
      value,
      label: `${percentage}% 달성 (${value}${unit})`,
    });
  });

  return milestones;
}

/**
 * 목표 업데이트
 */
export function updateGoal(goalId: string, updates: Partial<GrowthGoal>): void {
  const goals = getGoals();
  const index = goals.findIndex(g => g.id === goalId);

  if (index !== -1) {
    goals[index] = {
      ...goals[index],
      ...updates,
      updatedAt: new Date().toISOString(),
    };

    // 목표 달성 확인
    if (goals[index].currentValue >= goals[index].targetValue && !goals[index].isCompleted) {
      goals[index].isCompleted = true;
      goals[index].completedAt = new Date().toISOString();
    }

    // 마일스톤 달성 확인
    goals[index].milestones.forEach(milestone => {
      if (!milestone.achievedAt && goals[index].currentValue >= milestone.value) {
        milestone.achievedAt = new Date().toISOString();
      }
    });

    saveGoals(goals);
  }
}

/**
 * 목표 진행도 업데이트
 */
export function updateGoalProgress(goalId: string, progress: number, note?: string): void {
  const goals = getGoals();
  const goal = goals.find(g => g.id === goalId);

  if (goal) {
    const oldValue = goal.currentValue;
    goal.currentValue = Math.min(goal.targetValue, goal.currentValue + progress);
    goal.updatedAt = new Date().toISOString();

    // 마일스톤 체크
    goal.milestones.forEach(milestone => {
      if (!milestone.achievedAt && goal.currentValue >= milestone.value && oldValue < milestone.value) {
        milestone.achievedAt = new Date().toISOString();
      }
    });

    // 완료 체크
    if (goal.currentValue >= goal.targetValue && !goal.isCompleted) {
      goal.isCompleted = true;
      goal.completedAt = new Date().toISOString();
      goal.isActive = false;
    }

    saveGoals(goals);

    // 일일 로그 추가
    addDailyLog(goalId, progress, note);
  }
}

/**
 * 일일 로그 추가
 */
export function addDailyLog(goalId: string, progress: number, note?: string): void {
  const today = new Date().toISOString().split('T')[0];
  const logs = getDailyLogs();

  let todayLog = logs.find(log => log.date === today);

  if (!todayLog) {
    todayLog = {
      date: today,
      goals: [],
      createdAt: new Date().toISOString(),
    };
    logs.push(todayLog);
  }

  const goalLog = todayLog.goals.find(g => g.goalId === goalId);
  if (goalLog) {
    goalLog.progress += progress;
    if (note) goalLog.note = note;
  } else {
    todayLog.goals.push({ goalId, progress, note });
  }

  saveDailyLogs(logs);
  updateStreaks();
}

/**
 * 일일 반성 추가
 */
export function addDailyReflection(date: string, reflection: string, mood?: DailyLog['mood']): void {
  const logs = getDailyLogs();
  let log = logs.find(l => l.date === date);

  if (!log) {
    log = {
      date,
      goals: [],
      createdAt: new Date().toISOString(),
    };
    logs.push(log);
  }

  log.reflection = reflection;
  if (mood) log.mood = mood;

  saveDailyLogs(logs);
}

/**
 * 스트릭 업데이트
 */
function updateStreaks(): void {
  const logs = getDailyLogs();
  const today = new Date().toISOString().split('T')[0];
  const yesterday = new Date(Date.now() - 86400000).toISOString().split('T')[0];

  const streaks = getStreaks();

  // 일일 스트릭
  let dailyStreak = streaks.find(s => s.type === 'daily');
  if (!dailyStreak) {
    dailyStreak = {
      type: 'daily',
      currentStreak: 0,
      longestStreak: 0,
      lastActiveDate: '',
      startDate: today,
    };
    streaks.push(dailyStreak);
  }

  const todayLog = logs.find(l => l.date === today);

  if (todayLog && todayLog.goals.length > 0) {
    if (dailyStreak.lastActiveDate === yesterday) {
      dailyStreak.currentStreak++;
    } else if (dailyStreak.lastActiveDate !== today) {
      dailyStreak.currentStreak = 1;
      dailyStreak.startDate = today;
    }
    dailyStreak.lastActiveDate = today;
    dailyStreak.longestStreak = Math.max(dailyStreak.longestStreak, dailyStreak.currentStreak);
  }

  saveStreaks(streaks);
}

/**
 * 목표 목록 가져오기
 */
export function getGoals(): GrowthGoal[] {
  if (typeof window === 'undefined') return [];

  try {
    const stored = localStorage.getItem(GOALS_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (error) {
    console.error('Failed to get goals:', error);
  }

  return [];
}

/**
 * 활성 목표 가져오기
 */
export function getActiveGoals(): GrowthGoal[] {
  return getGoals().filter(g => g.isActive && !g.isCompleted);
}

/**
 * 완료된 목표 가져오기
 */
export function getCompletedGoals(): GrowthGoal[] {
  return getGoals().filter(g => g.isCompleted);
}

/**
 * 카테고리별 목표 가져오기
 */
export function getGoalsByCategory(category: GrowthGoal['category']): GrowthGoal[] {
  return getGoals().filter(g => g.category === category);
}

/**
 * 목표 저장
 */
function saveGoals(goals: GrowthGoal[]): void {
  if (typeof window === 'undefined') return;
  localStorage.setItem(GOALS_KEY, JSON.stringify(goals));
}

/**
 * 일일 로그 가져오기
 */
export function getDailyLogs(): DailyLog[] {
  if (typeof window === 'undefined') return [];

  try {
    const stored = localStorage.getItem(DAILY_LOGS_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (error) {
    console.error('Failed to get daily logs:', error);
  }

  return [];
}

/**
 * 특정 날짜의 로그 가져오기
 */
export function getDailyLog(date: string): DailyLog | null {
  return getDailyLogs().find(log => log.date === date) || null;
}

/**
 * 일일 로그 저장
 */
function saveDailyLogs(logs: DailyLog[]): void {
  if (typeof window === 'undefined') return;

  // 최대 365일치만 보관
  const cutoffDate = new Date();
  cutoffDate.setDate(cutoffDate.getDate() - 365);
  const filtered = logs.filter(log => new Date(log.date) > cutoffDate);

  localStorage.setItem(DAILY_LOGS_KEY, JSON.stringify(filtered));
}

/**
 * 스트릭 가져오기
 */
export function getStreaks(): GrowthStreak[] {
  if (typeof window === 'undefined') return [];

  try {
    const stored = localStorage.getItem(STREAKS_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (error) {
    console.error('Failed to get streaks:', error);
  }

  return [];
}

/**
 * 스트릭 저장
 */
function saveStreaks(streaks: GrowthStreak[]): void {
  if (typeof window === 'undefined') return;
  localStorage.setItem(STREAKS_KEY, JSON.stringify(streaks));
}

/**
 * 성장 통계 계산
 */
export function calculateGrowthStats(): GrowthStats {
  const goals = getGoals();
  const activeGoals = goals.filter(g => g.isActive && !g.isCompleted);
  const completedGoals = goals.filter(g => g.isCompleted);
  const streaks = getStreaks();

  // 카테고리별 진행도
  const categoryProgress: Record<string, number> = {};
  const categories: GrowthGoal['category'][] = ['writing', 'reading', 'social', 'learning', 'habit'];

  categories.forEach(category => {
    const categoryGoals = goals.filter(g => g.category === category);
    if (categoryGoals.length > 0) {
      const totalProgress = categoryGoals.reduce((sum, g) =>
        sum + (g.currentValue / g.targetValue) * 100, 0
      );
      categoryProgress[category] = totalProgress / categoryGoals.length;
    }
  });

  // 평균 진행도
  const averageProgress = activeGoals.length > 0
    ? activeGoals.reduce((sum, g) => sum + (g.currentValue / g.targetValue) * 100, 0) / activeGoals.length
    : 0;

  return {
    totalGoals: goals.length,
    completedGoals: completedGoals.length,
    activeGoals: activeGoals.length,
    completionRate: goals.length > 0 ? (completedGoals.length / goals.length) * 100 : 0,
    averageProgress,
    streaks,
    categoryProgress,
  };
}

/**
 * 목표 삭제
 */
export function deleteGoal(goalId: string): void {
  const goals = getGoals().filter(g => g.id !== goalId);
  saveGoals(goals);
}

/**
 * 데이터 초기화
 */
export function clearGrowthData(): void {
  if (typeof window === 'undefined') return;

  localStorage.removeItem(GOALS_KEY);
  localStorage.removeItem(DAILY_LOGS_KEY);
  localStorage.removeItem(STREAKS_KEY);
}

/**
 * 데이터 내보내기
 */
export function exportGrowthData(): string {
  return JSON.stringify({
    goals: getGoals(),
    dailyLogs: getDailyLogs(),
    streaks: getStreaks(),
    stats: calculateGrowthStats(),
    exportedAt: new Date().toISOString(),
  }, null, 2);
}