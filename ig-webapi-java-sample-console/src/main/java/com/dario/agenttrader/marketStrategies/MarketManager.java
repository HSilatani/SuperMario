package com.dario.agenttrader.marketStrategies;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.tradingservices.IGClient;
import com.dario.agenttrader.utility.ActorRegistery;

public class MarketManager extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private final ActorRegistery marketManagerRegistry = new ActorRegistery();

    public static final Props props(){
        return Props.create(MarketManager.class);
    }
    @Override
    public void preStart() {
        LOG.info("MarketManager created");
    }

    @Override
    public void postStop() {
        LOG.info("MarketManager stopped");
    }


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

      IGClient igClient =   IGClient.getInstance();
      Props props = MarketActor.props(msg.getEpic(),igClient);
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
