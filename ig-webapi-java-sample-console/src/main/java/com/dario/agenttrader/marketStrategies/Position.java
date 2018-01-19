package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Inbox;
import akka.actor.Props;
import akka.dispatch.Mailbox;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.dto.UpdateEvent;
import com.dario.agenttrader.utility.IGClientUtility;
import com.dario.agenttrader.dto.PositionInfo;

import java.util.ArrayList;
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
        LOG.info("Position {}registered", positionID);
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
            getSender().tell(
                    new PositionUpdatedDelta(positionID,newInfo.getKeyValues().get(PositionInfo.EPIC_KEY),delta)
                    ,getSelf());

            PositionUpdate positionUpdate = new PositionUpdate(
                    positionID,positionInfo.getEpic()
                    ,new UpdateEvent(positionInfo.getKeyValues(),UpdateEvent.POSITION_UPDATE));

            ActorRef strategyManager = MarketStrategySystem.getInstance().getStrategyManagerActor();
            strategyManager.tell(positionUpdate,getSelf());
        }
    }
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(PositionManager.OPU.class, this::onPositionUpdate)
                .match(PositionManager.RegisterPositionRequest.class, this::onRegisterPositionRequest)
                .build();
    }

    private void onRegisterPositionRequest(PositionManager.RegisterPositionRequest registerPositionRequest) {
        this.positionInfo = IGClientUtility.extractPositionInfo(registerPositionRequest);
        createDefaultStrategies();
        getSender().tell(new PositionManager.PositionRegistered(positionID),getSelf());
    }

    private void createDefaultStrategies() {
        ArrayList<String> epics = new ArrayList<>();
        epics.add(positionInfo.getKeyValues().get(PositionInfo.EPIC_KEY));
        String uniqStrategyID = positionID +"-"+epics.get(0);
        if(epics.get(0).contains("ETH")) {
            MarketStrategyInterface trackerMarketStrategy = new TrackerStrategy(epics, positionInfo);
            MarketStrategySystem.getInstance().getStrategyManagerActor().tell(
                    new StrategyManager.CreateStrategyMessage(getSelf(), uniqStrategyID, trackerMarketStrategy), getSelf());
        }
    }


    public static final class PositionUpdatedDelta {
        private final String positionId;
        private final String epic;
        private final Map<String,String[]> delta;



        public PositionUpdatedDelta(String ppositionID, String pepic, Map<String, String[]> pdelta) {
            delta = pdelta;
            positionId = ppositionID;
            epic = pepic;
        }

        public String getPositionId(){
            return  positionId;
        }

        public String getEpic() {
            return epic;
        }

        public Map<String, String[]> getDelta() {
            return delta;
        }
    }

    public static final class PositionUpdate{
        private final String positionId;
        private final String epic;
        private final UpdateEvent updateEvent;

        public PositionUpdate(String positionId, String epic, UpdateEvent updateEvent) {
            this.positionId = positionId;
            this.epic = epic;
            this.updateEvent = updateEvent;
        }

        public String getPositionId() {
            return positionId;
        }

        public String getEpic() {
            return epic;
        }

        public UpdateEvent getUpdateEvent() {
            return updateEvent;
        }
    }
}
