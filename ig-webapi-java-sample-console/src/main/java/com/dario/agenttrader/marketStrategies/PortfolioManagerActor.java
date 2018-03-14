package com.dario.agenttrader.marketStrategies;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.dario.agenttrader.tradingservices.TradingAPI;

public class PortfolioManagerActor extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private final TradingAPI tradingAPI;

    public PortfolioManagerActor(TradingAPI pTradingAPI){
        tradingAPI = pTradingAPI;
    }

    public static final  Props props(TradingAPI ptradingAPI){
        return Props.create(PortfolioManagerActor.class,ptradingAPI);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StrategyActor.TradingSignal.class,this::onTradingSignal)
                .build();
    }

    private void onTradingSignal(StrategyActor.TradingSignal signal) throws Exception{
        tradingAPI.editPosition(signal.getDealId()
                ,signal.getNewStop()
                ,signal.getNewLimit());
    }
}
