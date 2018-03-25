package com.dario.agenttrader.strategies;

import com.dario.agenttrader.marketStrategies.MarketStrategyInterface;
import com.dario.agenttrader.marketStrategies.StrategyActor;

import java.util.ArrayList;
import java.util.function.Consumer;

public abstract class AbstractMarketStrategy implements MarketStrategyInterface {

    private final ArrayList<String> observedMarkets;
    protected Consumer<StrategyActor.TradingSignal> strategyInstructionConsumer;

    AbstractMarketStrategy(ArrayList<String> epics){
        observedMarkets=epics;
    }

    public void setStrategyInstructionConsumer(Consumer<StrategyActor.TradingSignal> pstrategyInstructionConsumer){
        strategyInstructionConsumer = pstrategyInstructionConsumer;
    }


    @Override
    public ArrayList<String> getListOfObservedMarkets(){
        return observedMarkets;
    }


}
