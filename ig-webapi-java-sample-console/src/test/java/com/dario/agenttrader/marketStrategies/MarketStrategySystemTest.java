package com.dario.agenttrader.marketStrategies;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class  MarketStrategySystemTest{

    public static final String MARKET_STRATEGY_MANAGER_URL = "akka://MarketStrategySystem/user/marketStrategyManager";
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
}
