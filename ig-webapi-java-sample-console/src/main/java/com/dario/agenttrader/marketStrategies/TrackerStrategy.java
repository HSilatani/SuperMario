package com.dario.agenttrader.marketStrategies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackerStrategy extends AbstractMarketStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(TrackerStrategy.class);



    public TrackerStrategy(String[] epics) {
        super(epics);
    }

    @Override
    public void evaluate(MarketActor.MarketUpdated marketUpdate) {
            LOG.info("Received update for {}",marketUpdate.getEpic());
    }
}
