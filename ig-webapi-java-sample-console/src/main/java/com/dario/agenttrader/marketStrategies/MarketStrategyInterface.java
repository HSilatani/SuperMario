package com.dario.agenttrader.marketStrategies;

public interface MarketStrategyInterface {

    public abstract void evaluate(Object marketUpdate);

    public String[] getListOfObservedMarkets();
}
