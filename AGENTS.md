# AGENTS.md

이 문서는 이 저장소에서 작업하는 AI 코딩 에이전트를 위한 운영 규칙입니다.
상세 프로젝트 설명은 [`README.md`](./README.md), [`ROADMAP.md`](./ROADMAP.md), [`RETROSPECTIVE.md`](./RETROSPECTIVE.md), [`docs/사전과제.md`](./docs/사전과제.md)를 참고합니다.

---

## 1. 과제 목표

- Android(Kotlin + Jetpack Compose)로 암호화폐 종목 리스트와 실시간 호가창 구현
- Must-have
  - REST로 종목 리스트 조회
  - 호가창(매수/매도 + 현재가) 표시
  - WebSocket 실시간 갱신
- 제출물
  - 동작하는 앱
  - `README.md`
  - `ROADMAP.md`
  - `RETROSPECTIVE.md`
  - squash 하지 않은 커밋 히스토리

---

## 2. 문서 운영 규칙

문서는 변경이 생겼을 때만 갱신합니다. 사소한 수정마다 기계적으로 반복 갱신하지 않습니다.

### README.md

다음 중 하나가 바뀔 때만 갱신합니다.

- 거래소 선택/변경
- 라이브러리 추가/교체
- 아키텍처 또는 모듈 구조 변경
- 빌드/실행 방법 변경
- 요구사항 해석이나 가정 변경

### ROADMAP.md

다음 중 하나가 생기면 갱신합니다.

- 의도적으로 미구현/보류한 항목
- 테스트를 생략했거나 범위를 제한한 항목
- 후속 리팩토링이나 TODO 백로그

### RETROSPECTIVE.md

의미 있는 AI 상호작용만 기록합니다.

- 설계 결정, 디버깅, 리팩토링에 AI가 실질적으로 기여했고 결과가 반영됨
- AI 제안을 수정하거나 거부함
- AI가 만든 실수를 발견해 바로잡음

다음은 기록하지 않습니다.

- 단순 보일러플레이트 작성
- 사소한 rename, import 정리, 포맷 수정
- 판단 근거가 거의 없는 작은 dependency 이동

기록 형식은 기존 `RETROSPECTIVE.md`의 사례 형식을 따릅니다.

---

## 3. 기술 기본값

- Kotlin + Jetpack Compose
- Upbit 우선 검토
- Clean Architecture(data/domain/ui) + MVVM
- Coroutines + Flow, WebSocket은 `callbackFlow` 우선
- Retrofit + OkHttp WebSocket
- Hilt
- kotlinx.serialization
- Coil(필요 시)
- 테스트: JUnit4 + Turbine + MockK

과한 레이어링은 피합니다. 이 과제에서는 3~4개 레이어 이내를 기본값으로 봅니다.

---

## 4. 핵심 제약

- 호가창을 REST polling으로 구현하지 않습니다.
- WebSocket을 `GlobalScope`에서 돌리지 않습니다.
- 역직렬화/정렬 같은 무거운 작업을 UI 스레드에서 처리하지 않습니다.
- WebSocket은 라이프사이클에 맞춰 구독 해제되도록 유지합니다.
- 재연결, 연결 상태 UI, 백프레셔 처리는 항상 고려합니다.

---

## 5. 커밋과 마무리

- squash 금지
- 커밋은 작고 의미 있게 유지
- 커밋 메시지는 가능하면 `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:` 사용
- 문서 갱신은 관련 기능 커밋에 포함하거나 바로 뒤의 `docs:` 커밋으로 남김
- 세션 종료 전 확인
  - 필요한 문서가 실제 변경에 맞게 갱신됐는지
  - 미완/보류 항목이 있으면 `ROADMAP.md`에 남겼는지
  - 코드나 빌드 설정을 건드렸다면 `./gradlew assembleDebug`가 통과하는지

---

## 6. 금지사항

- 회고 문서를 마지막에 몰아서 쓰기
- force push나 히스토리 정리로 과정 숨기기
- 실명, API 키, 민감 정보 커밋
