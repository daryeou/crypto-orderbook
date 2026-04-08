# Crypto Order Book (Android)

Upbit 공개 API를 이용해 종목 리스트와 실시간 호가창을 구현하는 Android 과제용 앱입니다.

## 현재 기준

- 거래소: Upbit
- UI: Jetpack Compose
- 아키텍처 방향: Clean Architecture + MVVM + StateFlow
- 모듈 방향: `app`, `core:model`, `core:domain`, `core:network`, `core:data`, `feature:*`

## 거래소 선택 근거

후보를 간단히 비교했을 때 Upbit가 이번 과제에 가장 맞았습니다.

- 한국 환경에서 바로 검증하기 쉽습니다.
- REST와 WebSocket 문서가 분리되어 있어 역할을 나누기 쉽습니다.
- `market/all`, `ticker`, `orderbook`, `ticker` 조합으로 요구사항을 단순하게 충족할 수 있습니다.

## 아키텍처 초안

```text
app
  └─ feature:market / feature:orderbook
       └─ core:domain
            └─ core:data
                 ├─ core:network
                 └─ core:model
```

- UI는 `Route`와 stateless `Screen`으로 분리합니다.
- ViewModel은 concrete class로 두고, domain use case를 주입받는 방향을 기본값으로 잡습니다.
- WebSocket은 `callbackFlow`로 감싸고, 화면 생명주기와 함께 시작/중단합니다.

## 가정과 판단

- 종목 리스트는 KRW 마켓 위주로 보여줍니다.
- 종목 리스트는 REST 기반으로 조회합니다.
- 호가창은 WebSocket 기반으로 갱신합니다.
- Compose Preview는 `src/debug`에 둡니다.

## 빌드와 검증

초기 기준 명령은 다음을 사용합니다.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

세부 실행 절차와 검증 결과는 구현이 진행되면 계속 보강합니다.

## 관련 문서

- [`ROADMAP.md`](./ROADMAP.md)
- [`RETROSPECTIVE.md`](./RETROSPECTIVE.md)
- [`AGENTS.md`](./AGENTS.md)
