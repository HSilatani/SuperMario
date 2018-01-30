package com.dario.agenttrader.marketStrategies;

import java.util.ArrayList;
import java.util.function.Consumer;

public abstract class AbstractMarketStrategy implements MarketStrategyInterface{

    private final ArrayList<String> observedMarkets;
    protected Consumer<StrategyActor.ActOnStrategyInstruction> strategyInstructionConsumer;

    AbstractMarketStrategy(ArrayList<String> epics){
        observedMarkets=epics;
    }

    public void setStrategyInstructionConsumer(Consumer<StrategyActor.ActOnStrategyInstruction> pstrategyInstructionConsumer){
        strategyInstructionConsumer = pstrategyInstructionConsumer;
    }


    @Override
    public ArrayList<String> getListOfObservedMarkets(){
        return observedMarkets;
    }


}