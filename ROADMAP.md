# ROADMAP

미구현 항목, 보류 결정, 그리고 향후 개선 계획을 기록합니다.
작업 중 스킵하거나 TODO로 남긴 항목은 여기에 계속 반영합니다.
---

## 미구현 Must-have

현재 없음.

---

## 미구현 Nice-to-have

### 실시간 연결 / 오프라인 처리
- [x] 전역 오프라인 스낵바 + 반투명 오버레이
- [x] 연결 복구 시 활성 화면 자동 재조회 / 재구독
- [x] 호가창 소켓 오류 전용 retry 화면
- [ ] WebSocket 자동 재연결(backoff / jitter)
- [ ] 장시간 오프라인 이후 stale 데이터 유효 시간 정책 확정
- [ ] 연결 복구 시 UI/UX 세분화

### 화면 상태
- [x] 로딩 상태
- [x] 온라인 실패 시 화면 에러
- [ ] 종목 리스트 빈 상태 전용 UI
- [ ] 호가창 stale 데이터 유지 + 배너 방식 검토

### UI/UX 개선
- [ ] 종목 리스트 정보 위계와 가독성 개선
- [ ] 호가창 시각적 대비와 숫자 가독성 개선
- [ ] 오프라인 / 로딩 / 에러 상태 표현 일관성 점검

### 기타
- [ ] 시장 정렬 / 필터링 옵션
- [ ] 숫자 포맷터 공통화
- [ ] 다크 테마 / 디자인 시스템 분리 여부 재평가

---

## 테스트 전략

### 현재 작성된 테스트

1. `core/data/.../OrderBookRepositoryImplTest.kt`
   - orderbook / ticker frame 병합
   - WebSocket 실패 시 error payload 전이

2. `core/data/.../NetworkStatusRepositoryImplTest.kt`
   - 초기 연결 상태 즉시 방출
   - `Connected -> Disconnected -> Connected` 전이
   - callback 등록 / 해제

3. `feature/market/.../MarketListViewModelTest.kt`
   - 초기 `Loading -> Success`
   - 온라인 REST 실패 시 `Error`
   - 오프라인 시 상태 유지
   - 연결 복구 후 자동 polling 재개
   - retry 후 재구독

4. `feature/orderbook/.../OrderBookViewModelTest.kt`
   - 초기 `Loading -> Success`
   - orderbook / ticker 누적 성공 상태
   - 온라인 WebSocket 실패 시 `Error(SOCKET)`
   - 오프라인 시 상태 유지
   - 연결 복구 후 자동 재구독

### 아직 하지 않은 테스트

- `MarketRepositoryImpl` polling interval / 실패 정책
- 루트 오프라인 오버레이와 스낵바의 Compose UI 테스트
- Navigation3 백스택 복원과 entry-scoped ViewModel 동작 검증

### 의식적으로 제외한 항목

- 전체 E2E 계측 테스트
- Compose 스냅샷 테스트

---

## 리팩토링 백로그

- [ ] `NetworkStatusRepository`의 초기 상태 판별 로직을 별도 helper로 분리할지 검토
- [ ] `MarketListViewModel`, `OrderBookViewModel`의 connectivity trigger 조합을 공통 패턴으로 추출할지 검토
- [ ] `stream_type`과 snapshot / realtime 처리 의도를 repository 주석 또는 문서로 명확화
- [ ] 앱 루트 오프라인 UI를 별도 composable 파일로 분리할지 검토

---

## 확장 아이디어
- 
- 호가창 깊이 차트
- 즐겨찾기 종목 저장
