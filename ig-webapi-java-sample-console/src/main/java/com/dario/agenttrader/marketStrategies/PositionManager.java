package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.dto.PositionUpdate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PositionManager extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private Map<String,ActorRef> idToPosition = new HashMap<String, ActorRef>();
    private Map<ActorRef,String> positionToId = new HashMap<ActorRef,String>();


    public PositionManager(){

    }

    public static final Props props(){
        return Props.create(PositionManager.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(RegisterPosition.class, this::onRegiserPosition)
                .match(ListPositions.class,this::onListPosition)
                .match(Terminated.class,this::onTerminated)
                .match(OPU.class,this::onOPU)
                .match(Position.PositionUpdated.class,this::onPositionUpdated)
                .build();
    }

    private void onPositionUpdated(Position.PositionUpdated positionupdated){
        //Ignore confirms for now
    }

    private void onOPU(OPU opu) {
        LOG.info("OPU {}-{}",opu.getPostionUpdate().getS()
                , opu.getPostionUpdate().getDealId()
        );

        String positionId = opu.getPostionUpdate().getDealId();

        registerPosition(positionId,opu);
    }

    private void onListPosition(ListPositions p) {
        getSender().tell(new ListPositionResponse(idToPosition.keySet()),getSelf());
    }

    public void onTerminated(Terminated t) {
        ActorRef position = t.getActor();
        String positionId = positionToId.get(position);
        idToPosition.remove(positionId);
        positionToId.remove(position);
        LOG.info("Actor for Position {} is removed",positionId);
    }

    private void onRegiserPosition(RegisterPosition msg) {
        String regPosId = msg.getPositionId();
        registerPosition(regPosId,msg);
    }

    private void registerPosition(String regPosId,Object message) {
        ActorRef positionActor = idToPosition.get(regPosId);
        if(null == positionActor ){
            positionActor = getContext().actorOf(Position.props(regPosId),regPosId);
            getContext().watch(positionActor);
            idToPosition.put(regPosId,positionActor);
            positionToId.put(positionActor,regPosId);
        }
        positionActor.forward(message,getContext());
    }

    public static final class RegisterPosition{

        private final String positionId;

        RegisterPosition(String ppositionId){
            this.positionId = ppositionId;
        }

        public String getPositionId() {
            return positionId;
        }

    }

    public static final class PositionRegistered{

        private final String positionId;

        PositionRegistered(String ppositionId){
            this.positionId = ppositionId;
        }

        public String getPositionId() {
            return positionId;
        }
    }

    public static final class ListPositions{

    }

    public static final class ListPositionResponse{
        private final Set<String> positionIDs;

        ListPositionResponse(Set<String> pPositionIDs){
            this.positionIDs = pPositionIDs;
        }

        public Set<String> getPositionIDs() {
            return positionIDs;
        }
    }

    public static final class OPU {
        public static final boolean POSITION_CLOSED = true;
        private final PositionUpdate postionUpdate;
        private boolean closed=false;

        public OPU(PositionUpdate ppositionUpdate) {
            this.postionUpdate = ppositionUpdate;
        }

        public OPU(PositionUpdate ppositionUpdate, boolean positionClosed) {
            this.closed = positionClosed;
            postionUpdate = ppositionUpdate;
        }

        public PositionUpdate getPostionUpdate() {
            return postionUpdate;
        }

        public boolean isClosed() {
            return closed;
        }
    }

}
