package com.dario.agenttrader.marketStrategies;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.dario.agenttrader.tradingservices.TradingAPI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MarketActorTest {

    private static final String testEpic = "IX.D.HANGSENG.DAILY.IP";

    public static final String AKKA_SYSTEM_USER_URL = "akka://MarketStrategySystem/user/";
    public static final String MARKET_MANAGER_URL = AKKA_SYSTEM_USER_URL
            +MarketStrategySystem.MARKET_MANAGER;

    static ActorSystem system;
    static MarketStrategySystem marketStrategySystem = MarketStrategySystem.getInstance();

    @BeforeClass
    public static void setup() {
        TradingAPI mockedTradingAPI = mock(TradingAPI.class);
        marketStrategySystem.startMarketStrategySystem(mockedTradingAPI);
        system = marketStrategySystem.getActorSystem();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    private ActorRef setupMarketManager(){
        ActorRef marketManager = marketStrategySystem.getMarketManagerActor();
        return  marketManager;
    }
    @Test
    public void testMarketStrategySystemStartup() {
        final ActorRef marketManager = setupMarketManager();
        assertEquals(MARKET_MANAGER_URL, marketManager.path().toString());
    }

    @Test
    public void setupMarketActor(){
        ActorRef marketManager = setupMarketManager();
        final TestKit testProbe = new TestKit(system);
        MarketManager.SubscribeToMarketUpdate subscribeToMarketUpdate =
                new MarketManager.SubscribeToMarketUpdate(testEpic,testProbe.getRef());

        marketManager.tell(subscribeToMarketUpdate,testProbe.getRef());

        testProbe.expectNoMsg();
        marketManager.tell(new MarketManager.ListMarkets(),testProbe.getRef());
        MarketManager.ListOfMarkets listOfMarkets =
                testProbe.expectMsgClass(MarketManager.ListOfMarkets.class);

        assertThat(listOfMarkets.getMarkets(),hasSize(1));
        assertThat(listOfMarkets.getMarkets(),contains(testEpic));

    }



    @Test
    public void testMarketActorStop(){
//        final TestKit testProbe = new TestKit(system);
//        TradingAPI mockedTradingAPI = mock(TradingAPI.class);
//        ActorRef positionManager = system.actorOf(PositionManager.props(mockedTradingAPI));
//
//        ActorRef positionActor = setupPositionActor(testProbe,positionManager);
//        assertNotNull(positionActor);
//
//        testProbe.watch(positionActor);
//        positionActor.tell(new PositionManager.OPU(null,PositionManager.OPU.POSITION_CLOSED),
//                testProbe.getRef());
//
//        testProbe.expectTerminated(positionActor);
//
//        testProbe.awaitAssert(() ->{
//            positionManager.tell(new PositionManager.ListPositions(),testProbe.getRef());
//            PositionManager.ListPositionResponse emptyPositionsList =
//                    testProbe.expectMsgClass(PositionManager.ListPositionResponse.class);
//            assertEquals(Stream.of().collect(Collectors.toSet()),emptyPositionsList.getPositionIDs());
//            return null;
//        });

    }

}
