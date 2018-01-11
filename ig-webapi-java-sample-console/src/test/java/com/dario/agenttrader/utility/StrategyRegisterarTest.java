package com.dario.agenttrader.utility;

import akka.actor.*;
import akka.testkit.javadsl.TestKit;

import static org.junit.Assert.assertNotNull;

import com.dario.agenttrader.marketStrategies.MarketStrategySystem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class StrategyRegisterarTest {

    private final String DEAL_ID = "DIAAAABLAADV7A3";
    static ActorSystem system;
    static MarketStrategySystem marketStrategySystem = MarketStrategySystem.getInstance();

    @BeforeClass
    public static void setup() {
        system = marketStrategySystem.getActorSystem();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void registerNewActorTest(){
        ActorRegistery registery = new ActorRegistery();
        AbstractActor.ActorContext mockedContext =
                mock(AbstractActor.ActorContext.class);
        Props props =Props.create(TestActor.class);
        when(mockedContext.actorOf(props,DEAL_ID))
                .thenReturn(system.actorOf(props,DEAL_ID));
        Object msg = new Object();

        ActorRef expectedActor =
                registery.registerActorIfAbscent(mockedContext,props,DEAL_ID,msg);

        ActorRef actualActor = registery.getActorForUniqId(DEAL_ID);

        assertNotNull(actualActor==expectedActor);

    }

    public static final class TestActor extends AbstractActor{

        public static Props props(){
            return Props.create(TestActor.class);
        }

        @Override
        public Receive createReceive() {
            return null;
        }
    }

    //TODO need better test coverage

}
