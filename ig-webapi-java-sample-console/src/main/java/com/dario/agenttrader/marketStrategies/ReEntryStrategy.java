package com.dario.agenttrader.marketStrategies;

import com.dario.agenttrader.dto.PriceTick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

public class ReEntryStrategy extends AbstractMarketStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ReEntryStrategy.class);

    private Optional<BigDecimal> currentAsk = Optional.empty();
    private Optional<BigDecimal> currentBid = Optional.empty();
    private Direction direction;
    private TimeSeries priceTimeSeries=null;


    public ReEntryStrategy(ArrayList<String> epics,Direction pdirection) {
        super(epics);
        direction = pdirection;
    }

    @Override
    public void evaluate(MarketActor.MarketUpdated marketUpdate) {
        LOG.debug("Received update for {}",marketUpdate.getEpic());
        if(isMarketUpdateValid(marketUpdate)) {
            priceTimeSeries = marketUpdate.getMarketupdate().getTimeSeries();
            evaluateStrategy();
        }
    }

    private void evaluateStrategy() {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(priceTimeSeries);
        // Getting the simple moving average (SMA) of the close price over the last 5 ticks
        SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
        // Getting a longer SMA (e.g. over the 30 last ticks)
        SMAIndicator longSma = new SMAIndicator(closePrice, 30);
        Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma);
        // Selling rules
        // We want to sell:
        //  - if the 5-ticks SMA crosses under 30-ticks SMA
        Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma);

        Strategy strategy = new BaseStrategy(buyingRule, sellingRule);

        int endIndex = priceTimeSeries.getEndIndex();
        if (strategy.shouldEnter(endIndex)) {

            LOG.info("ENTER POSITION SIGNAL for {},short SMA:{} Long SMA {} at price open:{} ,close:{},{}"
                    ,getListOfObservedMarkets().get(0)
                    ,shortSma.getValue(endIndex)
                    ,longSma.getValue(endIndex)
                    ,priceTimeSeries.getLastTick().getOpenPrice()
                    ,priceTimeSeries.getLastTick().getClosePrice()
                    ,priceTimeSeries.getLastTick().getSimpleDateName()
                    );
        } else if (strategy.shouldExit(endIndex)) {
            // Exiting...
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
