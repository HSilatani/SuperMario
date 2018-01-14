package com.dario.agenttrader.marketStrategies;

public interface MarketStrategyInterface {

    public abstract void evaluate(MarketActor.MarketUpdated marketUpdate);

    public String[] getListOfObservedMarkets();
}
