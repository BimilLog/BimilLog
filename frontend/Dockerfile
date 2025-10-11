# 1. 빌드 단계
FROM node:20-alpine AS builder
WORKDIR /app

# 패키지 파일 먼저 복사 (의존성 캐싱 활용)
COPY package.json package-lock.json ./
RUN npm ci

# 소스코드 복사 (node_modules, .git 등은 .dockerignore로 제외)
COPY . .

# Next.js 빌드 실행 (메모리 제한 설정)
ENV NODE_OPTIONS="--max-old-space-size=8192"
RUN npm run build

# 2. 실행 단계 (Node.js) - Standalone 모드 사용
FROM node:20-alpine AS runner
WORKDIR /app

# 비루트 사용자 생성
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# Standalone 출력만 복사 (필수 의존성만 포함, ~100-200MB)
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
# 정적 파일 복사
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static
# Public 파일 복사
COPY --from=builder --chown=nextjs:nodejs /app/public ./public

USER nextjs

EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"
ENV NODE_ENV=production

# Standalone 모드는 server.js를 직접 실행
CMD ["node", "server.js"]
