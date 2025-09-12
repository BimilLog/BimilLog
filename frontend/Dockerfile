# 1. 빌드 단계
FROM node:20-alpine AS builder
WORKDIR /app

# 패키지 파일 먼저 복사 (의존성 캐싱 활용)
COPY package.json package-lock.json ./
RUN npm ci

# 소스코드 복사 (node_modules, .git 등은 .dockerignore로 제외)
COPY . .

# Next.js 빌드 실행
RUN npm run build

# 2. 실행 단계 (Node.js)
FROM node:20-alpine AS runner
WORKDIR /app

# 비루트 사용자 생성
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# 패키지 파일과 의존성 복사
COPY --from=builder /app/package.json ./package.json
COPY --from=builder /app/node_modules ./node_modules

# Next.js 빌드 결과물 복사
COPY --from=builder --chown=nextjs:nodejs /app/.next ./.next
COPY --from=builder /app/public ./public

USER nextjs

EXPOSE 3000
ENV PORT 3000
ENV HOSTNAME "0.0.0.0"

CMD ["npm", "start"]
