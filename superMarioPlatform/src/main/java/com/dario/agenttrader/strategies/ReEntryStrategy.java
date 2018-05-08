package com.dario.agenttrader.strategies;

import com.dario.agenttrader.domain.*;
import com.dario.agenttrader.actors.MarketActor;
import com.dario.agenttrader.actors.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.GainIndicator;
import org.ta4j.core.indicators.statistics.SimpleLinearRegressionIndicator;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

public class ReEntryStrategy extends AbstractMarketStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ReEntryStrategy.class);

    private Optional<BigDecimal> currentAsk = Optional.empty();
    private Optional<BigDecimal> currentBid = Optional.empty();
    private Direction direction;
    private TimeSeries priceTimeSeries=null;
    private int latestEndIndex = -2;
    private Long latesSpread=Long.MAX_VALUE;
    private long maxAllowedSpread = 25;
    private BigDecimal dealSize = BigDecimal.ONE;
    private int shortPeriod = 12;
    private int longPeriod= 26;
    private int emaDifferenceSafeDistance = 20;
    private int slopeTimeFrame=3;
    private long barMaturityThresholdSeconds =280;
    private BigDecimal absoluteSlopeThreshold = new BigDecimal(1.5);
    private BigDecimal absoluteSlopeChangeThreshold = new BigDecimal(0.60);
    private BigDecimal macdNeutralZone = new BigDecimal(5);
    private BigDecimal macdAccelarionNeutralZone = new BigDecimal(0.15);
    private BigDecimal oppositeStreamSafetyCoeficient = new BigDecimal(3);
    private Direction lastSignalDirection = null;
    private int stopDistanceMultiplier = 50;

    private MarketInfo staticMarketInfo = null;
    private Instant newBarTimeStamp=null;



    public ReEntryStrategy(ArrayList<String> epics,Direction pdirection) {
        super(epics);
        direction = pdirection;
    }

    @Override
    public void evaluate(MarketActor.MarketUpdated marketUpdate) {
        LOG.debug("Check if Update is valid {}, new Bar strted started:{}",marketUpdate.getEpic(),newBarTimeStamp);
        if(isMarketUpdateValid(marketUpdate) ) {//TODO:not sure if it filters updates correctly.Updates from other epics could hit
            LOG.debug("Updating  state",marketUpdate.getEpic());
            updateState(marketUpdate);
            //if(newBarIsAdded()) {
            LOG.debug("Updatinaluating strategy  state",marketUpdate.getEpic());
            if(isAllowedToEvaluateStrategy(marketUpdate)) {
                evaluateStrategy();
            }
            LOG.debug("Strategy evaluated",marketUpdate.getEpic());
            //}
        }
    }

    private void updateState(MarketActor.MarketUpdated marketUpdate) {
        priceTimeSeries = marketUpdate.getMarketupdate().getTimeSeries();
        staticMarketInfo = marketUpdate.getMarketupdate().getMarketInfo();
        latesSpread = calculateClosePriceSpread(marketUpdate);
    }

    private Long calculateClosePriceSpread(MarketActor.MarketUpdated marketUpdate) {
        Long spread = latesSpread;
        Object priceUpdate = marketUpdate.getMarketupdate().getUpdate();
        if (priceUpdate instanceof PriceTick){
            PriceTick priceTick = (PriceTick)priceUpdate;
            spread=priceTick.getBid().subtract(priceTick.getOffer()).abs().longValue();
        }
        return spread;
    }

    private boolean isAllowedToEvaluateStrategy(MarketActor.MarketUpdated marketUpdated){
        if(newBarIsAdded()||newBarTimeStamp==null){
            newBarTimeStamp= Instant.now();
        }

        boolean recievedChartUpdate = false;
        if(marketUpdated.getMarketupdate().getUpdate() instanceof PriceCandle){
            recievedChartUpdate = true;
        }

        long secondsSinceNewBar=Duration.between(newBarTimeStamp,Instant.now()).getSeconds();
        boolean isMinutesPassedOverThreshold = secondsSinceNewBar> barMaturityThresholdSeconds;

        boolean strategyEvaluationConditionsMet = recievedChartUpdate && isMinutesPassedOverThreshold;

        LOG.debug("Strategy evaluation condition met={} - minutes threshold={},recieved chart update={}",strategyEvaluationConditionsMet,isMinutesPassedOverThreshold,recievedChartUpdate);
        return  strategyEvaluationConditionsMet;
    }

    private boolean newBarIsAdded() {
        int endIndex = priceTimeSeries.getEndIndex();
        boolean isNewBarAdded = endIndex > latestEndIndex;
        if(isNewBarAdded){
            latestEndIndex = endIndex;
        }
        return isNewBarAdded;
    }

    //TODO:test Exit market
    //TODO: test gain
    //TODO: move messy rules to proper TA4J rule
    private void evaluateStrategy() {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(priceTimeSeries);
        int endIndex = priceTimeSeries.getEndIndex();
        //
        EMAIndicator shortEMA = new EMAIndicator(closePrice,shortPeriod);
        EMAIndicator longEMA = new EMAIndicator(closePrice,longPeriod);
        MACDIndicator macdIndicator = new MACDIndicator(closePrice,shortPeriod,longPeriod);
        EMAIndicator macdSignal = new EMAIndicator(macdIndicator,9);
        GainIndicator gainSignal = new GainIndicator(closePrice);
        //
        SimpleLinearRegressionIndicator slopeOfMACDIndicator =
                new SimpleLinearRegressionIndicator(
                        macdIndicator
                        ,slopeTimeFrame
                        ,SimpleLinearRegressionIndicator.SimpleLinearRegressionType.slope);
        SimpleLinearRegressionIndicator accelarationOfMACDSLOPEIndicator =
                new SimpleLinearRegressionIndicator(
                        slopeOfMACDIndicator
                        ,slopeTimeFrame
                        ,SimpleLinearRegressionIndicator.SimpleLinearRegressionType.slope);
        //
        Rule buyingRule = new OverIndicatorRule(shortEMA,longEMA)
                .and(new OverIndicatorRule(macdIndicator,macdSignal));
        //
        Rule sellingRule = new UnderIndicatorRule(shortEMA,longEMA)
                .and(new UnderIndicatorRule(macdIndicator,macdSignal));

        Strategy strategy = new BaseStrategy(buyingRule, sellingRule);

        Decimal lastBarsGain = gainSignal.getValue(endIndex);
        Decimal shortEMAValue = shortEMA.getValue(endIndex);
        Decimal longEMAValue = longEMA.getValue(endIndex);
        Decimal macdValue = macdIndicator.getValue(endIndex);
        Decimal macdSignalValue = macdSignal.getValue(endIndex);
        BigDecimal emaDifference = shortEMAValue.minus(longEMAValue).getDelegate().setScale(3,BigDecimal.ROUND_HALF_UP);
        Decimal slopeOfMACD = slopeOfMACDIndicator.getValue(endIndex);
        Decimal accelarationOfMACD = accelarationOfMACDSLOPEIndicator.getValue(endIndex);
        BigDecimal buySafetyCoefficient = (macdValue.isLessThan(macdNeutralZone))?oppositeStreamSafetyCoeficient:BigDecimal.ONE;
        BigDecimal sellSafetyCoefficient = (macdValue.isGreaterThan(macdNeutralZone.negate()))?oppositeStreamSafetyCoeficient:BigDecimal.ONE;
        boolean isLastBarGreen = lastBarsGain.isPositive();
        boolean isCloseSellPosition = accelarationOfMACD.minus(macdAccelarionNeutralZone).isPositive();
        boolean isCloseBuyPosition = accelarationOfMACD.plus(macdAccelarionNeutralZone).isNegative();
        boolean isStrategyShouldEnter = strategy.shouldEnter(endIndex);
        boolean isStrategyShouldExit = strategy.shouldExit(endIndex);
        boolean isSpreadWithinRange = latesSpread < maxAllowedSpread;
        boolean isEMAdifferenceInSafeZone = emaDifference.doubleValue() >emaDifferenceSafeDistance;
        boolean isBuyMACDAccelarationSatisfied = BigDecimal.valueOf(accelarationOfMACD.doubleValue()).compareTo(absoluteSlopeChangeThreshold.multiply(buySafetyCoefficient))>0;
        boolean isSellMACDAccelarationSatisfied = BigDecimal.valueOf(accelarationOfMACD.doubleValue()).compareTo(absoluteSlopeChangeThreshold.negate().multiply(sellSafetyCoefficient))<0;
        boolean isBuyMACDSlopeSatisfied = BigDecimal.valueOf(slopeOfMACD.doubleValue()).compareTo(absoluteSlopeThreshold)>0;
        boolean isSellMACDSlopeSatisfied = BigDecimal.valueOf(slopeOfMACD.doubleValue()).compareTo(absoluteSlopeThreshold.negate())<0;
        LOG.info("EPIC:{},IDX:{},O:{},C:{},H:{},L:{},SPRD:{},SPRD_IN:{},S_EMA:{},L_EMA:{},MACD:{},MACD_S:{},EMA_Diff:{},slope:{},MACD_Slope_B:{},MACD_Slope_S:{},MACD_Acc:{},MACD_ACC_B:{},MACD_ACC_S:{},GAIN:{},CLOSE_BUY:{},CLOSE_SELL:{},ENTER:{},EXIT:{},{}"
                , getEpic()
                ,endIndex
                ,priceTimeSeries.getLastBar().getOpenPrice()
                ,priceTimeSeries.getLastBar().getClosePrice()
                ,priceTimeSeries.getLastBar().getMaxPrice()
                ,priceTimeSeries.getLastBar().getMinPrice()
                ,latesSpread
                ,isSpreadWithinRange
                ,shortEMAValue.getDelegate().setScale(3,BigDecimal.ROUND_HALF_UP)
                ,longEMAValue.getDelegate().setScale(3,BigDecimal.ROUND_HALF_UP)
                ,macdValue.getDelegate().setScale(3,BigDecimal.ROUND_HALF_UP)
                ,macdSignalValue.getDelegate().setScale(3,BigDecimal.ROUND_HALF_UP)
                ,emaDifference
                ,slopeOfMACD.getDelegate().setScale(3,BigDecimal.ROUND_HALF_UP)
                ,isBuyMACDSlopeSatisfied
                ,isSellMACDSlopeSatisfied
                ,accelarationOfMACD.getDelegate().setScale(3,BigDecimal.ROUND_HALF_UP)
                ,isBuyMACDAccelarationSatisfied
                ,isSellMACDAccelarationSatisfied
                ,lastBarsGain.getDelegate().setScale(3,BigDecimal.ROUND_HALF_UP)
                ,isCloseBuyPosition
                ,isCloseSellPosition
                ,isStrategyShouldEnter
                ,isStrategyShouldExit
                ,priceTimeSeries.getLastBar().getSimpleDateName()
        );

        if ( isStrategyShouldEnter
                && isSpreadWithinRange
                //&& isBuyMACDAccelarationSatisfied
                //&& isBuyMACDSlopeSatisfied
                && isLastBarGreen
                ) {
            TradingSignal tradingSignal = createTradingSignal(direction);
            strategyInstructionConsumer.accept(tradingSignal);
            LOG.info("ENTER LONG POSITION SIGNAL:level{},{}",priceTimeSeries.getLastBar().getClosePrice(),tradingSignal);
        } else if ( isStrategyShouldExit
                && isSpreadWithinRange
                //&& isSellMACDAccelarationSatisfied
                //&& isSellMACDSlopeSatisfied
                && !isLastBarGreen
                ) {
            TradingSignal tradingSignal = createTradingSignal(direction.opposite());
            strategyInstructionConsumer.accept(tradingSignal);
            LOG.info("ENTER SHORT POSITION SIGNAL:level{},{}",priceTimeSeries.getLastBar().getClosePrice(),tradingSignal);
        } else if(isCloseBuyPosition){
            TradingSignal tradingSignal = createTradingSignalCloseIfThereIsABuyPositionOpen();
            //strategyInstructionConsumer.accept(tradingSignal);
            LOG.info("CLOSE LONG POSITION SIGNAL:level{},{}",priceTimeSeries.getLastBar().getClosePrice(),tradingSignal);
        } else if(isCloseSellPosition){
            TradingSignal tradingSignal = createTradingSignalCloseIfThereIsASellPositionOpen();
            //strategyInstructionConsumer.accept(tradingSignal);
            LOG.info("CLOSE SHORT POSITION SIGNAL:level:{},{}",priceTimeSeries.getLastBar().getClosePrice(),tradingSignal.toString());
        }

    }

    private TradingSignal createTradingSignalCloseIfThereIsASellPositionOpen() {
        String epic = getEpic();
        LOG.debug("CREATING TRADING SIGNAL FOR {}",epic);

        TradingSignal tradingSignal= TradingSignal.createExitMarketSignal(
                epic
                ,Direction.SELL()
        );
        LOG.debug("TRADING SIGNAL CREATED:level{},{}",priceTimeSeries.getLastBar().getClosePrice(),tradingSignal);
        return tradingSignal;
    }

    private TradingSignal createTradingSignalCloseIfThereIsABuyPositionOpen() {
        String epic = getEpic();
        LOG.debug("CREATING TRADING SIGNAL FOR {}",epic);

        TradingSignal tradingSignal= TradingSignal.createExitMarketSignal(
                epic
                ,Direction.BUY()
                );
        LOG.debug("TRADING SIGNAL CREATED:level{},{}",priceTimeSeries.getLastBar().getClosePrice(),tradingSignal);
        return tradingSignal;
    }

    private TradingSignal createTradingSignal(Direction pDirection) {
        String epic = getEpic();
        LOG.debug("CREATING TRADING SIGNAL FOR {}",epic);

        BigDecimal stopDistance = staticMarketInfo.getMinNormalStopLimitDistance().multiply(
                new BigDecimal(stopDistanceMultiplier));
        BigDecimal size = Optional.ofNullable(dealSize).orElse(staticMarketInfo.getMinDealSize());
        TradingSignal tradingSignal= TradingSignal.createEnterMarketSignal(
                epic
                ,pDirection
                ,size
                ,stopDistance
        );
        LOG.debug("TRADING SIGNAL CREATED:level{},{}",priceTimeSeries.getLastBar().getClosePrice(),tradingSignal);
        return tradingSignal;
    }

    private String getEpic() {
        return getListOfObservedMarkets().get(0);
    }


    @Override
    public void evaluate(Position.PositionUpdate positionUpdate) {
        LOG.debug("Ignoring update for position {}",positionUpdate.getPositionId());
    }

    @Override
    public ArrayList<String> getListOfObservedPositions() {
        ArrayList<String> positionList = new ArrayList<>();
        return positionList;
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


        return  isValid;
    }


    public BigDecimal getCurrentApplicablePrice() {
        BigDecimal currentApplicablePrice;

        if(direction.isBuy()){
            currentApplicablePrice = currentBid.get();
        }else if (direction.isSell()){
            currentApplicablePrice = currentAsk.get();
        }else{
            throw new IllegalStateException("Invalid value for position direction :" + direction);
        }

        return currentApplicablePrice;
    }
}
