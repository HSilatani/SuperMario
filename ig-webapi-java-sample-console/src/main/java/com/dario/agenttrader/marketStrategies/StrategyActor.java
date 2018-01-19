package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.ArrayList;
import java.util.Arrays;

public class StrategyActor extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private final String uniqId;
    private final ActorRef ownerActor;
    private MarketStrategyInterface marketStrategy;

    public StrategyActor(String puniqId,ActorRef pownerActor, MarketStrategyInterface pmarketStrategy){
        this.uniqId = puniqId;
        ownerActor = pownerActor;
        marketStrategy = pmarketStrategy;
    }

    public static Props props(String puniqId,ActorRef ownerActor,MarketStrategyInterface pmarketStrategy){
        return Props.create(StrategyActor.class,puniqId,ownerActor,pmarketStrategy);
    }

    @Override
    public void preStart()
    {
        getContext().watch(ownerActor);
        marketStrategy.setOwnerStrategyActor(getSelf());
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
                .match(MarketActor.MarketUpdated.class,this::onMarketUpdate)
                .match(Position.PositionUpdate.class,this::onPositionUpdate)
                .match(ActOnStrategyInstruction.class,this::onActOnStrategyInstruction)
                .match(Terminated.class,this::onTerminated)
                .build();
    }

    private void onTerminated(Terminated t) {
        ActorRef actor = t.getActor();
        getContext().stop(getSelf());
    }

    private void onPositionUpdate(Position.PositionUpdate positionUpdate){
        if(positionUpdate!=null
                && marketStrategy.getListOfObservedPositions().contains(positionUpdate.getPositionId())){

            marketStrategy.evaluate(positionUpdate);
        }
    }

    private void onMarketUpdate(MarketActor.MarketUpdated marketUpdated) {
        marketStrategy.evaluate(marketUpdated);
    }

    private void onCreateStrategy(StrategyManager.CreateStrategyMessage msg) {

        requestMarketUpdateSubscription();
        msg.getOwner().tell(new StrategyActor.StrategyCreated(msg.getUniqId()),getSelf());
        getSender().tell(new StrategyActor.StrategyCreated(msg.getUniqId()),getSelf());
    }

    private void requestMarketUpdateSubscription() {
        ArrayList<String> epics = marketStrategy.getListOfObservedMarkets();
        ActorRef marketManagerActor = MarketStrategySystem.getInstance().getMarketManagerActor();
        epics.forEach(epic ->
          marketManagerActor.tell(
                new MarketManager.SubscribeToMarketUpdate(epic,getSelf()),getSelf())
        );

    }

    private void onActOnStrategyInstruction(ActOnStrategyInstruction pactOnStrategyInstruction) {
        LOG.info(pactOnStrategyInstruction.getInstruction());

    }

    public static final class ActOnStrategyInstruction{
        String instruction ="";

        public ActOnStrategyInstruction(String pinstructionStr){
            instruction = pinstructionStr;
        }

        public String getInstruction() {
            return instruction;
        }
    }
    public static final class StrategyCreated {
        private final String uniqId;

        public StrategyCreated(String puniqId) {
            uniqId = puniqId;
        }

        public String getUniqId() {
            return uniqId;
        }
    }
}
