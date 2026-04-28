package com.kwakwonjo.cryptoorderbook.feature.orderbook

/**
 * 호가창(OrderBook)에 노출할 매수/매도 호가 단계 (최대 15호가)
 */
const val OrderBookUnit = 15

/**
 * 오더북 UI 렌더링 및 데이터 스트리밍 샘플링 주기 제한
 */
const val OrderBookSample = 100L