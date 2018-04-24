package com.dario.agenttrader.strategies;

import com.dario.agenttrader.domain.Direction;
import com.dario.agenttrader.domain.MarketInfo;
import com.dario.agenttrader.domain.PriceTick;
import com.dario.agenttrader.domain.TradingSignal;
import com.dario.agenttrader.actors.MarketActor;
import com.dario.agenttrader.actors.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
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
    private long barMaturityThresholdSeconds =59;
    private BigDecimal absoluteSlopeThreshold = new BigDecimal(3);
    private BigDecimal absoluteSlopeChangeThreshold = new BigDecimal(0.25);
    private int stopDistanceMultiplier = 3;

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
            if(isAllowedToEvaluateStrategy()) {
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

    private boolean isAllowedToEvaluateStrategy(){
        if(newBarIsAdded()||newBarTimeStamp==null){
            newBarTimeStamp= Instant.now();
        }

        long secondsSinceNewBar=Duration.between(newBarTimeStamp,Instant.now()).getSeconds();
        boolean isMinutesPassedOverThreshold = secondsSinceNewBar> barMaturityThresholdSeconds;
        return  isMinutesPassedOverThreshold;
    }

    private boolean newBarIsAdded() {
        int endIndex = priceTimeSeries.getEndIndex();
        boolean isNewBarAdded = endIndex > latestEndIndex;
        if(isNewBarAdded){
            latestEndIndex = endIndex;
        }
        return isNewBarAdded;
    }

    private void evaluateStrategy() {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(priceTimeSeries);
        int endIndex = priceTimeSeries.getEndIndex();
        //
        EMAIndicator shortEMA = new EMAIndicator(closePrice,shortPeriod);
        EMAIndicator longEMA = new EMAIndicator(closePrice,longPeriod);
        MACDIndicator macdIndicator = new MACDIndicator(closePrice,shortPeriod,longPeriod);
        EMAIndicator macdSignal = new EMAIndicator(macdIndicator,9);
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

        Decimal shortEMAValue = shortEMA.getValue(endIndex);
        Decimal longEMAValue = longEMA.getValue(endIndex);
        Decimal macdValue = macdIndicator.getValue(endIndex);
        Decimal macdSignalValue = macdSignal.getValue(endIndex);
        BigDecimal emaDifference = shortEMAValue.minus(longEMAValue).getDelegate().setScale(3,BigDecimal.ROUND_HALF_UP);
        Decimal slopeOfMACD = slopeOfMACDIndicator.getValue(endIndex);
        Decimal accelarationOfMACD = accelarationOfMACDSLOPEIndicator.getValue(endIndex);
        boolean isStrategyShouldEnter = strategy.shouldEnter(endIndex);
        boolean isStrategyShouldExit = strategy.shouldExit(endIndex);
        boolean isSpreadWithinRange = latesSpread < maxAllowedSpread;
        boolean isEMAdifferenceInSafeZone = emaDifference.doubleValue() >emaDifferenceSafeDistance;
        boolean isBuyMACDAccelarationSatisfied = BigDecimal.valueOf(accelarationOfMACD.doubleValue()).compareTo(absoluteSlopeChangeThreshold)>0;
        boolean isSellMACDAccelarationSatisfied = BigDecimal.valueOf(accelarationOfMACD.doubleValue()).compareTo(absoluteSlopeChangeThreshold.negate())<0;
        boolean isBuyMACDSlopeSatisfied = BigDecimal.valueOf(slopeOfMACD.doubleValue()).compareTo(absoluteSlopeThreshold)>0;
        boolean isSellMACDSlopeSatisfied = BigDecimal.valueOf(slopeOfMACD.doubleValue()).compareTo(absoluteSlopeThreshold.negate())<0;
        LOG.info("EPIC:{},EndIndex:{},price_open:{},close:{},high:{},low:{},spread:{},is_Spread_within_range:{},short_EMA:{},Long_EMA:{},MACD:{},MACDSIGNAL:{},EMA_Diff:{},slope:{},MACD_Slope_Buy:{},MACD_Slope_Sell:{},MACD_Accelaratio:{},MACD_ACCELARATION_Buy:{},MACD_ACCELARATIO_Sell:{},ENTER:{},EXIT:{},{}"
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
                ,isStrategyShouldEnter
                ,isStrategyShouldExit
                ,priceTimeSeries.getLastBar().getSimpleDateName()
        );

        if ( isStrategyShouldEnter && isSpreadWithinRange && isBuyMACDAccelarationSatisfied && isBuyMACDSlopeSatisfied) {
            TradingSignal tradingSignal = createTradingSignal(direction);
            strategyInstructionConsumer.accept(tradingSignal);
            LOG.info("ENTER POSITION SIGNAL:level{},{}",priceTimeSeries.getLastBar().getClosePrice(),tradingSignal);
        } else if ( isStrategyShouldExit && isSpreadWithinRange && isSellMACDAccelarationSatisfied && isSellMACDSlopeSatisfied) {
            TradingSignal tradingSignal = createTradingSignal(direction.opposite());
            strategyInstructionConsumer.accept(tradingSignal);
            LOG.info("EXIT POSITION SIGNAL:level{},{}",priceTimeSeries.getLastBar().getClosePrice(),tradingSignal);
        }

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
        if(! (marketUpdated.getMarketupdate().getUpdate() instanceof PriceTick)){
            isValid=false;
        }

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
