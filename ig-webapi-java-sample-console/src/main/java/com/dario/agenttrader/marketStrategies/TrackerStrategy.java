package com.dario.agenttrader.marketStrategies;

import com.dario.agenttrader.dto.MarketInfo;
import com.dario.agenttrader.dto.PositionInfo;
import com.dario.agenttrader.dto.UpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Optional;
import java.util.StringJoiner;

public class TrackerStrategy extends AbstractMarketStrategy {
    //TODO use consumer instead of passing Actor Ref
    //TODO maths and conversion between doubl and String is creating undesired effects , fixed


    private static final Logger LOG = LoggerFactory.getLogger(TrackerStrategy.class);

    private PositionInfo positionInfo;

    private Double trailingStep = new Double(1);
    private Optional<Double> nextTrigger;
    private Optional<Double> currentAsk = Optional.empty();
    private Optional<Double> currentBid = Optional.empty();
    private Optional<Double> stopDistance = Optional.empty();
    private Optional<Double> currentStop;
    private Double openLevel;
    private double direction;


    public TrackerStrategy(ArrayList<String> epics,PositionInfo pPositionInfo) {
        super(epics);
        positionInfo = pPositionInfo;
        calculateDirection();
        openLevel =  positionInfo.getOpenLevelDouble().orElseThrow(
                ()->new IllegalArgumentException("Position must have open level: " + positionInfo.getDealId()));

        currentStop = positionInfo.getStopLevelDouble();
        nextTrigger = Optional.of(openLevel+(direction * trailingStep));
        calculateStopDistantOnlyIfItIsNotSetAlready();
    }

    private void calculateDirection() {
        String strDirection = Optional.ofNullable(positionInfo.getDirection()).orElseThrow(
                ()-> new IllegalArgumentException("Position must have Direction: " + positionInfo.getDealId()));

        if(PositionInfo.DIRECTION_BUY.equalsIgnoreCase(strDirection)){
            direction = +1;
        }else if(PositionInfo.DIRECTION_SELL.equalsIgnoreCase(strDirection)){
            direction = -1;
        }else{
            throw new IllegalArgumentException("Position direction must either BUY or SELL. Current value is " + strDirection);
        }
    }

    private void calculateStopDistantOnlyIfItIsNotSetAlready() {
        if(!stopDistance.isPresent() && currentStop.isPresent()){
            stopDistance = Optional.of(Math.abs(openLevel.doubleValue()-currentStop.get().doubleValue()));
        }
    }

    @Override
    public void evaluate(MarketActor.MarketUpdated marketUpdate) {
        LOG.info("Received update for {}",marketUpdate.getEpic());
        isMarketUpdateValid(marketUpdate.getEpic());
        updateState(marketUpdate.getUpdateEvent());
        boolean isStateValidForEvaluation = validateState();
        if(isStateValidForEvaluation){
            evaluateStrategy();
        }
    }

    private boolean validateState(){
        StringJoiner stateMsg = new StringJoiner(",");
        boolean isStateValid = true;
        if (!stopDistance.isPresent()){
            isStateValid = false;
            stateMsg.add("StopLevel not available");
        }
        if(!(direction==1 || direction==-1)){
            isStateValid = false;
            stateMsg.add("Invalid direction = "+direction);
        }
        if(!nextTrigger.isPresent()){
            isStateValid = false;
            stateMsg.add("NextTrigerLevel not available");
        }
        if(!(trailingStep!=null && trailingStep.longValue()>-1)){
            isStateValid = false;
            stateMsg.add("TrailingStep not available");
        }
        if(!(currentBid.isPresent() && currentAsk.isPresent())){
            isStateValid = false;
            stateMsg.add("Ask or Bid price missing not available");
        }

        if(!isStateValid){
            LOG.info("Strategy wont evaluate because state is not valid:" + stateMsg);
        }

        return isStateValid;
    }

    private void evaluateStrategy() {
        double currentLevel = getCurrentApplicablePrice();
        if ((currentLevel * direction) >= (nextTrigger.get() * direction)){
                nextTrigger = Optional.of(currentLevel + (direction * trailingStep));
                double newStop= currentLevel - (direction * stopDistance.get());
                String instruction = "update stopLevel from " +currentStop.orElse(null) + "  -> : " + newStop;
                getOwnerStrategyActor().tell(new StrategyActor.ActOnStrategyInstruction(instruction),null);
        }
    }

    @Override
    public void evaluate(Position.PositionUpdate positionUpdate) {
        LOG.info("Received update for position {}",positionUpdate.getPositionId());
        if(positionInfo.getDealId().equalsIgnoreCase(positionUpdate.getPositionId())){
            updateState(positionUpdate.getUpdateEvent());
       }else{
            LOG.warn("Expecting update for ["+positionInfo.getDealId()+"] but received update for["+positionUpdate.getPositionId()+"]");

        }
    }

    @Override
    public ArrayList<String> getListOfObservedPositions() {
        ArrayList<String> positionList = new ArrayList<>();
        positionList.add(positionInfo.getEpic());
        return positionList;
    }

    private void updateState(UpdateEvent updateEvent) {

        if(updateEvent.isMarketUpdate()){
            MarketInfo marketInfo =new MarketInfo(updateEvent);
         currentAsk = marketInfo.getMarketUpdateOfrDouble();
         currentBid = marketInfo.getMarketUpdateBidDouble();
        }
        if(updateEvent.isPositionUpdate()) {
            PositionInfo positionInfo = new PositionInfo(updateEvent,"",1);
            currentStop = positionInfo.getStopLevelDouble();
            calculateStopDistantOnlyIfItIsNotSetAlready();
        }


    }

    private void isMarketUpdateValid(String epic) throws IllegalArgumentException{
        Optional<String> matchingObservedMarket = getListOfObservedMarkets().stream()
                .filter(observedEpic-> observedEpic.equalsIgnoreCase(epic))
                .findFirst();
        if(!matchingObservedMarket.isPresent())
            throw new IllegalArgumentException("Expecting updates for one of " +getListOfObservedMarkets()
                    + " but received update for " + epic);
    }


    public Double getCurrentApplicablePrice() {
        Double currentApplicablePrice;

        if(direction == 1){
            currentApplicablePrice = currentAsk.get();
        }else if (direction == -1){
            currentApplicablePrice = currentBid.get();
        }else{
            throw new IllegalStateException("Invalid value for position direction :" + direction);
        }

        return currentApplicablePrice;
    }
}
