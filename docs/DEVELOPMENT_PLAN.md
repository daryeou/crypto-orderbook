# 개발 기획 문서

과제 요구사항, 현재 구현 상태, 회고 내용을 바탕으로 재구성한 개발 기획 문서입니다. 
초기 스파이크와 구현 과정에서 검증된 판단을 반영해 보강했으며, 
이후 세부 개선 작업을 위임하거나 검토할 때 해당 문서로 사용합니다.

## 프로젝트 개요

- 목표: Android 앱에서 암호화폐 종목 리스트와 실시간 호가창을 제공
- 기술 스택: Kotlin, Jetpack Compose, Coroutines/Flow, Hilt, Retrofit, OkHttp WebSocket, kotlinx.serialization
- 기본 방향: 과제 범위에 맞는 최소 멀티모듈 구조와 검증 가능한 상태 관리 구조를 유지, 테스트 구현

## 기능 구현 목표

- 호가창에서 매수/매도 호가와 현재가를 보여준다.
  - REST API로 종목 리스트를 제공한다.
  - 현재가와 24시간 변동률은 polling으로 최신성을 보완한다.
- 호가창은 WebSocket으로 실시간 갱신한다.
  - 호가창에 REST polling을 사용하지 않는다.
- 각 화면에 Loading, Error(네트워크 활성 상태/Rest API, Socket 오류), Success 상태를 추가.
- UI 프리뷰는 별도의 Debug 소스셋에 구현
- Repository 검증을 위해 Unit Test를 사용
- 구현 결과뿐 아니라 구현 및 검증 과정 문서화도 중요하므로, 결정 근거와 보류 항목을 문서로 남긴다.

## 범위와 비범위

### 범위

- Upbit KRW 마켓 종목 리스트
- 종목별 현재가와 24시간 변동률 표시
- 종목 선택 시 실시간 호가창 진입
- 현재가, 매도 호가, 매수 호가, 로딩/에러 상태 표시
- 최소 ViewModel/Repository 테스트

### 비범위

- 복수 거래소 지원
- 차트, 즐겨찾기, 정렬/필터 고도화
- 자동 재연결 백오프 완성
- 오프라인 캐시와 stale 데이터 유지 정책
- 디자인 시스템 모듈 분리

## 거래소 및 API 선택 근거

### 거래소 선택

- 선택: Upbit
- 이유:
  - 한국 환경에서 검증하기 쉽다.
  - KRW 마켓이 명확하고 과제 요구사항과 잘 맞는다.
  - REST와 WebSocket 문서가 분리되어 있어 역할 분담이 명확하다.

### 사용 API

- 종목 메타데이터: `GET /v1/market/all`
- 종목 현재가/변동률: `GET /v1/ticker`
- 실시간 호가: `wss://api.upbit.com/websocket/v1` `orderbook`
- 실시간 현재가: `wss://api.upbit.com/websocket/v1` `ticker`

## 화면 구성과 사용자 흐름

1. 앱 실행 시 KRW 마켓 리스트를 표시한다.
2. 리스트에서는 종목명, 심볼, 현재가, 24시간 변동률을 보여준다.
3. 사용자가 종목을 선택하면 해당 마켓의 호가창 화면으로 이동한다.
4. 호가창에서는 매도 호가, 현재가, 매수 호가를 표시한다.
5. 호가창 연결 실패 시 에러 상태를 표시한다.
   - 네트워크 오프라인: 네트워크 연결 요청 화면 표시
   - 소켓 오류: 새로고침 버튼 제공

## 아키텍처와 모듈 구조

### 아키텍처

- 기본 구조: `Clean Architecture + MVVM + StateFlow`
- 의존 방향: `data <- domain -> feature ViewModel`
- 표현 계층: `Route + Screen` 분리
  - `Route`: Hilt 주입, 상태 수집, 액션 전달
  - `Screen`: 순수 UI 렌더링

### 모듈 구조

```text
app
core:model
core:domain
core:network
core:data
feature:market
feature:orderbook
```

### 모듈 역할

- `app`: Application, Activity, Navigation3 NavHost
- `core:model`: 순수 모델
- `core:domain`: repository contract, use case
- `core:network`: REST/WS DTO와 클라이언트
- `core:data`: repository 구현과 DI binding
- `feature:market`: 종목 리스트 화면과 ViewModel
- `feature:orderbook`: 호가창 화면과 ViewModel

## 데이터 흐름

### 종목 리스트

1. `MarketRepository`가 `market/all`과 `ticker`를 조합한다.
2. `ObserveMarketSummariesUseCase`가 polling 결과를 노출한다.
3. `MarketListViewModel`이 `MarketListContract.UiState`로 변환한다.
4. `MarketListScreen`이 상태를 렌더링한다.

### 호가창

1. `UpbitWebSocketClient`가 `callbackFlow`로 raw frame을 방출한다.
2. `OrderBookRepository`가 `orderbook`과 `ticker`를 `OrderBookPayload`로 병합한다.
3. `ObserveOrderBookUseCase`가 종목별 스트림을 노출한다.
4. `OrderBookViewModel`이 payload를 누적해 `OrderBookContract.UiState`로 변환한다.
5. `OrderBookScreen`이 상태별 UI를 렌더링한다.

## 상태 관리 전략

- 종목 리스트와 호가창 모두 `*Contract.UiState` sealed interface를 사용한다.
- ViewModel은 `StateFlow` 하나만 외부에 노출한다.
- 새로고침은 상태값이 아니라 이벤트이므로 `MutableSharedFlow<Unit>` 기반으로 처리한다.
- `flatMapLatest + stateIn(WhileSubscribed)` 구조를 사용해 화면 구독 상태에 맞춰 upstream 수집을 제어한다.
- Compose Preview는 `src/debug`에 두고 ViewModel 없이 샘플 상태만 주입한다.
- 모든 ViewModel 내 인수 전달은 Navigation3 + Hilt assisted injection을 사용한다.

## 에러 처리 및 재시도 정책

- `OrderBook` 에러는 `NETWORK`와 `SOCKET`으로 구분한다.
- 네트워크 연결이 없으면 WebSocket 구독 전에 `NETWORK`를 방출한다.
- 소켓 통신 중 오류가 나면 `SOCKET`을 방출한다.
- 화면 메시지는 ViewModel에 하드코딩하지 않고 `strings.xml`에서 결정한다.
- `SOCKET`일 때만 새로고침 버튼을 노출한다.
- `MarketList`는 현재 단순 `Error` 상태와 재시도 버튼만 제공한다.

## 테스트 전략

- `OrderBookViewModel`
  - 초기 로딩 상태
  - orderbook/ticker 누적 성공 상태
  - 오프라인 시 `NETWORK` 에러
  - 온라인 소켓 실패 시 `SOCKET` 에러
  - retry 후 재구독
- `MarketListViewModel`
  - 초기 로딩과 성공 상태
  - 실패 상태
  - retry 후 polling 재시작
- `OrderBookRepositoryImpl`
  - frame 병합과 에러 매핑 검증
- 검증 명령:
  - `./gradlew testDebugUnitTest`
  - `./gradlew assembleDebug`

## 개발 단계 계획

1. 거래소 API와 요구사항 해석을 고정한다.
2. 최소 동작 스파이크로 REST와 WebSocket 연결 가능성을 확인한다.
3. 멀티모듈 구조와 domain contract를 정한다.
4. 종목 리스트를 먼저 완성한다.
5. 호가창과 실시간 갱신을 완성한다.
6. 상태 관리와 에러 정책을 정리한다.
7. 테스트와 문서를 보강한다.

## 남은 이슈와 후속 과제

- WebSocket 자동 재연결과 백오프
- `OrderBook` 네트워크 복구 시 자동 재구독 여부 결정
- `MarketList` 빈 상태 전용 UI
- stale 데이터 유지 정책 여부 결정
- 정렬/필터와 사용자 편의 기능 확장
