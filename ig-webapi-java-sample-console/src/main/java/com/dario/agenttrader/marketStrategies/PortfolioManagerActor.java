package com.dario.agenttrader.marketStrategies;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.dto.PositionSnapshot;
import com.dario.agenttrader.tradingservices.TradingAPI;

import java.util.List;


public class PortfolioManagerActor extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private final PortfolioManager portfolioManager;

    private ActorRef positionManager;


    public PortfolioManagerActor(TradingAPI pTradingAPI,ActorRef ppositionManager){
        portfolioManager = new PortfolioManager(pTradingAPI);
        positionManager = ppositionManager;
    }

    public static final  Props props(TradingAPI ptradingAPI,ActorRef ppositionManager){
        return Props.create(PortfolioManagerActor.class,ptradingAPI,ppositionManager);
    }
    @Override
    public void preStart() {
        positionManager.tell(new PositionManager.SubscribeToRegisterUnRegisterPosition(),getSelf());
        portfolioManager.initialize();
        LOG.info("PorfolioManagerActor started");
    }
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StrategyActor.TradingSignal.class,this::onTradingSignal)
                .match(List.class,this::onPositionAddRemove)
                .build();
    }

    private void onPositionAddRemove(List positionSnapShots){
        List<PositionSnapshot> psnapsots = positionSnapShots;
        portfolioManager.updatePositionList(psnapsots);
    }


    private void onTradingSignal(StrategyActor.TradingSignal signal) {
        portfolioManager.processTradingSignal(signal);
    }


}
