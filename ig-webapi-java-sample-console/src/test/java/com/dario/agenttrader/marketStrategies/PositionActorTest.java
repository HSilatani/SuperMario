package com.dario.agenttrader.marketStrategies;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.dario.agenttrader.TestPositionProvider;
import com.dario.agenttrader.dto.UpdateEvent;
import com.dario.agenttrader.tradingservices.TradingAPI;
import com.dario.agenttrader.utility.IGClientUtility;
import com.dario.agenttrader.dto.PositionInfo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PositionActorTest {

    public static final String AKKA_SYSTEM_USER_URL = "akka://MarketStrategySystem/user/";
    public static final String MARKET_STRATEGY_MANAGER_URL = AKKA_SYSTEM_USER_URL
            +MarketStrategySystem.MARKET_STRATEGY_MANAGER;
    public static final String POSITION_MANAGER_URL =  AKKA_SYSTEM_USER_URL + MarketStrategySystem.POSITION_MANAGER;

    public static final String POSITION_ID = "DIA111";
    static ActorSystem system;
    static MarketStrategySystem marketStrategySystem = MarketStrategySystem.getInstance();


    private final String  OPU_MESSAGE = "{\"stopLevel\":110004,\"trailingStopDistance\":0,\"limitLevel\":null,\"trailingStep\":0,\"guaranteedStop\":false,\"currency\":\"GBP\",\"expiry\":\"DFB\",\"dealIdOrigin\":\"DIAAAABLAADV7A3\",\"dealId\":\"DIAAAABLAADV7A3\",\"dealReference\":\"QC63G1C0266DS3\",\"direction\":\"BUY\",\"epic\":\"UA.D.AMZN.DAILY.IP\",\"dealStatus\":\"ACCEPTED\",\"level\":119255,\"status\":\"UPDATED\",\"size\":2,\"channel\":\"WTP\",\"timestamp\":\"2017-12-30T23:30:00.277\"} ";
    private final String DEAL_ID = "DIAAAABLAADV7A3";

    @BeforeClass
    public static void setup() {
        TradingAPI mockedTradingAPI = mock(TradingAPI.class);
        marketStrategySystem.startMarketStrategySystem(mockedTradingAPI);
        system = marketStrategySystem.getActorSystem();
    //    system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testMarketStrategySystemStartup() {
        final TestKit testProbe = new TestKit(system);
        final ActorRef marketStrategyManager = marketStrategySystem.getStrategyManagerActor();
        final ActorRef positionManager = marketStrategySystem.getPositionManagerActor();

        assertEquals(MARKET_STRATEGY_MANAGER_URL, marketStrategyManager.path().toString());
        assertEquals(POSITION_MANAGER_URL,positionManager.path().toString());
    }




    public PositionManager.OPU createOPU(){
        Map<String,String> flattenedOPU = IGClientUtility.flatJSontoMap(OPU_MESSAGE);
        UpdateEvent positionUpdateEvent = new UpdateEvent(flattenedOPU,UpdateEvent.POSITION_UPDATE);
        return new PositionManager.OPU(
        new PositionInfo(positionUpdateEvent,"",1));
    }

    private ActorRef setupPositionActor(TestKit testKit,ActorRef positionManager){
         PositionManager.RegisterPositionRequest registerPositionRequest =
                TestPositionProvider.createTestRegisterPositionRequest();

        positionManager.tell(registerPositionRequest, testKit.getRef());

        testKit.expectMsgClass(PositionManager.PositionRegistered.class);
        ActorRef positionActor = testKit.getLastSender();

        return positionActor;
    }

    @Test
    public void testOnPositionUpdate(){
        final TestKit testProbePositionManagerProbe = new TestKit(system);
        TradingAPI mockedTradingAPI = mock(TradingAPI.class);
        ActorRef positionManager = system.actorOf(PositionManager.props(mockedTradingAPI));

        ActorRef positionActor = setupPositionActor(testProbePositionManagerProbe,positionManager);
        assertNotNull(positionActor);

        positionActor.tell(createOPU(), testProbePositionManagerProbe.getRef());

        Position.PositionUpdatedDelta response = testProbePositionManagerProbe.expectMsgClass(Position.PositionUpdatedDelta.class);

        assertNotNull(response);
        assertEquals("Update message positionId does not match",TestPositionProvider.DEAL_ID,response.getPositionId());

    }

    @Test
    public void testPositionActorStop(){
        final TestKit testProbe = new TestKit(system);
        TradingAPI mockedTradingAPI = mock(TradingAPI.class);
        ActorRef positionManager = system.actorOf(PositionManager.props(mockedTradingAPI));

        ActorRef positionActor = setupPositionActor(testProbe,positionManager);
        assertNotNull(positionActor);

        testProbe.watch(positionActor);
        positionActor.tell(new PositionManager.OPU(null,PositionManager.OPU.POSITION_CLOSED),
                testProbe.getRef());

        testProbe.expectTerminated(positionActor);

        testProbe.awaitAssert(() ->{
            positionManager.tell(new PositionManager.ListPositions(),testProbe.getRef());
            PositionManager.ListPositionResponse emptyPositionsList =
                    testProbe.expectMsgClass(PositionManager.ListPositionResponse.class);
            assertEquals(Stream.of().collect(Collectors.toSet()),emptyPositionsList.getPositionIDs());
            return null;
        });

    }

    @Test
    public void testPositionCreate(){

        final TestKit testProbe = new TestKit(system);
        TradingAPI mockedTradingAPI = mock(TradingAPI.class);
        ActorRef positionManager = system.actorOf(PositionManager.props(mockedTradingAPI));

        ActorRef positionActor = setupPositionActor(testProbe,positionManager);
        assertNotNull(positionActor);

        positionManager.tell(new PositionManager.ListPositions(),testProbe.getRef());
        PositionManager.ListPositionResponse positionsList =
                    testProbe.expectMsgClass(PositionManager.ListPositionResponse.class);
        assertEquals(Stream.of(TestPositionProvider.DEAL_ID).collect(Collectors.toSet()),
                positionsList.getPositionIDs());
    }

}
