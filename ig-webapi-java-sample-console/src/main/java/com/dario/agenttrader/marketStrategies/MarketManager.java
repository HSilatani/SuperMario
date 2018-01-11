package com.dario.agenttrader.marketStrategies;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class MarketManager extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    public static final Props props(){
        return Props.create(PositionManager.class);
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder().build();
    }

    public static final class SubscribeToMarketUpdate {
        private final String epic;
        public SubscribeToMarketUpdate(String pepic) {
            epic = pepic;
        }

        public String getEpic() {
            return epic;
        }
    }
}
