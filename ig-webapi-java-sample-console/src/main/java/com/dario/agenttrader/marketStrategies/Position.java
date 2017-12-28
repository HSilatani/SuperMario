package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Position extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private final String positionID;

    public Position(String ppositionId){
        this.positionID = ppositionId;
    }

    public static Props props(String ppositionId){
        return Props.create(Position.class,ppositionId);
    }

    @Override
    public void preStart() {
        LOG.info("Position {} registered", positionID);
    }

    @Override
    public void postStop() {
        LOG.info("Position {} unregistered", positionID);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(UpdatePosition.class, p ->
        {
            LOG.info("Position {} updated",positionID);
            getSender().tell(new Position.PositionUpdated(positionID),getSelf());
        }).match(PositionManager.RegisterPosition.class, r ->{
            getSender().tell(new PositionManager.PositionRegistered(positionID),getSelf());
        }).build();
    }

    public static final class UpdatePosition{

    }

    public static final class PositionUpdated{
        private final String positionId;

        PositionUpdated(String ppositionId){
            this.positionId = ppositionId;
        }

        public String getPositionId(){
            return  positionId;
        }

    }
}
