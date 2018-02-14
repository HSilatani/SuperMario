package com.dario.agenttrader.dto;

public interface Price<T> {
     public void mergeWithSnapshot(T lastPrice);
}
