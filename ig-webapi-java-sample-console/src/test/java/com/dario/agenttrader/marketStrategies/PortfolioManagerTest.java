package com.dario.agenttrader.marketStrategies;


import static org.mockito.Mockito.*;

import com.dario.agenttrader.dto.PositionSnapshot;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Market;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.PositionsItem;
import org.hamcrest.Matchers;
import org.junit.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import com.dario.agenttrader.tradingservices.TradingAPI;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PortfolioManagerTest {

    private static final String testEpic = "IX.D.HANGSENG.DAILY.IP";

    TradingAPI mockedTradingAPI;
    PortfolioManager portfolioManager;

    @Before
    public  void setup() {
        mockedTradingAPI = mock(TradingAPI.class);
        portfolioManager = new PortfolioManager(mockedTradingAPI);
    }

    @After
    public  void teardown() {
        mockedTradingAPI = null;
        portfolioManager = null;
    }


    @Test
    public void testProcessTradingSignalWhenThereIsAnOpenPositionOnTheMarket() throws Exception {
        List<PositionSnapshot> positionSnapshots =
                createPositionSnapsohtsListWithAPositionOnTestEPIC();
        when(mockedTradingAPI.listOpenPositions()).thenReturn(positionSnapshots);

        BigDecimal size = BigDecimal.ONE;
        BigDecimal stopDistance = BigDecimal.ONE;
        StrategyActor.TradingSignal signal =
                StrategyActor.TradingSignal.createEnterMarketSignal(
                        testEpic,
                        Direction.BUY(),
                        size,
                        stopDistance);

        portfolioManager.processTradingSignal(signal);

        verify(mockedTradingAPI,never()).createPosition(testEpic,Direction.BUY(),size,stopDistance);
    }
    @Test
    public void testProcessTradingSignalWhenThereIsNOOpenPositionOnTheMarket() throws Exception {
        List<PositionSnapshot> positionSnapshots = new ArrayList();
        when(mockedTradingAPI.listOpenPositions()).thenReturn(positionSnapshots);

        BigDecimal size = BigDecimal.ONE;
        BigDecimal stopDistance = BigDecimal.ONE;
        Direction directionBuy = Direction.BUY();
        StrategyActor.TradingSignal signal =
                StrategyActor.TradingSignal.createEnterMarketSignal(
                        testEpic,
                        directionBuy,
                        size,
                        stopDistance);

        portfolioManager.processTradingSignal(signal);

        verify(mockedTradingAPI,times(1)).createPosition(testEpic,directionBuy,size,stopDistance);
    }

    @Test
    public void testProcessTradingSignalToEditPosition() throws Exception{
        String dealId = "1";
        BigDecimal newStopLevel = BigDecimal.ONE;
        BigDecimal newLimitLevel = BigDecimal.ONE;


        StrategyActor.TradingSignal signal = StrategyActor.TradingSignal.createEditPositionSignal(
                dealId
                ,newStopLevel
                ,newLimitLevel
        );
        portfolioManager.processTradingSignal(signal);

        verify(mockedTradingAPI,times(1)).editPosition(dealId,newStopLevel,newLimitLevel);
    }
    private List<PositionSnapshot> createPositionSnapsohtsListWithAPositionOnTestEPIC() {
        Market market = new Market();
        market.setEpic(testEpic);
        PositionsItem positionItem = new PositionsItem();
        positionItem.setMarket(market);
        PositionSnapshot positionSnapshot = new PositionSnapshot(positionItem);
        List<PositionSnapshot> positionSnapShots = new ArrayList();
        positionSnapShots.add(positionSnapshot);
        return  positionSnapShots;
    }

}
