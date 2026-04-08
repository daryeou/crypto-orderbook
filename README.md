# Crypto Order Book (Android)

Upbit 공개 API를 이용해 KRW 마켓 종목 리스트와 실시간 호가창을 보여주는 Android 앱입니다.

## 빌드 & 실행

### 환경
- **최소 SDK**: 26
- **타겟 SDK**: 36
- **JDK**: 21
- **빌드 도구**: Gradle Wrapper 8.13

### 명령

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Windows에서는 `gradlew.bat`를 사용합니다.

## 거래소 선택 근거

후보 검토:

| 거래소 | WebSocket 호가 | 한국 접근성 | 문서 품질 | 비고 |
|---|---|---|---|---|
| Upbit | ✅ | ✅ | 양호 | KRW 마켓 제공 |
| Bithumb | ✅ | ✅ | 보통 | 응답 구조 해석 비용이 더 큼 |
| Binance | ✅ | ⚠️ | 우수 | 국내 접근성과 제출 검증 환경이 불리할 수 있음 |

**선택**: Upbit

**이유**
- 한국 환경에서 바로 검증하기 쉽습니다.
- REST와 WebSocket 문서가 분리되어 있어 종목 리스트와 호가창 역할을 명확히 나눌 수 있습니다.
- `market/all`, `ticker`, `orderbook`, `ticker` 조합으로 과제의 Must-have를 단순한 구조로 충족할 수 있습니다.

## 아키텍처

```
app
  └─ feature:market / feature:orderbook
       └─ core:domain
            └─ core:data
                 ├─ core:network
                 └─ core:model
```

### 선택 근거
- 과제 규모에 맞춰 `Clean Architecture + MVVM + StateFlow`를 유지했습니다.
- 이전에 진행했던 MVVM 기반 토이 프로젝트의 모듈 분리 경험은 참고하되, 설명 부담이 큰 `core:designsystem`, `core:common` 같은 확장 레이어는 도입하지 않았습니다.
- Repository interface와 UseCase를 `core:domain`으로 올려서 `data -> domain -> viewmodel` 의존 방향을 고정했습니다.
- ViewModel 인터페이스 대신 stateless `Screen`과 Hilt `Route`를 분리해 preview/test seam을 확보했습니다.

### 상태 관리
- 종목 리스트와 호가창 모두 `*Contract.UiState` sealed interface를 사용합니다.
- 각 화면은 `Route`와 `Screen(uiState, onAction...)`로 분리하고, `Route`는 상태 수집과 액션 전달만 담당합니다.
- 두 ViewModel 모두 `refreshTrigger + flatMapLatest + stateIn(WhileSubscribed)` 패턴으로 수집을 시작하고 중단합니다.
- `retry()`는 trigger 값만 갱신하고, 이전 upstream 취소와 새 구독 시작은 `flatMapLatest`에 맡깁니다.

### WebSocket 처리 전략
- `core:network`에서 OkHttp WebSocket을 `callbackFlow`로 감쌌습니다.
- `core:data`에서 `orderbook`과 `ticker` frame을 병합해 단일 `OrderBookPayload`로 변환합니다.
- `OrderBookViewModel`은 Navigation3 key를 assisted injection으로 받아 같은 종목 기준으로 `retry()` 재구독을 수행합니다.
- `orderbook`과 `ticker`가 분리되어 와도 ViewModel reducer가 이전 값을 누적해 하나의 `Success` 상태로 합칩니다.
- WebSocket 오류 시 호가창은 상단 배너 대신 전체 에러 화면을 보여주고, 사용자가 `새로고침`을 눌러 다시 연결합니다.
- 백프레셔는 ViewModel 수집 지점에서 `conflate()`를 적용했습니다.

### Navigation 전략
- 앱 네비게이션은 `Navigation3`의 `rememberNavBackStack`과 `NavDisplay`를 사용합니다.
- destination key는 `@Serializable` + `NavKey`로 구성해 구성 변경 시 back stack을 복원합니다.
- `rememberSaveableStateHolderNavEntryDecorator()`와 `rememberViewModelStoreNavEntryDecorator()`를 함께 사용해 상태와 ViewModel 스코프를 유지합니다.
- `OrderBook`는 feature 모듈의 `OrderBookNavKey`를 `creationCallback`으로 ViewModel factory에 전달해 `SavedStateHandle` 없이 인수를 주입합니다.
- 의존성은 `navigation3-runtime`, `navigation3-ui`, `lifecycle-viewmodel-navigation3` 조합을 사용하고, `navigation-compose`는 포함하지 않았습니다.

## 주요 라이브러리

| 라이브러리 | 용도 | 선택 근거 |
|---|---|---|
| Jetpack Compose | UI | 과제 필수, Route/Screen 분리에 적합 |
| Navigation3 | 화면 이동 | serializable key 기반 back stack과 entry 단위 상태 복원에 적합 |
| Hilt | DI | 모듈 간 repository/network binding을 단순화 |
| Retrofit | REST API | Upbit REST 엔드포인트 선언이 간결함 |
| OkHttp WebSocket | 실시간 호가/현재가 | WebSocket 제어와 `callbackFlow` 래핑이 쉬움 |
| kotlinx.serialization | JSON 직렬화 | REST/WS 응답 모델에 동일하게 적용 가능 |
| Coroutines / Flow | 비동기 처리 | StateFlow, callbackFlow, 테스트 도구와 궁합이 좋음 |
| JUnit4 + Turbine + MockK | 테스트 | ViewModel/Flow 검증에 적합 |

## 가정과 판단

- 종목 리스트는 **KRW 마켓만** 표시합니다.
- 종목 리스트의 현재가와 24h 변동률은 REST `/v1/ticker`를 **5초 polling**으로 갱신합니다.
- 호가창 중앙의 현재가는 WebSocket `ticker.trade_price`를 사용합니다.
- 호가 수량은 기본 15단(`KRW-BTC.15`)으로 구독합니다.
- 호가창은 과제 요구사항에 맞춰 REST polling 없이 WebSocket만 사용합니다.
- refresh 시 `OrderBook`와 `MarketList` 모두 이전 표시 상태를 버리고 `Loading`부터 다시 시작합니다.
- Compose Preview는 `src/debug`에만 두어 release 산출물에서 제외합니다.
- ViewModel 인터페이스는 도입하지 않았습니다. 대신 fake repository와 stateless screen으로 테스트성을 확보했습니다.

## 프로젝트 구조

```
app/
core/
  model/
  domain/
  network/
  data/
feature/
  market/
  orderbook/
```

## 검증 결과

- `./gradlew testDebugUnitTest` 통과
- `./gradlew assembleDebug` 통과

## 관련 문서

- [`docs/DEVELOPMENT_PLAN.md`](./docs/DEVELOPMENT_PLAN.md) — 재구성한 개발 기획 문서
- [`AGENTS.md`](./AGENTS.md) — 이 저장소에서 AI 에이전트 작업 규칙
## 최근 조정 메모

- `OrderBookViewModel`, `MarketListViewModel`의 refresh trigger는 `MutableStateFlow<Int>` 대신 `MutableSharedFlow<Unit>`를 사용한다. 재시도는 값 비교가 아니라 이벤트 의미가 더 맞고, `StateFlow<Unit>`은 같은 값 재방출이 불가능하기 때문이다.
- `OrderBookContract.UiState.Error`는 문자열 메시지 대신 `ErrorType(NETWORK, SOCKET)`만 가진다. 실제 안내 문구와 버튼 노출 여부는 화면에서 `strings.xml`과 `type`으로 결정한다.
- `OrderBook` 진입 시 네트워크가 없으면 WebSocket 구독 전에 `NETWORK` 에러를 표시하고, 소켓 통신 실패일 때만 새로고침 버튼을 노출한다.
