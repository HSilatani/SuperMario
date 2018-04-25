package com.dario.agenttrader.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.domain.UpdateEvent;
import com.dario.agenttrader.tradingservices.TradingAPI;
import com.dario.agenttrader.tradingservices.TradingDataStreamingService;
import com.dario.agenttrader.utility.ActorRegistery;
import com.dario.agenttrader.utility.IGClientUtility;
import com.dario.agenttrader.InterpreterAgent;
import com.dario.agenttrader.domain.PositionInfo;
import com.dario.agenttrader.domain.PositionSnapshot;
import com.dario.agenttrader.utility.SubscriberActorRegistery;
import com.lightstreamer.ls_client.UpdateInfo;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class PositionManager extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private final ActorRegistery opuSubscriberRegistry = new ActorRegistery();

    private final SubscriberActorRegistery<List<PositionSnapshot>>
            addRemovePositionSubscriberRegistry = new SubscriberActorRegistery();

    private final TradingAPI tradingAPI;

    private final TradingDataStreamingService tradingDataStreamingService =TradingDataStreamingService.getInstance();


    public PositionManager(TradingAPI ptradingAPI){
        tradingAPI = ptradingAPI;
    }

    public static final Props props(TradingAPI tradingAPI){
        return Props.create(PositionManager.class,tradingAPI);
    }

    @Override
    public void preStart(){
        opuSubscriberRegistry.actorAddRemoveHook(t->getSelf().tell(new PositionAddRemoveEvent(),getSelf()));
        LOG.info("PositionManager Started");
    }
    @Override
    public Receive createReceive() {
        return receiveBuilder().match(RegisterPositionRequest.class, this::onRegiserPosition)
                .match(ListPositions.class,this::onListPosition)
                .match(Terminated.class,this::onTerminated)
                .match(OPU.class,this::onOPU)
                .match(Position.PositionUpdatedDelta.class,this::onPositionUpdated)
                .match(LoadPositionsRequest.class,this::onLoadPositions)
                .match(PositionRegistered.class,this::onPositionRegistered)
                .match(SubscribeToPositionUpdate.class,this::onSubscribeToPositionUpdate)
                .match(SubscribeToRegisterUnRegisterPosition.class,this::onSubscribeToRegisterUnRegisterPosition)
                .match(PositionAddRemoveEvent.class,this::onPositionAddRemoveEvent)
                .build();
    }

    private void onPositionAddRemoveEvent(PositionAddRemoveEvent p){
        List<PositionSnapshot> positionSnapshots = null;
        try {
            positionSnapshots = tradingAPI.listOpenPositions();
            addRemovePositionSubscriberRegistry.informSubscriobers(positionSnapshots,getSelf());
        } catch (Exception e) {
            LOG.warning("unable to update addRemovePositionSubscribers",e);
        }

    }

    private void onSubscribeToRegisterUnRegisterPosition(
            SubscribeToRegisterUnRegisterPosition p) {
        addRemovePositionSubscriberRegistry.registerSubscriber(getSender(),getContext());
    }

    private void onSubscribeToPositionUpdate(SubscribeToPositionUpdate subscribeToPositionUpdate) {
        ActorRef positionActor = opuSubscriberRegistry.getActorForUniqId(subscribeToPositionUpdate.getPositionId());
        if(positionActor!=null){
            positionActor.forward(subscribeToPositionUpdate,getContext());
        }
    }

    private void onPositionRegistered(PositionRegistered positionRegistered) {
        LOG.info("Registration for position {} is confirmed",positionRegistered.getPositionId());
    }

    private void onLoadPositions(LoadPositionsRequest loadPositionsRequest) throws Exception {
            List<PositionSnapshot> positionSnapshots = tradingAPI.listOpenPositionsWithProfitAndLoss();

            positionSnapshots.forEach(psnapshot ->{
                registerNewPosition(psnapshot);
            });

            subscribeToPositionUpdates();
    }

    private void registerNewPosition(PositionSnapshot psnapshot) {
        RegisterPositionRequest registerPositionRequest =
                new RegisterPositionRequest(psnapshot.getPositionId(),psnapshot);
        getSelf().forward(registerPositionRequest,getContext());
    }
    private void subscribeToPositionUpdates() throws Exception{
        Consumer<UpdateInfo> consumer = updateInfo -> {
            UpdateEvent positionUpdateEvent =
                    new UpdateEvent(IGClientUtility.flatJSontoMap(updateInfo.getNewValue(1)),UpdateEvent.POSITION_UPDATE);
            PositionInfo positionInfo = new PositionInfo(positionUpdateEvent);

            if (updateInfo.getNewValue("OPU") != null)
            {
                boolean isclosed=
                        PositionInfo.STATUS_DELETED.equalsIgnoreCase(positionInfo.getStatus());

                LOG.info("Position update data {}",updateInfo);
                getSelf().tell(
                        new PositionManager.OPU(positionInfo,isclosed),
                        getSelf());
            }
        };
        tradingDataStreamingService.subscribeToOpenPositionUpdates(this.toString(),consumer);
    }

    private void onPositionUpdated(Position.PositionUpdatedDelta positionupdated){
        InterpreterAgent.getInstance().sendMessage(positionupdated);
    }

    private void onOPU(OPU opu) throws Exception{
        LOG.info("OPU {}-{}-{}",opu.getPostionInfo().getS()
                , opu.getPostionInfo().getDealId()
                ,opu.getPostionInfo().getStatus()
        );

        String positionId = opu.getPostionInfo().getDealId();

        ActorRef positionActor = opuSubscriberRegistry.getActorForUniqId(positionId);

        if(positionActor!=null){
            positionActor.forward(opu,getContext());
        }else{
            PositionSnapshot psnap = tradingAPI.getPositionSnapshot(positionId);
            if(psnap!=null){
                registerNewPosition(psnap);
            }

        }

    }

    private void onListPosition(ListPositions p) {
        Set<String> uniqIds = opuSubscriberRegistry.getUniqIds();
        getSender().tell(new ListPositionResponse(uniqIds),getSelf());
    }

    public void onTerminated(Terminated t) {
        ActorRef actor = t.getActor();
        String opuSubscriberActorRef = opuSubscriberRegistry.removeActor(actor);
        addRemovePositionSubscriberRegistry.removeActor(actor);
        String message = Optional.ofNullable(opuSubscriberActorRef).orElse("AddRemovePositionSubscriber");
        LOG.info("Actor {} is removed",message);
    }

    private void onRegiserPosition(RegisterPositionRequest msg) {
        String regPosId = msg.getPositionId();
        Props props = Position.props(regPosId);
        opuSubscriberRegistry.registerActorIfAbscent(getContext(),props,regPosId,msg);
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
        public LoadPositionsRequest() {

        }
    }

    public static final class SubscribeToPositionUpdate {
        private final String positionId;
        public SubscribeToPositionUpdate(String strPosition) {
            positionId = strPosition;
        }

        public String getPositionId() {
            return positionId;
        }
    }

    public static final class SubscribeToRegisterUnRegisterPosition {
    }

    public static final class PositionAddRemoveEvent{}
}
