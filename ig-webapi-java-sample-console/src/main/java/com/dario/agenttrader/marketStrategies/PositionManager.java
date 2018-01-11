package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.tradingservices.IGClient;
import com.dario.agenttrader.utility.ActorRegistery;
import com.dario.agenttrader.utility.IGClientUtility;
import com.dario.agenttrader.InterpreterAgent;
import com.dario.agenttrader.dto.PositionInfo;
import com.dario.agenttrader.dto.PositionSnapshot;
import com.iggroup.webapi.samples.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.UpdateInfo;

import java.util.List;
import java.util.Set;

public class PositionManager extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private final ActorRegistery registry = new ActorRegistery();


    public PositionManager(){

    }

    public static final Props props(){
        return Props.create(PositionManager.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(RegisterPositionRequest.class, this::onRegiserPosition)
                .match(ListPositions.class,this::onListPosition)
                .match(Terminated.class,this::onTerminated)
                .match(OPU.class,this::onOPU)
                .match(Position.PositionUpdated.class,this::onPositionUpdated)
                .match(LoadPositionsRequest.class,this::onLoadPositions)
                .match(PositionRegistered.class,this::onPositionRegistered)
                .build();
    }

    private void onPositionRegistered(PositionRegistered positionRegistered) {
        LOG.info("Registration for position {} is confirmed",positionRegistered.getPositionId());
    }

    private void onLoadPositions(LoadPositionsRequest loadPositionsRequest) {
        try {
            List<PositionSnapshot> positionSnapshots =
                    loadPositionsRequest.igClient.listOpenPositions();

            positionSnapshots.forEach(psnapshot ->{
                RegisterPositionRequest registerPositionRequest =
                        new RegisterPositionRequest(psnapshot.getPositionId(),psnapshot);
                getSelf().forward(registerPositionRequest,getContext());
            });

            subscribeToPositionUpdates(loadPositionsRequest.igClient);

        } catch (Exception e) {
            LOG.error(e, "Filed to load positions!");
        }
    }

    private void subscribeToPositionUpdates(IGClient igClient) throws Exception{
        ActorRef positionManagerActor = getSelf();

        igClient.subscribeToOpenPositionUpdates(
                new HandyTableListenerAdapter() {
                    @Override
                    public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                        PositionInfo positionInfo = new PositionInfo(
                                IGClientUtility.flatJSontoMap(updateInfo.getNewValue(1)),s,i);

                                if (updateInfo.getNewValue("OPU") != null)
                                {
                                    LOG.info("Position update i {} s {} data {}", i, s, updateInfo);
                                     positionManagerActor.tell(
                                            new PositionManager.OPU(positionInfo),
                                            positionManagerActor);
                                }
                    }
                }
        );
    }

    private void onPositionUpdated(Position.PositionUpdated positionupdated){
        InterpreterAgent.getInstance().sendMessage(positionupdated);
    }

    private void onOPU(OPU opu) {
        LOG.info("OPU {}-{}",opu.getPostionInfo().getS()
                , opu.getPostionInfo().getDealId()
        );

        String positionId = opu.getPostionInfo().getDealId();

        ActorRef positionActor = registry.getActorForUniqId(positionId);

        positionActor.forward(opu,getContext());
    }

    private void onListPosition(ListPositions p) {
        Set<String> uniqIds = registry.getUniqIds();
        getSender().tell(new ListPositionResponse(uniqIds),getSelf());
    }

    public void onTerminated(Terminated t) {
        ActorRef position = t.getActor();
        String positionId = registry.removeActor(position);
        LOG.info("Actor for Position {} is removed",positionId);
    }

    private void onRegiserPosition(RegisterPositionRequest msg) {
        String regPosId = msg.getPositionId();
        Props props = Position.props(regPosId);
        registry.registerActorIfAbscent(getContext(),props,regPosId,msg);
    }


    public static final class RegisterPositionRequest {

        private final String positionId;
        private final PositionSnapshot positionSnapshot;

        public RegisterPositionRequest(String ppositionId, PositionSnapshot ppositionSnapshot){
            this.positionId = ppositionId;
            this.positionSnapshot = ppositionSnapshot;
        }

        public String getPositionId() {
            return positionId;
        }

        public PositionSnapshot getPositionSnapshot() {
            return positionSnapshot;
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
        private final PositionInfo postionUpdate;
        private boolean closed=false;

        public OPU(PositionInfo ppositionInfo) {
            this.postionUpdate = ppositionInfo;
        }

        public OPU(PositionInfo ppositionInfo, boolean positionClosed) {
            this.closed = positionClosed;
            postionUpdate = ppositionInfo;
        }

        public PositionInfo getPostionInfo() {
            return postionUpdate;
        }

        public boolean isClosed() {
            return closed;
        }
    }

    public static final class LoadPositionsRequest {
        private final IGClient igClient;
        public LoadPositionsRequest(IGClient pigClient) {
            igClient=pigClient;
        }
    }
}
