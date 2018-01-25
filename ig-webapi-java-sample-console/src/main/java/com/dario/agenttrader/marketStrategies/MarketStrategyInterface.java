package com.dario.agenttrader.marketStrategies;

import akka.actor.ActorRef;

import java.util.ArrayList;
import java.util.function.Consumer;

public interface MarketStrategyInterface {

    public abstract void evaluate(MarketActor.MarketUpdated marketUpdate);

    public abstract void evaluate(Position.PositionUpdate positionUpdate);

    public ArrayList<String> getListOfObservedMarkets();

    public ArrayList<String>  getListOfObservedPositions();

    public void setStrategyInstructionConsumer(Consumer<StrategyActor.ActOnStrategyInstruction> pstrategyInstructionConsumer);

}
