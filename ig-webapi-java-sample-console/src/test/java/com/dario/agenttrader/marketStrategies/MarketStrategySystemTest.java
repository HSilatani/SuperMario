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

public class  MarketStrategySystemTest{

    public static final String MARKET_STRATEGY_MANAGER_URL = "akka://MarketStrategySystem/user/marketStrategyManager";
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

        assertEquals(MARKET_STRATEGY_MANAGER_URL, marketStrategyManager.path().toString());
    }

    @Test
    public void testPoitionActors(){
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
    }
}
