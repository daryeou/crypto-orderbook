# 바이브 코딩 회고

이 문서는 작업과 병행해 업데이트합니다.  
핵심은 “AI를 썼다”가 아니라 “AI 초안을 어떻게 검증하고, 어디를 수정하거나 거부했는가”를 남기는 것입니다.

---

## 사용 도구

- Codex (GPT-5 계열) — 설계 정리, 멀티모듈 스캐폴딩, 구현, 테스트 수정
- PowerShell / Gradle — 빌드, 테스트, 환경 검증
- Upbit 공식 API 문서 — REST / WebSocket 역할 분리 확인

---

## 작업 전략

### 문제 분해 방식
- 먼저 거래소/API 조합을 고정했다.
- 종목 리스트는 REST, 호가창은 WebSocket으로 역할을 분리했다.
- 실시간 호가창의 핵심을 `callbackFlow -> repository 병합 -> use case -> ViewModel UiState` 파이프라인으로 잡았다.
- 구현이 진행된 뒤에는 오프라인 상태를 화면 에러가 아니라 앱 전역 상태로 재정의해 UX를 정리했다.

### AI 활용 영역 vs 직접 판단 영역

| 영역 | 담당 | 이유 |
|---|---|---|
| 거래소/API 비교 | AI + 사람 검토 | 후보를 빠르게 좁히고 과제 관점 기준을 정리하기 쉬웠다 |
| 아키텍처 결정 | 사람 주도, AI 보조 | 과제 규모에 맞는 최소 구조 판단이 중요했다 |
| 보일러플레이트 코드 | AI | Gradle / Hilt / Compose 기본 골격을 빠르게 세울 수 있었다 |
| WebSocket 래핑 | AI 초안, 사람 검증 | `callbackFlow`와 payload 병합 구조를 빨리 확보할 수 있었다 |
| 상태 관리 / 오프라인 정책 | 사람 주도 | 화면 에러와 전역 상태를 어디서 나눌지 최종 판단이 필요했다 |
| 테스트 코드 | AI 초안, 사람 수정 | 실제 payload 의미와 collect 타이밍은 사람이 끝까지 맞춰야 했다 |

### 전체 작업 흐름
1. 기존 MVVM 기반 토이 프로젝트에서 재사용 가능한 구조 조사
2. 최소 멀티모듈 구조 확정
3. Upbit REST / WebSocket 클라이언트 구현
4. Market / OrderBook feature 구현
5. domain use case와 Navigation3 구조 정리
6. ViewModel / repository 테스트 작성
7. 오프라인 오버레이와 connectivity flow 도입
8. README / ROADMAP / RETROSPECTIVE 문서 보강

---

## 구체적 AI 상호작용 사례

### 사례 1: 기존 토이 프로젝트 구조를 얼마나 가져올지 결정
- **맥락**: 프로젝트 초기 구조를 정하는 단계
- **프롬프트**: 이전에 작업했던 MVVM 기반 토이 프로젝트 구조를 복사해서 멀티모듈로 갈지 질문
- **AI 출력**: `app + core:model + core:network + core:data + feature:market + feature:orderbook` 수준의 최소 모듈 구성을 제안
- **판단**: 수정 후 수락
- **이유**: 기존 프로젝트 전체 구조를 그대로 가져오면 과제 규모에 비해 과설계로 보일 수 있었다
- **수정 내용**: wrapper / 버전 체계만 참고하고 실제 기능 모듈은 새로 작성했다
- **검증**: README의 모듈 설명이 단순해졌고, 이후 Hilt / Navigation3 도입 시 의존 방향 설명이 쉬워졌다

### 사례 2: 호가창 현재가 데이터 소스 선택
- **맥락**: Upbit API 설계를 정리하던 중
- **프롬프트**: `/v1/market/all`, `wss://api.upbit.com/websocket/v1` 조합으로 구현 구조를 질문
- **AI 출력**: 종목 리스트는 `/v1/market/all` + `/v1/ticker`, 호가창 중앙 가격은 WebSocket `ticker.trade_price`를 같이 구독해야 한다고 제안
- **판단**: 수락
- **이유**: `orderbook` frame만으로는 현재가를 만족할 수 없었다
- **수정 내용**: repository에서 `orderbook`과 `ticker`를 병합하는 `OrderBookPayload` 구조로 구현했다
- **검증**: `OrderBookRepositoryImplTest`와 `OrderBookViewModelTest`에서 orderbook / ticker 누적 성공 상태를 검증했다

### 사례 3: 백프레셔 처리 위치 수정
- **맥락**: `OrderBookRepositoryImplTest`, `OrderBookViewModelTest`가 실패하던 단계
- **프롬프트**: 빠른 연속 frame에서 `conflate()`를 어디에 두는 게 맞는지 검토
- **AI 출력**: 처음에는 repository에 `conflate()`를 두는 방향을 제안
- **판단**: 수정
- **이유**: repository 단계에서 conflation을 하면 중간 상태 추적과 테스트 디버깅이 어려워졌다
- **수정 내용**: repository는 순수 병합 결과를 유지하고, ViewModel 수집 지점에서만 `conflate()`를 적용했다
- **검증**: 관련 테스트가 다시 통과했고, 중간 이벤트가 예상대로 보이는지 확인했다

### 사례 4: domain 모듈과 use case 도입
- **맥락**: 초기 구현 후 Clean Architecture 요구를 맞추는 단계
- **프롬프트**: UseCase, Repository interface가 들어간 domain 모듈 추가 요청
- **AI 출력**: `core:domain`에 repository contract와 use case를 두고, `core:data`는 구현과 Hilt binding만 담당하도록 재구성
- **판단**: 수락
- **이유**: 의존 방향 설명이 더 명확해지고 ViewModel 테스트도 단순화됐다
- **수정 내용**: feature는 data 의존을 제거하고 domain use case만 주입받도록 변경했다
- **검증**: ViewModel 테스트 더블이 repository 구현 대신 contract 기준으로 단순화됐다

### 사례 5: Navigation3 인수 전달 패턴 재정렬
- **맥락**: `OrderBookRoute`가 `start()/stop()`을 직접 호출하던 구조를 정리하는 단계
- **프롬프트**: 공식 Navigation3 passing arguments recipe를 따르되 `SavedStateHandle` 대신 어떤 방식이 맞는지 검토
- **AI 출력**: `creationCallback + AssistedInject`로 nav key를 ViewModel 생성자에 직접 주입하는 방향 제안
- **판단**: 수정 후 수락
- **이유**: app 모듈의 destination 타입을 그대로 주입하면 `feature -> app` 의존이 생겼다
- **수정 내용**: `OrderBookNavKey`를 feature 모듈로 내리고, app은 key만 back stack에 넣도록 변경했다
- **검증**: `CryptoOrderBookNavHost`에서 entry-scoped ViewModel 생성과 화면 이동이 정상 동작하는지 빌드로 확인했다

### 사례 6: refresh trigger를 상태값 대신 이벤트로 변경
- **맥락**: `retry()` 구조를 정리하던 단계
- **프롬프트**: `MutableStateFlow<Int>` 대신 `MutableSharedFlow<Unit>`을 쓰는 게 나은지 검토
- **AI 출력**: 초기에는 숫자 trigger를 유지하는 방향이 있었다
- **판단**: 수정
- **이유**: retry는 상태값이 아니라 이벤트이며, `StateFlow<Unit>`은 같은 값 재방출이 불가능했다
- **수정 내용**: `SharedFlow<Unit> + onStart { emit(Unit) }` 패턴으로 통일했다
- **검증**: `MarketListViewModelTest`, `OrderBookViewModelTest`에서 retry 후 재구독 시나리오를 검증했다

### 사례 7: 오프라인을 화면 에러가 아니라 앱 전역 상태로 재정의
- **맥락**: `NETWORK` 에러 화면과 루트 오버레이 정책이 충돌하던 단계
- **프롬프트**: `MainActivity`에서는 스낵바와 오버레이를 띄우고, ViewModel에서는 작업 실패만 화면 에러로 남기는 구조 제안
- **AI 출력**: 오프라인은 앱 전역 상태로 분리하고, 화면 에러는 온라인 상태의 REST / WebSocket 실패로 한정하는 방향 제안
- **판단**: 수락
- **이유**: 네트워크 단절을 각 화면 에러로 처리하면 UX가 중복되고 마지막 성공 상태 유지가 어려웠다
- **수정 내용**: `NetworkAvailability`, `observeConnectivity()`, `CryptoOrderBookApp`를 도입하고 루트 오버레이와 화면별 온라인 실패 처리를 분리했다
- **검증**: `NetworkStatusRepositoryImplTest`, 각 ViewModel 테스트, `testDebugUnitTest`, `assembleDebug`로 확인했다

### 사례 8: 정밀도 타입 검토 후 Double 유지
- **맥락**: 숫자 모델과 표시 정책을 정리하는 단계
- **프롬프트**: 기능 확장성을 위해 `Double` 대신 BigDecimal 계열 도입 여부를 검토
- **AI 출력**: 고정밀 수치 타입으로 바꾸는 방향도 가능하다고 제안
- **판단**: 보류
- **이유**: 현재 공개 API가 `Double` 기반이고, 이 과제 범위에서는 화면 표시 수준의 정밀도만 요구된다
- **수정 내용**: 현재는 `Double`을 유지하고, 다중 거래소 지원이나 더 높은 정밀도 요구가 생기면 재검토하기로 했다
- **검증**: 현재 UI와 테스트 범위에서 정밀도 문제 없이 요구사항을 만족하는지 확인했다

### 사례 9: 문서와 실제 구현의 불일치 정리
- **맥락**: README, DEVELOPMENT_PLAN, ROADMAP, RETROSPECTIVE가 현재 구조와 일부 어긋나던 단계
- **프롬프트**: 현재 코드 기준으로 문서 역할과 내용을 다시 정리해 달라는 요청
- **AI 출력**: 문서별 역할을 분리하고, 구현과 검증 결과를 기준으로 문서를 다시 쓰는 방향 제안
- **판단**: 수락
- **이유**: 구현 과정 문서화가 평가 항목에 포함되므로 코드와 문서가 어긋나면 설득력이 떨어진다
- **수정 내용**: README / DEVELOPMENT_PLAN / ROADMAP / RETROSPECTIVE를 현재 구조 기준으로 재정렬했다
- **검증**: README만 읽어도 아키텍처와 오프라인 정책이 이해되도록 다시 정리했다

### 사례 10: ConnectivityManager에서 실제 연결 여부를 어떤 기준으로 볼지 정리
- **맥락**: 전역 오프라인 오버레이와 connectivity flow를 도입하던 단계
- **프롬프트**: `ConnectivityManager`에서 네트워크 연결 유무를 어떤 속성으로 판단해야 하는지 검토
- **AI 출력**: 처음에는 `activeNetwork` 존재 여부만으로도 확인할 수 있다는 방향이 섞여 있었다
- **판단**: 수정
- **이유**: `activeNetwork`만 있으면 와이파이에 붙어 있지만 실제 인터넷이 안 되는 상태를 걸러내지 못한다
- **수정 내용**: `activeNetwork`로 현재 네트워크를 찾은 뒤 `NetworkCapabilities`의 `NET_CAPABILITY_INTERNET`과 `NET_CAPABILITY_VALIDATED`를 함께 확인하도록 정리했다
- **검증**: `NetworkStatusRepositoryImplTest`에서 초기 상태와 capability 전이를 검증했고, 실제 구현도 같은 기준으로 통일했다

### 사례 11: OrderBookViewModel 상태 구조 단순화
- **맥락**: `OrderBookViewModel`의 `uiState` 조립 로직이 누적 상태와 네트워크 상태를 함께 다루면서 읽기 어려워지던 단계
- **프롬프트**: `meta + content + uiStatus` 구조로 정리하고, `uiState`는 `orderBookFlow`와 `NetworkAvailability`만 결합하는 단순한 형태로 만들고 싶다는 요청
- **AI 출력**: 중간 `socketAwareUiState`나 `scan` 기반 누적 상태를 `uiState` 쪽에 두는 방향도 제안했다
- **판단**: 수정
- **이유**: 최종 `uiState` 조립부는 단순해야 하고, 이전 값 유지가 필요하더라도 그 책임은 raw payload 흐름 한 곳에만 두는 편이 읽기 쉬웠다
- **수정 내용**: `OrderBookPayload` 누적은 `orderBookFlow.scan { previous.merge(payload) }`로 한정하고, `uiState`는 `combine(orderBookFlow, networkAvailability)`에서 `meta + content + uiStatus`만 조립하도록 정리했다
- **검증**: `OrderBookContract`, `OrderBookViewModel`, `OrderBookScreen`, preview, 테스트가 같은 상태 구조를 보도록 맞췄고, 이후 `MarketListViewModel`도 같은 관점에서 단순화할 후보로 남겼다

### 사례 12: MarketList polling 책임과 다중 마켓 탭 구조 정리
- **맥락**: `MarketListViewModel`을 손보면서 KRW 전용 리스트를 KRW/BTC/USDT 탭 구조로 확장하던 단계
- **프롬프트**: polling 책임을 repository보다 use case에 두고, `MarketSummary`에 마켓 정보를 넣어 `TabRow` 기반 전환이 가능하도록 정리하고 싶다는 요청
- **AI 출력**: `MarketRepository`는 단건 fetch만 맡기고, `ObserveMarketSummariesUseCase`에서 `while + delay` polling을 수행하는 방향을 제안했고, 초기에는 `selectedMarketType` 상태를 ViewModel에서 관리하는 구조도 함께 제안했다
- **판단**: 수정 후 수락
- **이유**: polling 주기는 데이터 소스 구현보다 도메인 정책에 가까웠고, repository를 다른 곳에서 재사용할 때도 단건 fetch 계약이 더 단순했다. 또한 탭 전환은 ViewModel 상태로 관리하는 편이 Compose 계층보다 테스트하기 쉬웠다
- **수정 내용**: `MarketRepository`를 `fetchMarketSummaries()` 계약으로 단순화하고, `ObserveMarketSummariesUseCase`가 polling을 담당하도록 변경했다. `MarketSummary`에는 `MarketType`을 추가하고, `MarketListViewModel`은 전체 `items + uiStatus` 상태만 노출하며 `MarketListScreen`이 `TabRow + HorizontalPager`로 시장 전환을 관리하도록 정리했다
- **검증**: `MarketListViewModelTest`에 retry, 오프라인 복구, 전체 마켓 유지 시나리오를 반영했고 `testDebugUnitTest`, `assembleDebug`를 다시 통과시켰다

### 사례 13: BTC/USDT 소수점 가격 포맷 대응
- **맥락**: 다중 마켓 지원 후 BTC 마켓 가격이 목록과 호가창에서 충분한 소수 자릿수로 보이지 않던 단계
- **프롬프트**: BTC 마켓의 경우 가격이 소수점 9자리까지 내려갈 수 있으니 목록과 호가창 모두 대응이 필요하다는 요청
- **AI 출력**: KRW는 정수 그룹 포맷을 유지하고, BTC/USDT는 별도 소수 포맷으로 분기하는 방향을 정리
- **판단**: 수락
- **이유**: KRW 기준 포맷을 그대로 쓰면 BTC/USDT 가격 정보가 과도하게 절삭돼 실제 시세 판단에 필요한 자릿수가 사라진다
- **수정 내용**: `MarketListScreen`과 `OrderBookScreen`에서 market type 기준으로 가격 포맷을 분기하고, BTC/USDT는 최대 소수점 9자리까지 표시하도록 조정했다
- **검증**: `testDebugUnitTest`, `assembleDebug`를 다시 실행해 멀티 마켓 확장 이후에도 빌드와 테스트가 유지되는지 확인했다

### 사례 14: 4월 10일 아침, MarketList 구조 리팩터링 시작
- **맥락**: 다중 마켓 지원 이후 `MarketList` 구조를 다시 점검하던 단계
- **프롬프트**: 종목 리스트 화면 구조를 보고 불필요하게 ViewModel에서 `BaseState`와 `UiState`가 나뉘어 있고, repository 안에서 `fetchMarketSummaries()`로 모든 티커를 조회하고 있음을 확인했으니 분리 개선 작업을 진행하겠다는 판단 정리
- **AI 출력**: polling 책임을 use case로 올리고, 탭 상태는 ViewModel보다 화면 계층에서 관리하는 쪽이 단순하다는 방향을 제안
- **판단**: 수정 후 수락
- **이유**: 기존 구조는 기능은 동작했지만 상태 계층이 중복되고, 화면 전환 상태까지 ViewModel이 들고 있어 읽기 어려웠다. `MarketList`는 전체 목록과 화면 전환 상태를 분리하는 편이 이후 정렬/필터링 확장에도 유리하다고 봤다
- **수정 내용**: `MarketRepository`는 단건 fetch만 담당하고 `ObserveMarketSummariesUseCase`가 polling을 관리하도록 유지했다. 이후 `MarketListViewModel`은 전체 목록 상태만 노출하고, `MarketListScreen`이 `TabRow + HorizontalPager`로 시장 전환을 맡도록 구조를 단순화하는 작업을 진행했다
- **검증**: 리팩터링 중간마다 `MarketListViewModelTest`, `assembleDebug`를 다시 확인하면서 상태 단순화 이후에도 빌드와 테스트가 유지되는지 점검했다

---

## AI가 만든 실수

- repository 단계에 `conflate()`를 둔 초안 때문에 중간 상태가 사라졌다.
  - **감지 방법**: `OrderBookRepositoryImplTest`, `OrderBookViewModelTest` 실패
  - **대응**: `conflate()`를 ViewModel 수집 지점으로 이동

- 네트워크 단절을 `OrderBook` 화면의 `NETWORK` 에러로 유지하는 방향이 한동안 남아 있었다.
  - **감지 방법**: 루트 오프라인 오버레이를 추가하려는 시점에 화면 에러 정책과 중복되는 것을 발견
  - **대응**: 오프라인은 앱 전역 상태로 분리하고, `OrderBook` 화면 에러는 `SOCKET`만 남김

- 제출 전략상 “히스토리에서만 제외”하려던 문서가 워킹트리에서도 빠진 상태가 됐다.
  - **감지 방법**: 파일 목록을 직접 점검하던 중 `ROADMAP.md`, `RETROSPECTIVE.md`, `docs/사전과제.md`가 누락된 것을 확인
  - **대응**: 백업 브랜치 기준으로 복구하고, 현재 코드 기준으로 내용도 다시 맞춤

---

## 아키텍처 결정

### 직접 내린 판단
- Clean Architecture + MVVM + StateFlow 유지
- ViewModel 인터페이스는 도입하지 않음
- `Route`와 `Screen`을 분리해 preview / test seam 확보
- Compose Preview는 `src/debug`에만 둠
- Navigation3 + Hilt assisted injection 사용
- 오프라인은 앱 전역 상태로 처리하고, 화면 에러는 온라인 상태의 작업 실패로 제한

### AI 제안을 거부하거나 수정한 경우
- 기존 프로젝트 수준의 넓은 멀티모듈 구성을 그대로 복사하는 방향은 거부
- `conflate()`를 repository에 두는 방향은 수정
- `NETWORK` 화면 에러를 유지하는 방향은 수정
- BigDecimal 계열 도입은 현재 범위에서는 보류

---

## 솔직한 평가

### AI가 효과적이었던 부분
- 초기 멀티모듈 / Hilt / Compose 골격을 빠르게 세우는 데 효과적이었다.
- REST / WebSocket 역할 분리와 테스트 초안 작성에도 속도 이점이 컸다.

### AI의 한계를 느낀 부분
- 동시성, retry, 오프라인 UX 같은 경계 조건은 그대로 수용하면 설계 충돌이 생겼다.
- 테스트 더블이 실제 production 의미와 맞는지까지는 사람이 직접 검토해야 했다.

### 다음에 다르게 할 점
- 오프라인 정책과 화면 에러 정책을 더 초기에 분리해서 잡겠다.
- WebSocket 이벤트 흐름은 더 작은 최소 동작 구현과 테스트로 먼저 고정하겠다.
- 제출 전략에서 “히스토리 제외”와 “워킹트리 제외”를 더 명확히 구분해 관리하겠다.
