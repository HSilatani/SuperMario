package com.dario.agenttrader.marketStrategies;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MarketStrategySystem {
    private static MarketStrategySystem oneAndOnly = new MarketStrategySystem();

    public static MarketStrategySystem getInstance(){
        return oneAndOnly;
    }

    private static Logger LOG = LoggerFactory.getLogger(MarketStrategySystem.class);

    private boolean isStrategySystemRunning = false;

    private final ActorSystem system;

    private final ActorRef marketStrategyManagerActor;

    private MarketStrategySystem(){
        system = ActorSystem.create("MarketStrategySystem");

             marketStrategyManagerActor =
                    system.actorOf(MarketStrategyManager.props(), "marketStrategyManager");

            isStrategySystemRunning = true;
    }


    public ActorSystem getActorSystem() {
        return system;
    }

    public ActorRef getMarketStrategyManagerActor() {
        return marketStrategyManagerActor;
    }
}
