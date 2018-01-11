package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.Arrays;

public class StrategyActor extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private final String uniqId;
    private final ActorRef ownerActor;
    private ActorRef marketActor;
    private MarketStrategy marketStrategy;

    public StrategyActor(String puniqId,ActorRef pownerActor){
        this.uniqId = puniqId;
        ownerActor = pownerActor;
    }

    public static Props props(String puniqId,ActorRef ownerActor){
        return Props.create(Position.class,puniqId,ownerActor);
    }

    @Override
    public void preStart() {
        LOG.info("Strategy {} registered", uniqId);
    }

    @Override
    public void postStop() {
        LOG.info("Strategy {} unregistered", uniqId);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StrategyManager.CreateStrategyMessage.class,this::onCreateStrategy)
                .build();
    }

    private void onCreateStrategy(StrategyManager.CreateStrategyMessage msg) {
        marketStrategy = msg.getMarketStrategy();
        requestMarketUpdateSubscription();
        msg.getOwner().tell(new StrategyActor.StrategyCreated(msg.getUniqId()),getSelf());
        getSender().tell(new StrategyActor.StrategyCreated(msg.getUniqId()),getSelf());
    }

    private void requestMarketUpdateSubscription() {
        String[] epics = marketStrategy.getListOfObservedMarkets();

        Arrays.stream(epics).forEach(epic ->
          MarketStrategySystem.getInstance().getMarketManagerActor().tell(
                new MarketManager.SubscribeToMarketUpdate(epic),getSelf())
        );

    }

    private class StrategyCreated {
        private final String uniqId;

        public StrategyCreated(String puniqId) {
            uniqId = puniqId;
        }

        public String getUniqId() {
            return uniqId;
        }
    }
}
