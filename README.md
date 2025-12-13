
<p align="center">
  <img src="assets/logo.png" alt="HealthChat+ Logo" width="280" />
</p>


## 한 줄 소개 (One-liner)
**자연어 한 줄 입력 → 식단·운동·감정 자동 구조화 → 근거 기반 초개인화 피드백 제공**

예:  
> “아침에 김밥 먹고 점심엔 라면 2개, 저녁에 30분 뛰었어. 오늘 좀 우울했어.”  
→ AI가 식단/운동/감정을 분리·분석하고 **Daily Log**로 통합 후 코칭 생성

---
**AI 기반 초개인화 건강 코치 플랫폼**

자연어로 입력한 하루 기록을 AI가 자동으로 분석하여  
식단 · 운동 · 감정 데이터를 구조화하고,  
**공신력 있는 근거(WHO · KDCA · KDRI)** 기반의 맞춤형 건강 피드백을 제공하는 서비스입니다.
## 기획 의도 (Problem → Goal)
기존 건강 관리 서비스는
- 기록 형식이 복잡해 **꾸준히 사용하기 어렵고**
- AI 피드백이 있어도 **근거가 부족해 신뢰하기 어렵다**는 한계가 있습니다.

---
### 개발 목표 (Planning Goals)
- **기록 부담 최소화**: 사용자가 앱 형식에 맞춰 쓰지 않아도 자연어 한 문장으로 기록
- **근거 기반 피드백**: WHO/KDCA/KDRI 등 공신력 있는 기준을 근거로 제시
- **초개인화 코칭**: 사용자 프로필(목표/신체/수면/알레르기 등)을 반영한 맞춤 분석
- **하루 단위 통합 관리**: 식단·운동·감정을 **Daily Log** 중심으로 통합

---

## 핵심 기능 (Core Features)
### 🧠 자연어 분석 & 라우팅
- 사용자 입력 문장을 **식단/운동/감정 영역으로 자동 분리**
- Gemini API 기반 **구조화(JSON 변환)**
- 누락/불완전 입력에 대한 **fallback 로직**

### 🍽 식단 · 🏃 운동 · ❤️ 감정 분석
- 식단: 음식명 및 섭취 맥락 분석
- 운동: 종류/시간/강도 기반 활동 분석
- 감정: 감정 카테고리 및 상태 분석

### 📒 일일 로그(Daily Log) 통합
- 식단·운동·감정을 **하루 단위로 통합 저장**
- 날짜별 조회 및 수정
- 대시보드용 집계 데이터 생성

### 🤖 AI Health Coach (근거 기반 피드백)
- Daily Log 기반 종합 요약/코칭 생성
- WHO·KDCA·KDRI 등 기준을 **RAG DB로 참조**하여 근거 제시
- 목표 기반 개선 방향 제안

### 👤 사용자 프로필(초개인화)
- 키/몸무게/목표/수면/알레르기 설정
- 프로필 기반 개인화 분석 반영

---

## 시스템 아키텍처 (Architecture)
<p align="center">
  <img src="assets/architecture-healthchat.png" alt="HealthChat+ Architecture" width="900"/>
</p>

- **Routing Service**가 자연어 입력을 식단·운동·감정 분석 서비스로 분기
- 분석 결과를 **Daily Meal / Daily Exercise / Daily Emotion**으로 저장
- **Daily Log**로 통합 후 **AI Health Coach**가 종합 피드백 생성
- WHO·KDRI 등 문헌은 **RAG DB**를 통해 근거 기반 참조

---

## 기술 스택 (Tech Stack)
- **Frontend**: React, TypeScript, Tailwind CSS  
- **Backend**: Spring Boot, MySQL  
- **AI/ML**: Gemini API  
- **Infra**: AWS EC2, Docker, Git  

---

## 스크린샷 (Screenshots)
### 📸 대시보드 메인 화면
<img src="assets/screenshot-dashboard.png" alt="Dashboard" width="100%"/>

### 📸 AI 분석 결과 & 근거 기반 피드백
<img src="assets/screenshot-feedback.png" alt="Feedback" width="100%"/>

### 📸 자연어 입력 기반 하루 로그
<img src="assets/screenshot-input.png" alt="Input" width="100%"/>

---

## 🎬 시연 영상 (Demo)

<video
  src="assets/demo_web.mp4"
  controls
  muted
  playsinline
  width="100%">
  브라우저가 video 태그를 지원하지 않습니다.
</video>

---

### ▶ YouTube 시연 영상
- https://www.youtube.com/watch?v=QBMxtf1YZhg


## Contact
- Email: [jeonyu2589@naver.com](mailto:jeonyu2589@naver.com)  
- GitHub: https://github.com/Jeon03
