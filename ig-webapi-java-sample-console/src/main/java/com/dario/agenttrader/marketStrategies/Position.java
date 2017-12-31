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

    public void onPositionUpdate(PositionManager.OPU opu){

        if(opu.isClosed()){
          getContext().stop(getSelf());
        }else if(opu.getPostionUpdate()!=null){
            LOG.info("New Value STOP:{} registered", opu.getPostionUpdate().getStop());
            getSender().tell(new PositionUpdated(positionID),getSelf());
        }
    }
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(PositionManager.OPU.class, this::onPositionUpdate)
                .match(PositionManager.RegisterPosition.class, r ->{
            getSender().tell(new PositionManager.PositionRegistered(positionID),getSelf());
        }).build();
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
