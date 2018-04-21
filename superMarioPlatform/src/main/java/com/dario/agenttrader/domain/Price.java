package com.dario.agenttrader.domain;

public interface Price<T> {
     public void mergeWithSnapshot(T lastPrice);
}
