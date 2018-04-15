package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.dto.UpdateEvent;
import com.dario.agenttrader.strategies.TrackerStrategy;
import com.dario.agenttrader.utility.IGClientUtility;
import com.dario.agenttrader.dto.PositionInfo;
import com.dario.agenttrader.utility.SubscriberActorRegistery;

import java.util.ArrayList;
import java.util.Map;

public class Position extends AbstractActor{


    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private final String positionID;
    private PositionInfo positionInfo;
    private SubscriberActorRegistery<PositionUpdate> subscribers;

    public Position(String ppositionId){
        this.positionID = ppositionId;
        subscribers = new SubscriberActorRegistery<>();
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
        PositionUpdate positionClosed = new PositionUpdate(positionID,positionInfo.getEpic(),PositionInfo.STATUS_DELETED);
        subscribers.informSubscriobers(positionClosed,null);
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

            subscribers.informSubscriobers(positionUpdate,getSelf());
        }
    }
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(PositionManager.OPU.class, this::onPositionUpdate)
                .match(PositionManager.RegisterPositionRequest.class, this::onRegisterPositionRequest)
                .match(PositionManager.SubscribeToPositionUpdate.class,this::onSubscribeToPositionUpdate)
                .match(Terminated.class,this::onTerminated)
                .build();
    }

    private void onTerminated(Terminated t) {
        ActorRef actor = t.getActor();
        subscribers.removeActor(actor);
    }

    private void onSubscribeToPositionUpdate(PositionManager.SubscribeToPositionUpdate subscribeToPositionUpdate) {
        if(positionID.equalsIgnoreCase(subscribeToPositionUpdate.getPositionId())){
            subscribers.registerSubscriber(getSender(),getContext());
        }else{
            LOG.warning("Cant accept for positions update for {} because I don't own the position.",subscribeToPositionUpdate.getPositionId());
        }
    }

    private void onRegisterPositionRequest(PositionManager.RegisterPositionRequest registerPositionRequest) {
        this.positionInfo = IGClientUtility.extractPositionInfo(registerPositionRequest);
        createDefaultStrategies();
        getSender().tell(new PositionManager.PositionRegistered(positionID),getSelf());
    }

    private void createDefaultStrategies() {
        createTrackerStrategy();
    }



    private void createTrackerStrategy(){
        ArrayList<String> epics = new ArrayList<>();
        epics.add(positionInfo.getKeyValues().get(PositionInfo.EPIC_KEY));
        String uniqStrategyID = positionID +"-"+epics.get(0);
        MarketStrategyInterface trackerMarketStrategy = new TrackerStrategy(epics, positionInfo);
        MarketStrategySystem.getInstance().getStrategyManagerActor().tell(
                new StrategyManager.CreateStrategyMessage(getSelf(), uniqStrategyID, trackerMarketStrategy), getSelf());
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
        private UpdateEvent updateEvent;
        private boolean isClosed=false;

        public PositionUpdate(String positionId, String epic, UpdateEvent updateEvent) {
            this(positionId,epic,PositionInfo.STATUS_OPEN);
            this.updateEvent = updateEvent;
        }

        public PositionUpdate(String positionId,String epic,String status){
            this.positionId = positionId;
            this.epic = epic;
            isClosed= PositionInfo.STATUS_DELETED.equalsIgnoreCase(status);
            updateEvent = null;
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

        public boolean isClosed() {
            return isClosed;
        }
    }
}
