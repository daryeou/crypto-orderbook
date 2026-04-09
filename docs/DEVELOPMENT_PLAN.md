# 개발 기획 문서

이 문서는 과제 요구사항, 현재 구현 상태, 중간 검토 결과를 바탕으로 재구성한 개발 기획 문서입니다.  
초기 최소 동작 구현과 이후 구현 과정에서 검증된 판단을 기준으로 정리했으며, 이후 작업 위임과 구현 점검의 기준 문서로 사용합니다.

## 프로젝트 개요

- 목표: Android 앱에서 암호화폐 종목 리스트와 실시간 호가창 제공
- 대상 거래소: Upbit
- 기술 스택: Kotlin, Jetpack Compose, Coroutines/Flow, Hilt, Retrofit, OkHttp WebSocket, kotlinx.serialization
- 기본 방향: 과제 범위에 맞춘 최소 멀티모듈 구조와 검증 가능한 상태 관리

## 과제 요구사항 해석

- REST API로 종목 리스트를 제공해야 한다.
- 매수/매도 호가와 현재가가 포함된 호가창을 제공해야 한다.
- 호가창은 반드시 WebSocket 기반 실시간 갱신이어야 하며 REST polling으로 대체하지 않는다.
- 구현 결과뿐 아니라 설계 판단, 검증 과정, 문서화 흐름이 중요하다.

## 범위와 비범위

### 범위

- Upbit KRW 마켓 종목 리스트
- 종목별 현재가와 24시간 변동률
- 종목 선택 후 실시간 호가창 진입
- 현재가, 매도 호가, 매수 호가 표시
- 로딩/에러/오프라인 상태 처리
- 최소 단위의 ViewModel/Repository 테스트

### 비범위

- 복수 거래소 지원
- 차트, 즐겨찾기, 고급 정렬/필터
- 완성형 재연결 백오프 정책
- 오프라인 캐시와 stale 데이터 정책 고도화
- 디자인 시스템 모듈 분리

## 거래소 및 API 선택 근거

### 거래소 선택

- 선택: Upbit
- 이유:
  - 국내 환경에서 검증이 쉽다.
  - KRW 마켓이 명확해 과제 목적과 잘 맞는다.
  - REST와 WebSocket 문서가 명확하게 분리되어 있다.

### 사용 API

- `GET /v1/market/all`
- `GET /v1/ticker`
- `wss://api.upbit.com/websocket/v1` `orderbook`
- `wss://api.upbit.com/websocket/v1` `ticker`

## 화면 구성과 사용자 흐름

1. 앱 실행 시 KRW 마켓 리스트를 표시한다.
2. 리스트에서 종목명, 현재가, 24시간 변동률을 보여준다.
3. 사용자가 종목을 선택하면 해당 마켓의 호가창 화면으로 이동한다.
4. 호가창에서는 매도 호가, 현재가, 매수 호가를 표시한다.
5. 온라인 상태에서 WebSocket 연결이 실패하면 호가창 전용 재시도 화면을 보여준다.
6. 네트워크가 끊기면 화면별 에러 대신 앱 전역 오버레이를 보여주고 복구를 기다린다.

## 아키텍처와 모듈 구조

### 아키텍처

- 기본 구조: `Clean Architecture + MVVM + StateFlow`
- 의존 방향: `data -> domain -> feature ViewModel`
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

- `app`: Application, Activity, Navigation3 NavHost, 전역 오프라인 UI
- `core:model`: 순수 모델
- `core:domain`: repository contract, use case
- `core:network`: REST/WS DTO와 클라이언트
- `core:data`: repository 구현, connectivity 구현, Hilt binding
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
2. `OrderBookRepository`가 `orderbook`과 `ticker`를 병합해 `OrderBookPayload`를 만든다.
3. `ObserveOrderBookUseCase`가 종목별 스트림을 노출한다.
4. `OrderBookViewModel`이 payload를 누적해 `OrderBookContract.UiState`로 변환한다.
5. `OrderBookScreen`이 상태를 렌더링한다.

### 전역 네트워크 상태

1. `NetworkStatusRepository`가 `observeConnectivity()`와 `isNetworkAvailable()`를 제공한다.
   - 단순히 `activeNetwork` 존재 여부만 보지 않고, `NetworkCapabilities`의 `NET_CAPABILITY_INTERNET`와 `NET_CAPABILITY_VALIDATED`를 함께 확인해 실제 인터넷 가능 상태만 온라인으로 판단한다.
2. `AppRootViewModel`이 연결 상태를 `StateFlow<ConnectivityStatus>`로 노출한다.
3. `CryptoOrderBookApp`이 루트 `SnackbarHost`와 오프라인 오버레이를 제어한다.
4. 각 화면 ViewModel은 같은 connectivity 흐름을 기준으로 온라인 상태에서만 갱신을 유지한다.

## 상태 관리 전략

- 종목 리스트와 호가창 모두 `*Contract.UiState` sealed interface를 사용한다.
- ViewModel은 `StateFlow` 하나만 외부로 노출한다.
- 새로고침은 상태값이 아니라 이벤트이므로 `MutableSharedFlow<Unit>`로 처리한다.
- `flatMapLatest + stateIn(WhileSubscribed)`로 화면 구독 상태에 맞춰 upstream 수집을 제어한다.
- Compose Preview는 `src/debug`에 두고 ViewModel 없이 샘플 상태만 주입한다.
- `OrderBook`는 Navigation3 + Hilt assisted injection으로 nav key를 ViewModel에 전달한다.

## 에러 처리 및 재시도 정책

- 오프라인은 화면 에러가 아니라 앱 전역 상태다.
- 앱 루트는 오프라인 시:
  - 스낵바 1회 노출
  - 반투명 scrim + 중앙 인디케이터 표시
  - 현재 화면 상태 유지, 갱신만 중단
- `MarketListViewModel`
  - 오프라인 시 `Error`를 내지 않는다.
  - 마지막 `Success`가 있으면 그대로 유지한다.
  - 온라인 상태에서 REST polling 실패 시에만 `Error`를 낸다.
  - 연결 복구 시 자동으로 polling을 재개한다.
- `OrderBookViewModel`
  - 오프라인 시 `Error`를 내지 않는다.
  - 마지막 `Success` 또는 `Loading`을 유지한다.
  - 온라인 상태에서 WebSocket 실패 시에만 `Error(SOCKET)`을 낸다.
  - 연결 복구 시 자동으로 재구독한다.
- 수동 `retry()`는 온라인 상태의 실패 화면에서만 사용한다.

## 테스트 전략

- `MarketListViewModel`
  - 초기 `Loading -> Success`
  - 온라인 실패 시 `Error`
  - 오프라인 시 상태 유지
  - 연결 복구 후 자동 polling 재개
  - retry 후 재구독
- `OrderBookViewModel`
  - 초기 `Loading -> Success`
  - orderbook/ticker 누적 성공 상태
  - 온라인 WebSocket 실패 시 `Error(SOCKET)`
  - 오프라인 시 상태 유지
  - 연결 복구 후 자동 재구독
- `NetworkStatusRepositoryImpl`
  - 초기 상태 즉시 방출
  - callback 등록/해제
  - `Connected -> Disconnected -> Connected` 전이
- 검증 명령:
  - `./gradlew testDebugUnitTest`
  - `./gradlew assembleDebug`

## 개발 단계 계획

### 1. 거래소/API 검토
- 거래소 후보와 API 조합을 비교한다.
- REST와 WebSocket으로 요구사항을 충족할 수 있는지 먼저 확인한다.

### 2. 앱 구조 구성
- 모듈 구조, Navigation, DI를 먼저 고정한다.
- 이후 기능을 얹을 최소 실행 구조를 확보한다.

### 3. 종목 리스트 구현
- KRW 마켓 리스트, 현재가, 변동률 표시를 먼저 완성한다.
- REST 기반 데이터 흐름과 기본 화면 상태를 정리한다.

### 4. 호가창 최소 동작 구현
- WebSocket `orderbook`과 `ticker`를 병합해 호가창을 연결한다.
- 현재가 포함 실시간 갱신이 되는 최소 기능을 먼저 확보한다.

### 5. 상태 정책 정리
- contract 기반 `UiState`, retry, assisted injection, connectivity 정책을 정리한다.
- 화면 에러와 앱 전역 오프라인 상태를 분리한다.

### 6. 테스트와 문서 보강
- ViewModel / repository 테스트를 추가한다.
- README, ROADMAP, RETROSPECTIVE를 현재 구현 기준으로 갱신한다.

## 남은 이슈와 후속 과제

- WebSocket 자동 재연결 백오프 정책
- `MarketList` 빈 상태 전용 UI
- stale 데이터 유지 정책 고도화
- 오프라인 복구 후 사용자 안내 방식 세분화
- 정렬/필터와 사용자 편의 기능 확장
