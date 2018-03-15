package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.tradingservices.TradingAPI;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.StringJoiner;

public class StrategyActor extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private final String uniqId;
    private final ActorRef ownerActor;
    private MarketStrategyInterface marketStrategy;
    private TradingAPI tradingAPI;

    public StrategyActor(TradingAPI ptradingAPI,String puniqId,ActorRef pownerActor, MarketStrategyInterface pmarketStrategy){
        this.uniqId = puniqId;
        ownerActor = pownerActor;
        marketStrategy = pmarketStrategy;
        tradingAPI = ptradingAPI;
    }

    public static Props props(TradingAPI ptradingAPI, String puniqId, ActorRef ownerActor, MarketStrategyInterface pmarketStrategy){
        return Props.create(StrategyActor.class,ptradingAPI, puniqId,ownerActor,pmarketStrategy);
    }

    @Override
    public void preStart()
    {
       getContext().watch(ownerActor);
       marketStrategy.setStrategyInstructionConsumer(
                signal -> getSelf().tell(signal,getSelf())
        );
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
                .match(TradingSignal.class,this::onActOnStrategyInstruction)
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
            if(!positionUpdate.isClosed()) {
                marketStrategy.evaluate(positionUpdate);
            }else{
                getContext().stop(getSelf());
            }
        }
    }

    private void onMarketUpdate(MarketActor.MarketUpdated marketUpdated) {
        marketStrategy.evaluate(marketUpdated);
    }

    private void onCreateStrategy(StrategyManager.CreateStrategyMessage msg) {
        requestPositionUpdateSubscription();
        requestMarketUpdateSubscription();
        msg.getOwner().tell(new StrategyActor.StrategyCreated(msg.getUniqId()),getSelf());
        getSender().tell(new StrategyActor.StrategyCreated(msg.getUniqId()),getSelf());
    }

    private void requestPositionUpdateSubscription() {
        ArrayList<String> positions = marketStrategy.getListOfObservedPositions();
        ActorRef positionManager = MarketStrategySystem.getInstance().getPositionManagerActor();

        positions.forEach(strPosition ->
            positionManager.tell(new PositionManager.SubscribeToPositionUpdate(strPosition),getSelf())
        );
    }

    private void requestMarketUpdateSubscription() {
        ArrayList<String> epics = marketStrategy.getListOfObservedMarkets();
        ActorRef marketManagerActor = MarketStrategySystem.getInstance().getMarketManagerActor();
        epics.forEach(epic ->
          marketManagerActor.tell(
                new MarketManager.SubscribeToMarketUpdate(epic,getSelf()),getSelf())
        );

    }

    private void onActOnStrategyInstruction(TradingSignal tradingSignal ) throws Exception{
        LOG.info(tradingSignal.getInstruction());
        ActorRef portfolioManager = MarketStrategySystem.getInstance().getPortfolioManagerActor();
        portfolioManager.tell(tradingSignal,getSelf());

    }

    public static final class TradingSignal{
        public static final String ENTER_MARKET_INSTRUCTION="ENTER MARKET";
        public static final String EDIT_POSITION_INSTRUCTION="EDIT POSITION";
        String instruction ="";
        BigDecimal newStopLevel;
        BigDecimal newLimitLevel;
        String dealId;
        String epic;
        Direction direction;
        BigDecimal size;
        BigDecimal stopDistance;
        public static TradingSignal createEnterMarketSignal(
                String pEPIC
                ,Direction pDirection
                ,BigDecimal pSize
                ,BigDecimal pStopDistance){

            TradingSignal newSingal =  new TradingSignal();
            newSingal.setInstruction(ENTER_MARKET_INSTRUCTION);
            newSingal.setDirection(pDirection);
            newSingal.setEpic(pEPIC);
            newSingal.setStopDistance(pStopDistance);
            newSingal.setSize(pSize);

            return newSingal;
        }

        public static TradingSignal createEditPositionSignal(
                String pdealId
                , BigDecimal pnewStopLevel
                , BigDecimal pnewLimitLevel
        ){
            TradingSignal newSingal =  new TradingSignal();
            newSingal.setInstruction(EDIT_POSITION_INSTRUCTION);
            newSingal.setDealId(pdealId);
            newSingal.setNewStopLevel(pnewStopLevel);
            newSingal.setNewLimitLevel(pnewLimitLevel);


            return newSingal;
        }

        private TradingSignal(){

        }

        public void setInstruction(String instruction) {
            this.instruction = instruction;
        }

        public void setNewStopLevel(BigDecimal newStopLevel) {
            this.newStopLevel = newStopLevel;
        }

        public void setNewLimitLevel(BigDecimal newLimitLevel) {
            this.newLimitLevel = newLimitLevel;
        }

        public BigDecimal getStopDistance() {
            return stopDistance;
        }

        public void setStopDistance(BigDecimal stopDistance) {
            this.stopDistance = stopDistance;
        }

        public void setDealId(String dealId) {
            this.dealId = dealId;
        }

        public String getEpic() {
            return epic;
        }

        public void setEpic(String epic) {
            this.epic = epic;
        }

        public Direction getDirection() {
            return direction;
        }

        public void setDirection(Direction direction) {
            this.direction = direction;
        }

        public BigDecimal getSize() {
            return size;
        }

        public void setSize(BigDecimal size) {
            this.size = size;
        }

        public String getInstruction() {
            return instruction;
        }

        public BigDecimal getNewStopLevel(){
            return newStopLevel;
        }

        public BigDecimal getNewLimitLevel() {
            return newLimitLevel;
        }

        public String getDealId() {
            return dealId;
        }

        @Override
        public String toString() {
            StringJoiner instruction = new StringJoiner(",");
            if(EDIT_POSITION_INSTRUCTION.equalsIgnoreCase(this.getInstruction())){
                instruction.add(this.getInstruction());
                instruction.add(this.getEpic());
                instruction.add("DealID="+this.getDealId());
                instruction.add("STOP="+getNewStopLevel());
                instruction.add("LIMIT="+getNewLimitLevel());

            }else if(ENTER_MARKET_INSTRUCTION.equalsIgnoreCase(this.getInstruction())){
                instruction.add(this.getInstruction());
                instruction.add(this.getEpic());
                instruction.add("Direction="+this.getDirection().toString());
                instruction.add("Size="+this.getSize());
                instruction.add("STOP="+getStopDistance());
                instruction.add("LIMIT="+getNewLimitLevel());
            }
            return instruction.toString();
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
