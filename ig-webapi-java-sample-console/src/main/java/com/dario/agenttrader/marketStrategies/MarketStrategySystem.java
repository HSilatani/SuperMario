package com.dario.agenttrader.marketStrategies;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import com.dario.agenttrader.tradingservices.IGClient;
import com.dario.agenttrader.tradingservices.TradingAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MarketStrategySystem {
    public static final String MARKET_STRATEGY_MANAGER = "marketStrategyManager";
    public static final String POSITION_MANAGER = "positionManager";
    public static final String MARKET_MANAGER = "marketManager";
    private static MarketStrategySystem oneAndOnly = new MarketStrategySystem();

    public static MarketStrategySystem getInstance(){
        return oneAndOnly;
    }

    private static Logger LOG = LoggerFactory.getLogger(MarketStrategySystem.class);

    private boolean isStrategySystemRunning = false;

    private final ActorSystem system;

    private ActorRef strategyManagerActor;

    private ActorRef positionManagerActor;

    private ActorRef marketManagerActor;

    private TradingAPI tradingAPI;

    private MarketStrategySystem(){
        system = ActorSystem.create("MarketStrategySystem");

    }

    public void startMarketStrategySystem(TradingAPI ptradingAPI){
        tradingAPI = ptradingAPI;

             strategyManagerActor =
                    system.actorOf(StrategyManager.props(tradingAPI), MARKET_STRATEGY_MANAGER);

             positionManagerActor =
                     system.actorOf(PositionManager.props(tradingAPI), POSITION_MANAGER);

             marketManagerActor = system.actorOf(MarketManager.props(tradingAPI),MARKET_MANAGER);

            isStrategySystemRunning = true;
    }


    public ActorSystem getActorSystem() {
        return system;
    }

    public ActorRef getStrategyManagerActor() {
        return strategyManagerActor;
    }

    public ActorRef getPositionManagerActor(){
        return positionManagerActor;
    }

    public ActorRef getMarketManagerActor() {
        return marketManagerActor;
    }
}
