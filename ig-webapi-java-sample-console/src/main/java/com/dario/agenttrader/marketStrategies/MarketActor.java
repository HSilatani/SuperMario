package com.dario.agenttrader.marketStrategies;

import com.dario.agenttrader.dto.MarketUpdate;

public class MarketActor {



    public static final class MarketUpdated{
        private MarketUpdate marketUpdate;
        public MarketUpdate getMarketUpdate() {
            return marketUpdate;
        }
    }
}
