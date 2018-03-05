package com.dario.agenttrader.marketStrategies;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import com.dario.agenttrader.dto.MarketInfo;
import com.dario.agenttrader.dto.PositionSnapshot;
import com.dario.agenttrader.tradingservices.TradingAPI;
import com.iggroup.webapi.samples.client.rest.dto.getAccountsV1.AccountsItem;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.PositionsItem;
import com.iggroup.webapi.samples.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.UpdateInfo;
import org.hamcrest.Matchers;
import org.junit.*;
import scala.concurrent.duration.FiniteDuration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;

public class MarketActorTest {

    private static final String testEpic = "IX.D.HANGSENG.DAILY.IP";

    public static final String AKKA_SYSTEM_USER_URL = "akka://MarketStrategySystem/user/";
    public static final String MARKET_MANAGER_URL = AKKA_SYSTEM_USER_URL
            +MarketStrategySystem.MARKET_MANAGER;

    static ActorSystem system;
    static MarketStrategySystem marketStrategySystem = MarketStrategySystem.getInstance();

    static TradingAPI mockedTradingAPI = new MarketUpdateProducerTradingAPIMock();
    @Before
    public  void setup() {

        marketStrategySystem.startMarketStrategySystem(mockedTradingAPI);
        system = marketStrategySystem.getActorSystem(); 
    }


    @After
    public  void teardown() {
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
    public void testSetupMarketActor(){
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
    public void testTerminateMarketActorWhenThereIsNoSubscribed(){
        ActorRef marketManager = setupMarketManager();
        final TestKit testProbe = new TestKit(system);
        ActorRef testSubscriber = testProbe.getTestActor();
        MarketManager.SubscribeToMarketUpdate subscribeToMarketUpdate =
                new MarketManager.SubscribeToMarketUpdate(testEpic,testSubscriber);
        marketManager.tell(subscribeToMarketUpdate,testSubscriber);

        marketManager.tell(new MarketManager.ListMarkets(),testProbe.getRef());

        MarketManager.ListOfMarkets listOfMarkets=testProbe.expectMsgClass(MarketManager.ListOfMarkets.class);
        assertThat(listOfMarkets.getIdsAndRefs().keySet(), Matchers.contains(testEpic));

        ActorRef testEpicMarketActor = listOfMarkets.getIdsAndRefs().get(testEpic);
        testEpicMarketActor.tell(new MarketActor.ListSubscribers(testEpic),testProbe.getRef());
        MarketActor.SubscribersList subscribersList = testProbe.expectMsgClass(MarketActor.SubscribersList.class);
        assertThat(subscribersList.getSubscribersList(),Matchers.contains(testSubscriber));


        testSubscriber.tell(PoisonPill.getInstance(),null);

        final TestKit testProbe2 = new TestKit(system);
        testEpicMarketActor.tell(new MarketActor.ListSubscribers(testEpic),testProbe2.getRef());
        MarketActor.SubscribersList listOfSubscribers2 = testProbe2.expectMsgClass(MarketActor.SubscribersList.class);
        assertThat(listOfSubscribers2.getSubscribersList(),hasSize(0));
    }

    @Test
    public void testDeliverMarketUpdate(){
        ActorRef marketManager = setupMarketManager();
        final TestKit testProbe = new TestKit(system);
        ActorRef testSubscriber1 = testProbe.getTestActor();
        ActorRef testSubscriber2 = testProbe.getTestActor();

        MarketManager.SubscribeToMarketUpdate subscribeToMarketUpdate =
                new MarketManager.SubscribeToMarketUpdate(testEpic,testSubscriber1);
        marketManager.tell(subscribeToMarketUpdate,testSubscriber1);

        MarketManager.SubscribeToMarketUpdate subscribeToMarketUpdate2 =
                new MarketManager.SubscribeToMarketUpdate(testEpic,testSubscriber2);
        marketManager.tell(subscribeToMarketUpdate2,testSubscriber2);

        marketManager.tell(new MarketManager.ListMarkets(),testProbe.getRef());

        MarketManager.ListOfMarkets listOfMarkets=testProbe.expectMsgClass(MarketManager.ListOfMarkets.class);
        ActorRef testEpicMarketActor = listOfMarkets.getIdsAndRefs().get(testEpic);
        assertNotNull(testEpicMarketActor);
        //
        ActorRef subscriber3 = testProbe.getRef();
        MarketManager.SubscribeToMarketUpdate subscribeToMarketUpdate3 =
                new MarketManager.SubscribeToMarketUpdate(testEpic,subscriber3);
        marketManager.tell(subscribeToMarketUpdate3,subscriber3);
        //
        MarketUpdateProducerTradingAPIMock tradingApi = (MarketUpdateProducerTradingAPIMock) mockedTradingAPI;
        tradingApi.generateChartCandleUpdate();
        //
        //
        MarketActor.MarketUpdated marketUpdateMessage =testProbe.expectMsgClass(
                FiniteDuration.create(10, TimeUnit.SECONDS)
                ,MarketActor.MarketUpdated.class);
        assertThat(marketUpdateMessage,notNullValue());

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
