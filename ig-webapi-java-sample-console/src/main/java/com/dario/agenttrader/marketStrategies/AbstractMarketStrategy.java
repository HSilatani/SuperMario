package com.dario.agenttrader.marketStrategies;

public abstract class AbstractMarketStrategy implements MarketStrategyInterface{

    private final String[] observedMarkets;

    AbstractMarketStrategy(String[] epics){
        observedMarkets=epics;

    }

    @Override
    public String[] getListOfObservedMarkets(){
        return observedMarkets;
    }


}
