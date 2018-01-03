package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.IGClientUtility;
import com.dario.agenttrader.dto.PositionInfo;

import java.util.Map;

public class Position extends AbstractActor{


    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private final String positionID;
    private PositionInfo positionInfo;

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
        }else if(opu.getPostionInfo()!=null){
            PositionInfo newInfo = opu.getPostionInfo();
            Map<String,String[]> delta = IGClientUtility.findDelta(newInfo,positionInfo);

            delta.forEach((k,v) -> LOG.info("Change detected: {} from {} to {}",k,v[0],v[1]));

            positionInfo=newInfo;
            getSender().tell(new PositionUpdated(positionID),getSelf());
        }
    }
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(PositionManager.OPU.class, this::onPositionUpdate)
                .match(PositionManager.RegisterPositionRequest.class, this::onRegisterPositionRequest
                ).build();
    }

    private void onRegisterPositionRequest(PositionManager.RegisterPositionRequest registerPositionRequest) {
        this.positionInfo = IGClientUtility.extractPositionInfo(registerPositionRequest);
        getSender().tell(new PositionManager.PositionRegistered(positionID),getSelf());
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
