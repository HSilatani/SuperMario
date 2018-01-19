package com.dario.agenttrader.marketStrategies;


import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import com.dario.agenttrader.tradingservices.TradingAPI;
import com.dario.agenttrader.utility.ActorRegistery;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.stop;

public class MarketManager extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private final ActorRegistery marketManagerRegistry = new ActorRegistery();

    private final TradingAPI tradingAPI;

    public MarketManager(TradingAPI pTradingAPI){
        tradingAPI = pTradingAPI;
    }

    public static final Props props(TradingAPI ptradingAPI){
        return Props.create(MarketManager.class,ptradingAPI);
    }
    @Override
    public void preStart() {
        LOG.info("MarketManager created");
    }

    @Override
    public void postStop() {
        LOG.info("MarketManager stopped");
    }

//    private static SupervisorStrategy strategy =
//            new OneForOneStrategy(1, Duration.create(1, TimeUnit.MINUTES),
//                    DeciderBuilder
//                            .match(Exception.class,
//                                    e -> e.getCause().getMessage().contains("Insufficient permissions"),e-> stop())
//                            .matchAny(o -> escalate()).build());
//
//    public SupervisorStrategy supervisorStrategy() {
//        return strategy;
//    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SubscribeToMarketUpdate.class,this::onSubscribeToMarket)
                .match(Terminated.class,this::onTerminated)
                .build();
    }

    private void onTerminated(Terminated t) {
        ActorRef terminatedActor = t.getActor();
        String marketId = marketManagerRegistry.removeActor(terminatedActor);
        LOG.info("Market actor for {} is terminated",marketId);
    }

    private void onSubscribeToMarket(SubscribeToMarketUpdate msg) {

      Props props = MarketActor.props(msg.getEpic(),tradingAPI);
      CreateMarket createMarket = new CreateMarket(msg.getEpic());
      ActorRef marketActor = marketManagerRegistry.registerActorIfAbscent(
              getContext(),props,msg.getEpic(),createMarket);

      marketActor.forward(msg,getContext());

    }

    public static final class SubscribeToMarketUpdate {
        private final String epic;
        private final ActorRef subscriber;
        public SubscribeToMarketUpdate(String pepic,ActorRef pobserver) {
            this.subscriber = pobserver;
            epic = pepic;
        }

        public ActorRef getSubscriber() {
            return subscriber;
        }

        public String getEpic()
        {
            return epic;
        }
    }

    public static final class CreateMarket{
        private final String epic;
        CreateMarket(String pepic){
            this.epic = pepic;
        }

        public String getEpic() {
            return epic;
        }
    }
}
