package com.dario.agenttrader.marketStrategies;


import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.tradingservices.TradingAPI;



public class PortfolioManagerActor extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private final PortfolioManager portfolioManager;


    public PortfolioManagerActor(TradingAPI pTradingAPI){
        portfolioManager = new PortfolioManager(pTradingAPI);
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


    private void onTradingSignal(StrategyActor.TradingSignal signal) {
        portfolioManager.processTradingSignal(signal);
    }


}
