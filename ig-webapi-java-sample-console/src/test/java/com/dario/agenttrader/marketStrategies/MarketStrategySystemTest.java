package com.dario.agenttrader.marketStrategies;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dario.agenttrader.IGClientUtility;
import com.dario.agenttrader.dto.PositionInfo;
import com.dario.agenttrader.dto.PositionSnapshot;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.PositionsItem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class  MarketStrategySystemTest{

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
        system = marketStrategySystem.getActorSystem();
    //    system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testGreeterActorSendingOfGreeting() {
        final TestKit testProbe = new TestKit(system);
        final ActorRef marketStrategyManager = marketStrategySystem.getMarketStrategyManagerActor();
        final ActorRef positionManager = marketStrategySystem.getPositionManagerActor();

        assertEquals(MARKET_STRATEGY_MANAGER_URL, marketStrategyManager.path().toString());
        assertEquals(POSITION_MANAGER_URL,positionManager.path().toString());
    }




    public PositionManager.OPU createOPU(){
        Map<String,String> flattenedOPU = IGClientUtility.flatJSontoMap(OPU_MESSAGE);
        return new PositionManager.OPU(
        new PositionInfo(flattenedOPU,"",1));
    }

    @Test
    public void testPositionActors(){
        final TestKit testProbePositionManager = new TestKit(system);
        final TestKit testProbePosition = new TestKit(system);

        ActorRef positionManager = system.actorOf(PositionManager.props());
        PositionManager.RegisterPositionRequest registerPositionRequest = createTestRegisterPositionRequest();
        positionManager.tell(registerPositionRequest, testProbePositionManager.getRef());
        testProbePositionManager.expectMsgClass(PositionManager.PositionRegistered.class);
        ActorRef positionActor = testProbePositionManager.getLastSender();
        assertNotNull(positionActor);

        positionActor.tell(createOPU(), testProbePosition.getRef());

        Position.PositionUpdated response = testProbePosition.expectMsgClass(Position.PositionUpdated.class);

        assertNotNull(response);
        assertEquals("Update message positionId does not match",POSITION_ID,response.getPositionId());

        // When position stops itself , it must be removed from Manager
        positionManager.tell(new PositionManager.ListPositions(),testProbePositionManager.getRef());
        PositionManager.ListPositionResponse positionsList =
                testProbePositionManager.expectMsgClass(PositionManager.ListPositionResponse.class);
        assertEquals(Stream.of(POSITION_ID).collect(Collectors.toSet()),positionsList.getPositionIDs());

        testProbePosition.watch(positionActor);
        positionActor.tell(new PositionManager.OPU(null,PositionManager.OPU.POSITION_CLOSED),
                testProbePosition.getRef());
        testProbePosition.expectTerminated(positionActor);

        testProbePosition.awaitAssert(() ->{
            positionManager.tell(new PositionManager.ListPositions(),testProbePositionManager.getRef());
            PositionManager.ListPositionResponse emptyPositionsList =
                    testProbePositionManager.expectMsgClass(PositionManager.ListPositionResponse.class);
            assertEquals(Stream.of().collect(Collectors.toSet()),emptyPositionsList.getPositionIDs());
            return null;
        });
    }

    private PositionManager.RegisterPositionRequest createTestRegisterPositionRequest() {
        com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Position positionV2 =
                new com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Position();
        positionV2.setDealId(DEAL_ID);
        PositionsItem pitem = new PositionsItem();
        pitem.setPosition(positionV2);
        PositionSnapshot pSnap = new PositionSnapshot(pitem);
        PositionManager.RegisterPositionRequest registerPositionRequest =
                new PositionManager.RegisterPositionRequest(POSITION_ID,pSnap);
        return registerPositionRequest;
    }

    @Test
    public void testOnLoadPositionReques(){
        assertTrue("test not implemented",false);
    }
}
