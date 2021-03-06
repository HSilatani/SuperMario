package com.dario.agenttrader.marketStrategies;

import com.dario.agenttrader.actors.MarketActor;
import com.dario.agenttrader.actors.Position;
import com.dario.agenttrader.domain.TradingSignal;

import java.util.ArrayList;
import java.util.function.Consumer;

public interface MarketStrategyInterface {

    public abstract void evaluate(MarketActor.MarketUpdated marketUpdate);

    public abstract void evaluate(Position.PositionUpdate positionUpdate);

    public ArrayList<String> getListOfObservedMarkets();

    public ArrayList<String>  getListOfObservedPositions();

    public void setStrategyInstructionConsumer(Consumer<TradingSignal> pstrategyInstructionConsumer);

}
