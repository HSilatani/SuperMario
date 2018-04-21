package com.dario.agenttrader.marketStrategies;


import static org.mockito.Mockito.*;

import com.dario.agenttrader.domain.Direction;
import com.dario.agenttrader.domain.PositionSnapshot;
import com.dario.agenttrader.domain.TradingSignal;
import com.dario.agenttrader.tradingservices.TradingDataStreamingService;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Market;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Position;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.PositionsItem;
import org.junit.*;

import com.dario.agenttrader.tradingservices.TradingAPI;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
        TradingDataStreamingService.getInstance().initializeStreamingService(mockedTradingAPI);
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
        TradingSignal signal =
                TradingSignal.createEnterMarketSignal(
                        testEpic,
                        Direction.SELL(),
                        size,
                        stopDistance);

        portfolioManager.processTradingSignal(signal);

        verify(mockedTradingAPI,never()).createPosition(signal);
    }
    @Test
    public void testProcessTradingSignalWhenThereIsAnOpenPositionOnOppositeDirection() throws Exception {
        List<PositionSnapshot> positionSnapshots =
                createPositionSnapsohtsListWithAPositionOnTestEPIC();
        when(mockedTradingAPI.listOpenPositions()).thenReturn(positionSnapshots);

        BigDecimal size = BigDecimal.ONE;
        BigDecimal stopDistance = BigDecimal.ONE;
        TradingSignal signal =
                TradingSignal.createEnterMarketSignal(
                        testEpic,
                        Direction.BUY(),
                        size,
                        stopDistance);

        portfolioManager.processTradingSignal(signal);
        verify(mockedTradingAPI,never()).createPosition(signal);

        portfolioManager.processTradingSignal(signal);

        verify(mockedTradingAPI, Mockito.times(1)).createPosition(signal);;
        verify(mockedTradingAPI, Mockito.times(1)).closeOpenPosition(any());
        verify(mockedTradingAPI, Mockito.times(1)).listOpenPositions();
    }
    @Test
    public void testProcessTradingSignalWhenThereIsNOOpenPositionOnTheMarket() throws Exception {
        List<PositionSnapshot> positionSnapshots = new ArrayList();
        when(mockedTradingAPI.listOpenPositionsWithProfitAndLoss()).thenReturn(positionSnapshots);
        when(mockedTradingAPI.createPosition(any(TradingSignal.class))).thenReturn(createDummyMarioPosition());
        BigDecimal size = BigDecimal.ONE;
        BigDecimal stopDistance = BigDecimal.ONE;
        Direction directionBuy = Direction.BUY();
        TradingSignal signal =
                TradingSignal.createEnterMarketSignal(
                        testEpic,
                        directionBuy,
                        size,
                        stopDistance);

        portfolioManager.processTradingSignal(signal);

        verify(mockedTradingAPI,times(1)).createPosition(signal);
    }

    private com.dario.agenttrader.domain.Position createDummyMarioPosition() {
            long positionAgeInMilis=10;
            String dealId="dealID";
            String epic="IX.HAN";
            String dealRef="REF1";
            String createdDateTime =
                    Instant.now().minusMillis(positionAgeInMilis).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);;
            double size = 1.5;
            Direction direction = Direction.BUY();
            return new com.dario.agenttrader.domain.Position(epic,dealRef,size,direction,createdDateTime);
    }

    @Test
    public void testProcessTradingSignalToEditPosition() throws Exception{
        String dealId = "1";
        BigDecimal newStopLevel = BigDecimal.ONE;
        BigDecimal newLimitLevel = BigDecimal.ONE;


        TradingSignal signal = TradingSignal.createEditPositionSignal(
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
        Position position = createDummySellPosition();
        positionItem.setPosition(position);
        PositionSnapshot positionSnapshot = new PositionSnapshot(positionItem);
        List<PositionSnapshot> positionSnapShots = new ArrayList();
        positionSnapShots.add(positionSnapshot);
        return  positionSnapShots;
    }

    private Position createDummySellPosition() {
        Position position =
                new Position();
        position.setDealId("DEAL_ID");
        position.setDealReference("DEAL_REF");
        position.setDirection(com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Direction.SELL);
        position.setSize(BigDecimal.ONE);
        position.setCreatedDateUTC(Instant.now().atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return position;
    }

}
