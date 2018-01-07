package com.dario.agenttrader;


import com.dario.agenttrader.dto.PositionSnapshot;
import com.dario.agenttrader.marketStrategies.PositionManager;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Direction;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Market;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Position;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.PositionsItem;

import java.math.BigDecimal;

public class TestPositionProvider {
    public static final String  OPU_MESSAGE = "{\"stopLevel\":110004,\"trailingStopDistance\":0,\"limitLevel\":null,\"trailingStep\":0,\"guaranteedStop\":false,\"currency\":\"GBP\",\"expiry\":\"DFB\",\"dealIdOrigin\":\"DIAAAABLAADV7A3\",\"dealId\":\"DIAAAABLAADV7A3\",\"dealReference\":\"QC63G1C0266DS3\",\"direction\":\"BUY\",\"epic\":\"UA.D.AMZN.DAILY.IP\",\"dealStatus\":\"ACCEPTED\",\"level\":119255,\"status\":\"UPDATED\",\"size\":2,\"channel\":\"WTP\",\"timestamp\":\"2017-12-30T23:30:00.277\"} ";
    public static final String DEAL_ID = "DIAAAABLAADV7A3";

    public static PositionSnapshot getPositionSnapshot() {
        PositionsItem positionsItem = getPositionsItem();
        return new PositionSnapshot(positionsItem);
    }

    public static PositionsItem getPositionsItem() {
        Position position = new Position();
        Market market = new Market();


        position.setContractSize(1f);
        position.setCreatedDate("2017/11/13 08:02:44:000");
        position.setCreatedDateUTC("2017-11-13T08:02:44");
        position.setCurrency("GBP");
        position.setDealId(DEAL_ID);
        position.setControlledRisk(false);
        position.setDirection(Direction.BUY);
        position.setLevel(new BigDecimal(510));
        position.setLimitLevel(new BigDecimal(602));
        position.setSize(new BigDecimal(5));
        position.setStopLevel(new BigDecimal(512));
        position.setTrailingStep(null);
        position.setTrailingStopDistance(null);
        market.setEpic("KA.D.BP.DAILY.IP");
        market.setExpiry("DFB");

        PositionsItem positionsItem = new PositionsItem();
        positionsItem.setPosition(position);
        positionsItem.setMarket(market);
        return positionsItem;
    }


    public static PositionManager.RegisterPositionRequest createTestRegisterPositionRequest() {
        PositionsItem pitem = getPositionsItem();
        PositionSnapshot pSnap = new PositionSnapshot(pitem);
        PositionManager.RegisterPositionRequest registerPositionRequest =
                new PositionManager.RegisterPositionRequest(DEAL_ID,pSnap);
        return registerPositionRequest;
    }
}
