package com.dario.agenttrader.marketStrategies;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;
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

    @Test
    public void testPositionActors(){
        final TestKit testProbePositionManager = new TestKit(system);
        final TestKit testProbePosition = new TestKit(system);

        ActorRef positionManager = system.actorOf(PositionManager.props());
        positionManager.tell(new PositionManager.RegisterPosition(POSITION_ID), testProbePositionManager.getRef());
        testProbePositionManager.expectMsgClass(PositionManager.PositionRegistered.class);
        ActorRef positionActor = testProbePositionManager.getLastSender();
        assertNotNull(positionActor);

        positionActor.tell(new Position.UpdatePosition(), testProbePosition.getRef());

        Position.PositionUpdated response = testProbePosition.expectMsgClass(Position.PositionUpdated.class);

        assertNotNull(response);
        assertEquals("Update message positionId does not match",POSITION_ID,response.getPositionId());

        // When position stops itself , it must be removed from Manager
        positionManager.tell(new PositionManager.ListPositions(),testProbePositionManager.getRef());
        PositionManager.ListPositionResponse positionsList =
                testProbePositionManager.expectMsgClass(PositionManager.ListPositionResponse.class);
        assertEquals(Stream.of(POSITION_ID).collect(Collectors.toSet()),positionsList.getPositionIDs());

        testProbePosition.watch(positionActor);
        positionActor.tell(new Position.UpdatePosition(Position.UpdatePosition.POSITION_CLOSED),
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
}
