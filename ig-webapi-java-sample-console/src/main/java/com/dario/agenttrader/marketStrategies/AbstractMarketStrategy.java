package com.dario.agenttrader.marketStrategies;

import akka.actor.ActorRef;

import java.util.ArrayList;

public abstract class AbstractMarketStrategy implements MarketStrategyInterface{

    private final ArrayList<String> observedMarkets;
    private ActorRef ownerStrategyActor;

    AbstractMarketStrategy(ArrayList<String> epics){
        observedMarkets=epics;
    }

    public void setOwnerStrategyActor(ActorRef actorRef){
        ownerStrategyActor = actorRef;
    }

    public ActorRef getOwnerStrategyActor() {
        return ownerStrategyActor;
    }

    @Override
    public ArrayList<String> getListOfObservedMarkets(){
        return observedMarkets;
    }


}
