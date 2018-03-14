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
        String instruction ="";
        BigDecimal newStop;
        BigDecimal newLimit;
        String dealId;
        String epic;
        Direction direction;
        BigDecimal size;

        public static TradingSignal createEnterMarketSignal(
                String pInstruction
                ,String pEPIC
                ,Direction pDirection
                ,BigDecimal pSize
                ,BigDecimal pStop){

            TradingSignal newSingal =  new TradingSignal();
            newSingal.setInstruction(pInstruction);
            newSingal.setDirection(pDirection);
            newSingal.setEpic(pEPIC);
            newSingal.setNewStop(pStop);
            newSingal.setSize(pSize);

            return newSingal;
        }

        public static TradingSignal createEditPositionSignal(
                String pdealId
                , BigDecimal pnewStop
                , BigDecimal pnewLimit
                , String pInstruction
        ){
            TradingSignal newSingal =  new TradingSignal();
            newSingal.setInstruction(pInstruction);
            newSingal.setDealId(pdealId);
            newSingal.setNewStop(pnewStop);
            newSingal.setNewLimit(pnewLimit);


            return newSingal;
        }

        private TradingSignal(){

        }

        public void setInstruction(String instruction) {
            this.instruction = instruction;
        }

        public void setNewStop(BigDecimal newStop) {
            this.newStop = newStop;
        }

        public void setNewLimit(BigDecimal newLimit) {
            this.newLimit = newLimit;
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

        public BigDecimal getNewStop() {
            return newStop;
        }

        public BigDecimal getNewLimit() {
            return newLimit;
        }

        public String getDealId() {
            return dealId;
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
