package com.dario.agenttrader.marketStrategies;


import com.dario.agenttrader.IGClientUtility;
import com.dario.agenttrader.dto.PositionInfo;

import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PositionUpdateTest {
    private final String  OPU_MESSAGE = "{\"stopLevel\":110004,\"trailingStopDistance\":0,\"limitLevel\":null,\"trailingStep\":0,\"guaranteedStop\":false,\"currency\":\"GBP\",\"expiry\":\"DFB\",\"dealIdOrigin\":\"DIAAAABLAADV7A3\",\"dealId\":\"DIAAAABLAADV7A3\",\"dealReference\":\"QC63G1C0266DS3\",\"direction\":\"BUY\",\"epic\":\"UA.D.AMZN.DAILY.IP\",\"dealStatus\":\"ACCEPTED\",\"level\":119255,\"status\":\"UPDATED\",\"size\":2,\"channel\":\"WTP\",\"timestamp\":\"2017-12-30T23:30:00.277\"} ";
    private final String DEAL_ID = "DIAAAABLAADV7A3";


    @Test
    public void testJsonFlatter(){
        Map<String,String> flattenedOPU = IGClientUtility.flatJSontoMap(OPU_MESSAGE);
        assertNotNull(flattenedOPU);
        String dealId = flattenedOPU.get(PositionInfo.DEAL_ID_KEY);
        assertEquals(DEAL_ID,dealId);
    }
}
