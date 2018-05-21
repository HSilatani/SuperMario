package com.dario.agenttrader.strategies;

import com.dario.agenttrader.domain.*;
import com.dario.agenttrader.actors.MarketActor;
import com.dario.agenttrader.actors.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.WilliamsRIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.GainIndicator;
import org.ta4j.core.indicators.statistics.SimpleLinearRegressionIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

public class ReEntryStrategy extends AbstractMarketStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ReEntryStrategy.class);
    private final ReEntryParameters reEntryParameters = ReEntryParameters.fiveMinParameterSet();

    private Optional<BigDecimal> currentAsk = Optional.empty();
    private Optional<BigDecimal> currentBid = Optional.empty();
    private Direction direction;
    private TimeSeries priceTimeSeries=null;
    private int latestEndIndex = -2;
    private Long latesSpread=Long.MAX_VALUE;

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
        boolean isMinutesPassedOverThreshold = secondsSinceNewBar> reEntryParameters.getBarMaturityThresholdSeconds();

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
        EMAIndicator shortEMA = new EMAIndicator(closePrice, reEntryParameters.getShortPeriod());
        EMAIndicator longEMA = new EMAIndicator(closePrice, reEntryParameters.getLongPeriod());
        MACDIndicator macdIndicator = new MACDIndicator(closePrice, reEntryParameters.getShortPeriod(), reEntryParameters.getLongPeriod());
        EMAIndicator macdSignal = new EMAIndicator(macdIndicator,9);
        GainIndicator gainSignal = new GainIndicator(closePrice);
        WilliamsRIndicator williamRIndicator = new WilliamsRIndicator(priceTimeSeries, reEntryParameters.getWilliam_r_timeFrame());
        StandardDeviationIndicator sdIndicator = new StandardDeviationIndicator(closePrice, reEntryParameters.getLongPeriod());

        //
        SimpleLinearRegressionIndicator slopeOfMACDIndicator =
                new SimpleLinearRegressionIndicator(
                        macdIndicator
                        , reEntryParameters.getSlopeTimeFrame()
                        ,SimpleLinearRegressionIndicator.SimpleLinearRegressionType.slope);
        SimpleLinearRegressionIndicator accelarationOfMACDSLOPEIndicator =
                new SimpleLinearRegressionIndicator(
                        slopeOfMACDIndicator
                        , reEntryParameters.getSlopeTimeFrame()
                        ,SimpleLinearRegressionIndicator.SimpleLinearRegressionType.slope);
        //
        Rule buyingRule = new OverIndicatorRule(shortEMA,longEMA)
                .and(new OverIndicatorRule(macdIndicator,macdSignal));
        //
        Rule sellingRule = new UnderIndicatorRule(shortEMA,longEMA)
                .and(new UnderIndicatorRule(macdIndicator,macdSignal));

        Strategy strategy = new BaseStrategy(buyingRule, sellingRule);

        Decimal sd = sdIndicator.getValue(endIndex);
        Decimal williamr = williamRIndicator.getValue(endIndex);
        Decimal lastBarsGain = gainSignal.getValue(endIndex);
        Decimal shortEMAValue = shortEMA.getValue(endIndex);
        Decimal longEMAValue = longEMA.getValue(endIndex);
        Decimal macdValue = macdIndicator.getValue(endIndex);
        Decimal macdSignalValue = macdSignal.getValue(endIndex);
        BigDecimal emaDifference = shortEMAValue.minus(longEMAValue).getDelegate().setScale(3,BigDecimal.ROUND_HALF_UP);
        Decimal slopeOfMACD = slopeOfMACDIndicator.getValue(endIndex);
        Decimal accelarationOfMACD = accelarationOfMACDSLOPEIndicator.getValue(endIndex);
        BigDecimal buySafetyCoefficient = (macdValue.isLessThan(reEntryParameters.getMacdNeutralZone()))? reEntryParameters.getOppositeStreamSafetyCoeficient() :BigDecimal.ONE;
        BigDecimal sellSafetyCoefficient = (macdValue.isGreaterThan(reEntryParameters.getMacdNeutralZone().negate()))? reEntryParameters.getOppositeStreamSafetyCoeficient() :BigDecimal.ONE;
        boolean isSDInRange = sd.isGreaterThanOrEqual(reEntryParameters.getMinStandardDeviationThreshold());
        boolean isLastBarGreen = lastBarsGain.isPositive();
        boolean isLastBarRed = !isLastBarGreen;
        boolean isCloseSellPosition = accelarationOfMACD.minus(reEntryParameters.getMacdAccelarionNeutralZone()).isPositive();
        boolean isCloseBuyPosition = accelarationOfMACD.plus(reEntryParameters.getMacdAccelarionNeutralZone()).isNegative();
        boolean isStrategyShouldEnter = strategy.shouldEnter(endIndex);
        boolean isStrategyShouldExit = strategy.shouldExit(endIndex);
        boolean isSpreadWithinRange = latesSpread < reEntryParameters.getMaxAllowedSpread();
        boolean isEMAdifferenceInSafeZone = emaDifference.doubleValue() > reEntryParameters.getEmaDifferenceSafeDistance();
        boolean isBuyMACDAccelarationSatisfied = BigDecimal.valueOf(accelarationOfMACD.doubleValue()).compareTo(reEntryParameters.getAbsoluteSlopeChangeThreshold().multiply(buySafetyCoefficient))>0;
        boolean isSellMACDAccelarationSatisfied = BigDecimal.valueOf(accelarationOfMACD.doubleValue()).compareTo(reEntryParameters.getAbsoluteSlopeChangeThreshold().negate().multiply(sellSafetyCoefficient))<0;
        boolean isBuyMACDAccVeryStrong = BigDecimal.valueOf(accelarationOfMACD.doubleValue()).compareTo(reEntryParameters.getAbsoluteStrongSlopeChange().multiply(buySafetyCoefficient))>0;
        boolean isSellMACDAccVeryStrong = BigDecimal.valueOf(accelarationOfMACD.doubleValue()).compareTo(reEntryParameters.getAbsoluteStrongSlopeChange().negate().multiply(sellSafetyCoefficient))<0;
        boolean william_r_no_buy_flag = williamr.isGreaterThanOrEqual(reEntryParameters.getWilliam_r_no_buy()) && !isBuyMACDAccVeryStrong;
        boolean william_r_no_sell_flag = williamr.isLessThanOrEqual(reEntryParameters.getWilliam_r_no_sell()) && !isSellMACDAccVeryStrong;
        boolean isBuyMACDSlopeSatisfied = BigDecimal.valueOf(slopeOfMACD.doubleValue()).compareTo(reEntryParameters.getAbsoluteSlopeThreshold())>0;
        boolean isSellMACDSlopeSatisfied = BigDecimal.valueOf(slopeOfMACD.doubleValue()).compareTo(reEntryParameters.getAbsoluteSlopeThreshold().negate())<0;
        LOG.info("EPIC:{},IDX:{},O:{},C:{},H:{},L:{},SP:{},SP_IN:{},S_EMA:{},L_EMA:{},EMA_D:{},MACD:{},MACD_S:{},WR:{},WR_NB:{},WR_NS:{},SD:{},SD_IN:{},SL:{},MACD_SL_B:{},MACD_SL_S:{},MACD_AC:{},MACD_AC_B:{},MACD_AC_S:{},GN:{},C_BUY:{},C_SELL:{},LNG:{},SHRT:{},{}"
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
                ,emaDifference
                ,macdValue.getDelegate().setScale(3,BigDecimal.ROUND_HALF_UP)
                ,macdSignalValue.getDelegate().setScale(3,BigDecimal.ROUND_HALF_UP)
                ,williamr.getDelegate().setScale(2,BigDecimal.ROUND_HALF_UP)
                ,william_r_no_buy_flag
                ,william_r_no_sell_flag
                ,sd.getDelegate().setScale(3,BigDecimal.ROUND_HALF_UP)
                ,isSDInRange
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
                && isSDInRange
                && isBuyMACDAccelarationSatisfied
                && isBuyMACDSlopeSatisfied
                && isLastBarGreen
                && !william_r_no_buy_flag
                ) {
            TradingSignal tradingSignal = createTradingSignal(direction);
            strategyInstructionConsumer.accept(tradingSignal);
            LOG.info("ENTER LONG POSITION SIGNAL:level{},{}",priceTimeSeries.getLastBar().getClosePrice(),tradingSignal);
        } else if ( isStrategyShouldExit
                && isSDInRange
                && isSpreadWithinRange
                && isSellMACDAccelarationSatisfied
                && isSellMACDSlopeSatisfied
                && isLastBarRed
                && !william_r_no_sell_flag
                ) {
            TradingSignal tradingSignal = createTradingSignal(direction.opposite());
            strategyInstructionConsumer.accept(tradingSignal);
            LOG.info("ENTER SHORT POSITION SIGNAL:level{},{}",priceTimeSeries.getLastBar().getClosePrice(),tradingSignal);
        } else if( isStrategyShouldExit
                && isSDInRange
                && isSpreadWithinRange
                && isLastBarRed
                ){
            TradingSignal tradingSignal = createTradingSignalCloseIfThereIsABuyPositionOpen();
            strategyInstructionConsumer.accept(tradingSignal);
            LOG.info("CLOSE LONG POSITION SIGNAL:level{},{}",priceTimeSeries.getLastBar().getClosePrice(),tradingSignal);
        } else if(
                isSDInRange
                && isStrategyShouldEnter
                && isSpreadWithinRange
                && isLastBarGreen
                ) {
            TradingSignal tradingSignal = createTradingSignalCloseIfThereIsASellPositionOpen();
            strategyInstructionConsumer.accept(tradingSignal);
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
                new BigDecimal(reEntryParameters.getStopDistanceMultiplier()));
        BigDecimal size = Optional.ofNullable(reEntryParameters.getDealSize()).orElse(staticMarketInfo.getMinDealSize());
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

    public static class ReEntryParameters {
        private long maxAllowedSpread;
        private BigDecimal dealSize;
        private int shortPeriod;
        private int longPeriod;
        private int emaDifferenceSafeDistance;
        private int slopeTimeFrame;
        private long barMaturityThresholdSeconds;
        private int william_r_timeFrame;
        private BigDecimal absoluteSlopeThreshold;
        private BigDecimal absoluteSlopeChangeThreshold;
        private BigDecimal absoluteStrongSlopeChange;
        private BigDecimal macdNeutralZone;
        private BigDecimal macdAccelarionNeutralZone;
        private BigDecimal oppositeStreamSafetyCoeficient;
        private int stopDistanceMultiplier;
        private BigDecimal william_r_no_buy;
        private BigDecimal william_r_no_sell;
        private BigDecimal minStandardDeviationThreshold;

        public static ReEntryParameters fiveMinParameterSet(){
            ReEntryParameters fiveMinParameters = new ReEntryParameters();
            fiveMinParameters.maxAllowedSpread = 25;
            fiveMinParameters.dealSize=BigDecimal.ONE;
            fiveMinParameters.shortPeriod = 12;
            fiveMinParameters.longPeriod = 26;
            fiveMinParameters.emaDifferenceSafeDistance = 20;
            fiveMinParameters.slopeTimeFrame = 2;
            fiveMinParameters.barMaturityThresholdSeconds = 280;
            fiveMinParameters.william_r_timeFrame = 14;
            fiveMinParameters.absoluteSlopeThreshold = new BigDecimal(0.25);
            fiveMinParameters.absoluteSlopeChangeThreshold = new BigDecimal(0.60);
            fiveMinParameters.absoluteStrongSlopeChange = new BigDecimal(1.5);
            fiveMinParameters.macdNeutralZone = new BigDecimal(5);
            fiveMinParameters.macdAccelarionNeutralZone = new BigDecimal(0.15);
            fiveMinParameters.oppositeStreamSafetyCoeficient = new BigDecimal(2);
            fiveMinParameters.stopDistanceMultiplier = 50;
            fiveMinParameters.william_r_no_buy = new BigDecimal(-20);
            fiveMinParameters.william_r_no_sell = new BigDecimal(-79);
            fiveMinParameters.minStandardDeviationThreshold = new BigDecimal(18.5);

            return fiveMinParameters;
        }
       private ReEntryParameters(){

       }

        public long getMaxAllowedSpread() {
            return maxAllowedSpread;
        }
        public BigDecimal getDealSize() {
            return dealSize;
        }
        public int getShortPeriod() {
            return shortPeriod;
        }
        public int getLongPeriod() {
            return longPeriod;
        }
        public int getEmaDifferenceSafeDistance() {
            return emaDifferenceSafeDistance;
        }
        public int getSlopeTimeFrame() {
            return slopeTimeFrame;
        }
        public long getBarMaturityThresholdSeconds() {
            return barMaturityThresholdSeconds;
        }
        public int getWilliam_r_timeFrame() {
            return william_r_timeFrame;
        }
        public BigDecimal getAbsoluteSlopeThreshold() {
            return absoluteSlopeThreshold;
        }
        public BigDecimal getAbsoluteSlopeChangeThreshold() {
            return absoluteSlopeChangeThreshold;
        }
        public BigDecimal getAbsoluteStrongSlopeChange() {
            return absoluteStrongSlopeChange;
        }
        public BigDecimal getMacdNeutralZone() {
            return macdNeutralZone;
        }
        public BigDecimal getMacdAccelarionNeutralZone() {
            return macdAccelarionNeutralZone;
        }
        public BigDecimal getOppositeStreamSafetyCoeficient() {
            return oppositeStreamSafetyCoeficient;
        }
        public int getStopDistanceMultiplier() {
            return stopDistanceMultiplier;
        }
        public BigDecimal getWilliam_r_no_buy() {
            return william_r_no_buy;
        }
        public BigDecimal getWilliam_r_no_sell() {
            return william_r_no_sell;
        }
        public BigDecimal getMinStandardDeviationThreshold() {
            return minStandardDeviationThreshold;
        }

    }
}
