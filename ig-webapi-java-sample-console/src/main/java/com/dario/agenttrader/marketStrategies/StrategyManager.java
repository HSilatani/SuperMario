package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.utility.ActorRegistery;


public class StrategyManager extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(), this);

    public static Props props() {
        return Props.create(StrategyManager.class);
    }

    private final ActorRegistery registry = new ActorRegistery();



    @Override
    public void preStart() {
        LOG.info("Market Strategy Manager started");
    }

    @Override
    public void postStop() {
        LOG.info("Market Strategy Manager stopped");

    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CreateStrategyMessage.class,this::onCreateStrategy)
                .match(Terminated.class,this::onTerminated)
                .build();
    }

    public void onTerminated(Terminated t) {
        ActorRef actor = t.getActor();
        String uniqId = registry.removeActor(actor);

        LOG.info("Actor for strategy {} is removed",uniqId);
    }

    public void onCreateStrategy(CreateStrategyMessage createStrategyMsg) {
        Props props = StrategyActor.props(
                createStrategyMsg.getUniqId(), createStrategyMsg.getOwner());

        registry.registerActorIfAbscent(getContext()
                ,props
                ,createStrategyMsg.getUniqId()
                ,createStrategyMsg);
    }

    public static final class CreateStrategyMessage{
        private String uniqId;
        private ActorRef owner;
        private MarketStrategyInterface marketStrategy;

        public CreateStrategyMessage(String pUniqId, MarketStrategyInterface pMarketStrategy){
            this.uniqId = pUniqId;
            this.marketStrategy = pMarketStrategy;
        }

        public CreateStrategyMessage(ActorRef pOwner,String  pUniqId, MarketStrategyInterface pMarketStrategy){
            this(pUniqId, pMarketStrategy);
            owner = pOwner;
        }

        public String getUniqId() {
            return uniqId;
        }

        public ActorRef getOwner() {
            return owner;
        }

        public MarketStrategyInterface getMarketStrategy() {
            return marketStrategy;
        }
    }
}
