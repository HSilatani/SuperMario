package com.dario.agenttrader.marketStrategies;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MarketStrategySystem {
    public static final String MARKET_STRATEGY_MANAGER = "marketStrategyManager";
    public static final String POSITION_MANAGER = "positionManager";
    private static MarketStrategySystem oneAndOnly = new MarketStrategySystem();

    public static MarketStrategySystem getInstance(){
        return oneAndOnly;
    }

    private static Logger LOG = LoggerFactory.getLogger(MarketStrategySystem.class);

    private boolean isStrategySystemRunning = false;

    private final ActorSystem system;

    private final ActorRef marketStrategyManagerActor;

    private final ActorRef positionManagerActor;

    private MarketStrategySystem(){
        system = ActorSystem.create("MarketStrategySystem");

             marketStrategyManagerActor =
                    system.actorOf(MarketStrategyManager.props(), MARKET_STRATEGY_MANAGER);

             positionManagerActor =
                     system.actorOf(PositionManager.props(), POSITION_MANAGER);

            isStrategySystemRunning = true;
    }


    public ActorSystem getActorSystem() {
        return system;
    }

    public ActorRef getMarketStrategyManagerActor() {
        return marketStrategyManagerActor;
    }

    public ActorRef getPositionManagerActor() {
        return positionManagerActor;
    }
}
