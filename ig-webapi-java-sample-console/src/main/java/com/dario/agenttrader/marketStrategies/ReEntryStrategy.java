package com.dario.agenttrader.marketStrategies;

import com.dario.agenttrader.dto.MarketInfo;
import com.dario.agenttrader.dto.PriceTick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

public class ReEntryStrategy extends AbstractMarketStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ReEntryStrategy.class);

    private Optional<BigDecimal> currentAsk = Optional.empty();
    private Optional<BigDecimal> currentBid = Optional.empty();
    private Direction direction;
    private TimeSeries priceTimeSeries=null;
    private boolean isBuyingRuleTriggered = false;
    private int latestEndIndex = -2;

    private MarketInfo staticMarketInfo = null;


    public ReEntryStrategy(ArrayList<String> epics,Direction pdirection) {
        super(epics);
        direction = pdirection;
    }

    @Override
    public void evaluate(MarketActor.MarketUpdated marketUpdate) {
        LOG.debug("Received update for {}",marketUpdate.getEpic());
        if(isMarketUpdateValid(marketUpdate)) {
            priceTimeSeries = marketUpdate.getMarketupdate().getTimeSeries();
            staticMarketInfo = marketUpdate.getMarketupdate().getMarketInfo();
            if(newBarIsAdded()) {
                evaluateStrategy();
            }
        }
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
        EMAIndicator shortEMA = new EMAIndicator(closePrice,6);
        EMAIndicator longEMA = new EMAIndicator(closePrice,26);
        MACDIndicator macdIndicator = new MACDIndicator(closePrice);
        EMAIndicator macdSignal = new EMAIndicator(macdIndicator,9);
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

        LOG.info("EPIC:{}, EndIndex:{},isBuyingRuleTriggered:{},short EMA:{} Long EMA:{} MACD:{} MACDSIGNAL:{} at price open:{} ,close:{}, high{},low {}, {}"
                ,getListOfObservedMarkets().get(0)
                ,endIndex
                ,isBuyingRuleTriggered
                ,shortEMAValue
                ,longEMAValue
                ,macdValue
                ,macdSignalValue
                ,priceTimeSeries.getLastBar().getOpenPrice()
                ,priceTimeSeries.getLastBar().getClosePrice()
                ,priceTimeSeries.getLastBar().getMaxPrice()
                ,priceTimeSeries.getLastBar().getMinPrice()
                ,priceTimeSeries.getLastBar().getSimpleDateName()
        );
        if (strategy.shouldEnter(endIndex) && !isBuyingRuleTriggered) {
            String epic = getListOfObservedMarkets().get(0);
            LOG.info("CREATING TRADING SIGNAL FOR {}",epic);
            StrategyActor.TradingSignal tradingSignal= StrategyActor.TradingSignal.createEnterMarketSignal(
                    epic
                    ,direction
                    ,staticMarketInfo.getMinDealSize()
                    ,staticMarketInfo.getMinNormalStopLimitDistance()
            );
            LOG.info("TRADING SIGNAL CREATED:{}",tradingSignal);
            strategyInstructionConsumer.accept(tradingSignal);
            LOG.info("ENTER POSITION SIGNAL");
            isBuyingRuleTriggered = true;
        } else if (strategy.shouldExit(endIndex) && isBuyingRuleTriggered) {
            isBuyingRuleTriggered = false;
            LOG.info("EXIT POSITION SIGNAL");
        }

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
