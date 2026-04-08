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
- 종목 리스트: `MarketListUiState` sealed interface
- 호가창: `OrderBookUiState` data class + `ConnectionState`
- 각 화면은 `Route(hiltViewModel())`와 `Screen(uiState, onAction...)`로 분리
- `MarketListViewModel`은 domain use case를 통해 REST polling을 수행하고, `OrderBookViewModel`은 WebSocket 구독을 수행합니다.

### WebSocket 처리 전략
- `core:network`에서 OkHttp WebSocket을 `callbackFlow`로 감쌌습니다.
- `core:data`에서 `orderbook`과 `ticker` frame을 병합해 단일 `OrderBookPayload`로 변환합니다.
- 화면 이탈 시 `OrderBookViewModel.stop()`으로 구독을 해제합니다.
- 백프레셔는 ViewModel 수집 지점에서 `conflate()`를 적용했습니다.

### Navigation 전략
- 앱 네비게이션은 `Navigation3`의 `rememberNavBackStack`과 `NavDisplay`를 사용합니다.
- destination key는 `@Serializable` + `NavKey`로 구성해 구성 변경 시 back stack을 복원합니다.
- `rememberSaveableStateHolderNavEntryDecorator()`와 `rememberViewModelStoreNavEntryDecorator()`를 함께 사용해 상태와 ViewModel 스코프를 유지합니다.
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

- [`ROADMAP.md`](./ROADMAP.md) — 미구현 항목과 테스트 전략
- [`RETROSPECTIVE.md`](./RETROSPECTIVE.md) — AI 도구 활용 회고
- [`AGENTS.md`](./AGENTS.md) — 이 저장소에서 AI 에이전트 작업 규칙
