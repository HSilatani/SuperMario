package com.dario.agenttrader.marketStrategies;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import com.dario.agenttrader.tradingservices.IGClient;
import com.dario.agenttrader.tradingservices.TradingAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;


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

    private ActorSystem system;

    private ActorRef strategyManagerActor;

    private ActorRef positionManagerActor;

    private ActorRef marketManagerActor;

    private TradingAPI tradingAPI;

    private String actorSystemName;

    private MarketStrategySystem(){

    }
    public void startMarketStrategySystem(TradingAPI ptradingAPI){
        startMarketStrategySystem(ptradingAPI,"MarketStrategySystem");
    }

    public void startMarketStrategySystem(TradingAPI ptradingAPI,String strActorSystemName){

        system = ActorSystem.create(strActorSystemName);
        tradingAPI = ptradingAPI;

        strategyManagerActor =
                    system.actorOf(StrategyManager.props(tradingAPI), MARKET_STRATEGY_MANAGER);

        positionManagerActor =
                     system.actorOf(PositionManager.props(tradingAPI), POSITION_MANAGER);


        marketManagerActor = system.actorOf(MarketManager.props(tradingAPI),MARKET_MANAGER);

        triggerDefaultStrategies();

        isStrategySystemRunning = true;
    }

    private void triggerDefaultStrategies() {
            ArrayList<String> epics = new ArrayList<>();
            epics.add("IX.D.HANGSENG.DAILY.IP");//TODO: load from properties files
            MarketStrategyInterface reEntryStrategy = new ReEntryStrategy(epics,Direction.BUY());
            String uniqStrategyID= epics.get(0) + "-Reentry" ;
            getStrategyManagerActor().tell(
                    new StrategyManager.CreateStrategyMessage(
                            getStrategyManagerActor(),
                            uniqStrategyID,
                            reEntryStrategy),
                            getStrategyManagerActor()
                            );
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
