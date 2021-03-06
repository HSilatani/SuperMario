package com.dario.agenttrader.strategies;

import com.dario.agenttrader.domain.*;
import com.dario.agenttrader.actors.MarketActor;
import com.dario.agenttrader.actors.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.StringJoiner;

public class TrackerStrategy extends AbstractMarketStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(TrackerStrategy.class);

    private PositionInfo positionInfo;

    private BigDecimal trailingStep = new BigDecimal(5);
    private Optional<BigDecimal> nextTrigger;
    private Optional<BigDecimal> currentAsk = Optional.empty();
    private Optional<BigDecimal> currentBid = Optional.empty();
    private Optional<BigDecimal> stopDistance = Optional.empty();
    private Optional<BigDecimal> currentStop;
    private BigDecimal openLevel;
    private int direction;
    private MarketInfo staticMarketInfo = null;
    //TODO: this needs to be calculated based on market spread and volatility. thisis tuned for HK
    private BigDecimal profitProtectingThresholdforLongPosition = BigDecimal.valueOf(100);
    private BigDecimal profitProtectingThresholdforShortPosition = BigDecimal.valueOf(40);


    public TrackerStrategy(ArrayList<String> epics,PositionInfo pPositionInfo) {
        super(epics);
        positionInfo = pPositionInfo;
        calculateDirection();
        openLevel =  positionInfo.getOpenLevelBigDecimal().orElseThrow(
                ()->new IllegalArgumentException("Position must have open level: " + positionInfo.getDealId()));

        currentStop = positionInfo.getStopLevelBigDecimal();
        nextTrigger = Optional.of(openLevel.add(trailingStep.multiply(BigDecimal.valueOf(direction))));
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
            stopDistance = Optional.of(openLevel.subtract(currentStop.get()).abs());
        }
    }

    @Override
    public void evaluate(MarketActor.MarketUpdated marketUpdate) {
        LOG.debug("Received update for {}",marketUpdate.getEpic());
        if(isMarketUpdateValid(marketUpdate)) {
            updateState(marketUpdate);
            boolean isStateValidForEvaluation = validateState();
            if (isStateValidForEvaluation) {
                evaluateStrategy();
            }
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
            LOG.info("Strategy {} for market {} wont evaluate because state is not valid:{}"
                    ,positionInfo.getDealId()
                    ,positionInfo.getEpic()
                    , stateMsg);
        }

        return isStateValid;
    }

    private void evaluateStrategy() {
        BigDecimal currentLevel = getCurrentApplicablePrice();
        BigDecimal oldTrigger = nextTrigger.get();

        if (isPriceBeyondTriggerPrice(currentLevel)){

                nextTrigger = Optional.of(currentLevel.add(trailingStep.multiply(BigDecimal.valueOf(direction))));
                calculateNewProfitProtectingStopDistance();
                BigDecimal newStop= currentLevel.subtract(stopDistance.get().multiply(BigDecimal.valueOf(direction)));
                boolean isNewStopValid = newStop.compareTo(currentStop.get())*direction>0;
                LOG.debug("New stop is valid:{}  {}->{}",isNewStopValid,currentStop.get(),newStop);
                if(isNewStopValid){
                    String instruction = "update stopLevel from " +currentStop.orElse(null) + "  -> : " + newStop;
                    LOG.info("Strategy {}-{} issuing instruction {} : Trigger={}->{},currentLevel={}"
                            ,positionInfo.getDealId()
                            ,staticMarketInfo.getMarketName()
                            ,instruction
                            ,oldTrigger
                            ,nextTrigger.get()
                            ,currentLevel);
                    TradingSignal strategySignal =
                            TradingSignal.createEditPositionSignal(
                                    positionInfo.getDealId()
                                    ,newStop
                                    ,null
                            );
                    strategyInstructionConsumer.accept(strategySignal);
                }
        }

        if(nextTrigger.isPresent() && nextTrigger.get().subtract(currentLevel).abs().compareTo(trailingStep)>0){
            nextTrigger = Optional.of(currentLevel.add(trailingStep.multiply(BigDecimal.valueOf(direction))));

            LOG.info("Strategy {}-{} correcting trigger: Trigger={}->{},currentLevel={}"
                    ,positionInfo.getDealId()
                    ,staticMarketInfo.getMarketName()
                    ,oldTrigger
                    ,nextTrigger.get()
                    ,currentLevel);
        }
    }

    private void calculateNewProfitProtectingStopDistance() {
        BigDecimal profitProtectingStopDistance = null;
        BigDecimal profitThreshold = profitProtectingThresholdforLongPosition;
        if(direction == -1){
            profitThreshold = profitProtectingThresholdforShortPosition;
        }


        if(positionInfo.getOpenLevelBigDecimal().isPresent()){
            BigDecimal openLevel = positionInfo.getOpenLevelBigDecimal().get();
            BigDecimal applicablePrice = getCurrentApplicablePrice();
            BigDecimal plFactor = applicablePrice.subtract(openLevel);
            plFactor = plFactor.multiply(new BigDecimal(direction));
            LOG.info("P&L: {},{},{},O:{},L:{}"
                    ,positionInfo.getDealId()
                    ,positionInfo.getEpic()
                    ,plFactor.setScale(2,BigDecimal.ROUND_HALF_UP)
                    ,openLevel.setScale(2,BigDecimal.ROUND_HALF_UP)
                    ,applicablePrice.setScale(2,BigDecimal.ROUND_HALF_UP));
            int comparePLFactorGreaterToThreashold = plFactor.compareTo(profitThreshold);
            if(comparePLFactorGreaterToThreashold>0){
                profitProtectingStopDistance = staticMarketInfo.getMinNormalStopLimitDistance();
            }

            if(profitProtectingStopDistance!=null && profitProtectingStopDistance.compareTo(stopDistance.get())!=0){
                LOG.info("Changing StopDistance for strategy {}-{} from {} to ProfitProtecting Stop {}"
                        ,positionInfo.getDealId()
                        ,staticMarketInfo.getMarketName()
                        ,stopDistance.get()
                        ,profitProtectingStopDistance);
                stopDistance = Optional.ofNullable(profitProtectingStopDistance);
            }
        }
    }

    private boolean isPriceBeyondTriggerPrice(BigDecimal currentLevel){
        BigDecimal directionisedCurrentLevel = currentLevel.multiply(BigDecimal.valueOf(direction));
        BigDecimal directionisedTriggerLevel = nextTrigger.get().multiply(BigDecimal.valueOf(direction));
        int comparisonResult = directionisedCurrentLevel.compareTo(directionisedTriggerLevel);
        return comparisonResult>-1;
    }

    @Override
    public void evaluate(Position.PositionUpdate positionUpdate) {
        LOG.debug("Received update for position {}",positionUpdate.getPositionId());
        if(positionInfo.getDealId().equalsIgnoreCase(positionUpdate.getPositionId())){
            updateState(positionUpdate);
       }else{
            LOG.warn("Expecting update for ["+positionInfo.getDealId()+"] but received update for["+positionUpdate.getPositionId()+"]");

        }
    }

    @Override
    public ArrayList<String> getListOfObservedPositions() {
        ArrayList<String> positionList = new ArrayList<>();
        positionList.add(positionInfo.getDealId());
        return positionList;
    }

    private void updateState(MarketActor.MarketUpdated<PriceTick> marketUpdated){
        PriceTick priceTick = marketUpdated.getMarketupdate().getUpdate();
        currentAsk = Optional.ofNullable(priceTick.getOffer());
        currentBid = Optional.ofNullable(priceTick.getBid());
        staticMarketInfo = marketUpdated.getMarketupdate().getMarketInfo();
        LOG.debug("Updating state ask:{},bid:{}",currentAsk.orElse(null),currentBid.orElse(null));
    }
    private void updateState(Position.PositionUpdate positionUpdate){
        UpdateEvent updateEvent = positionUpdate.getUpdateEvent();
        if(updateEvent.isPositionUpdate()) {
            PositionInfo positionInfo = new PositionInfo(updateEvent,"",1);
            currentStop = positionInfo.getStopLevelBigDecimal();
            calculateStopDistantOnlyIfItIsNotSetAlready();
        }


    }

    private boolean isMarketUpdateValid(MarketActor.MarketUpdated marketUpdated) throws IllegalArgumentException{
        String epic = marketUpdated.getEpic();
        Optional<String> matchingObservedMarket = getListOfObservedMarkets().stream()
                .filter(observedEpic-> observedEpic.equalsIgnoreCase(epic))
                .findFirst();
        if(!matchingObservedMarket.isPresent()) {
            throw new IllegalArgumentException("Expecting updates for one of " + getListOfObservedMarkets()
                    + " but received update for " + epic);
        }
        boolean isValid = true;
        if(! (marketUpdated.getMarketupdate().getUpdate() instanceof PriceTick)){
            isValid=false;
        }

        return  isValid;
    }


    public BigDecimal getCurrentApplicablePrice() {
        BigDecimal currentApplicablePrice;

        if(direction == 1){
            currentApplicablePrice = currentBid.get();
        }else if (direction == -1){
            currentApplicablePrice = currentAsk.get();
        }else{
            throw new IllegalStateException("Invalid value for position direction :" + direction);
        }

        return currentApplicablePrice;
    }
}
