package com.dario.agenttrader.domain;


import com.dario.agenttrader.utility.IGClientUtility;
import org.hamcrest.Matchers;
import org.junit.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class PriceTickTest {

    @Test
    public void testMarketStrategySystemStartup() {
        String updateInforWithChange = "[ 30420, 30428, (null), null, 1527780865075, (30509), (-0.28), (30636), (30357) ]";
        String updateInfoWithoutChange = "[ (null), (null), (null), (1), 1527780867258, (null), (null), (null), (null) ]";

        PriceTick lastTick = IGClientUtility.extractMarketPriceTick(updateInforWithChange);
        PriceTick newTick = IGClientUtility.extractMarketPriceTick(updateInfoWithoutChange);

        newTick.mergeWithSnapshot(lastTick);

        assertThat(newTick.getBid(),Matchers.equalTo(lastTick.getBid()));
        assertThat(newTick.getOffer(),Matchers.equalTo(lastTick.getOffer()));
        assertThat(newTick.getUtm(),Matchers.equalTo("1527780867258"));
        assertThat(lastTick.getUtm(),Matchers.equalTo("1527780865075"));
    }


}
