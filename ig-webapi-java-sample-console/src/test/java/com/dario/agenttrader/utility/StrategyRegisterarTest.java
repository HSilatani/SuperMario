package com.dario.agenttrader.utility;

import akka.actor.*;
import akka.testkit.javadsl.TestKit;

import static org.junit.Assert.assertNotNull;

import com.dario.agenttrader.marketStrategies.MarketStrategySystem;
import com.dario.agenttrader.tradingservices.TradingAPI;
import org.junit.*;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class StrategyRegisterarTest {


    public static final String ACTOR_SYSTEM_NAME = "PositionActorTest";
    public static final String AKKA_SYSTEM_USER_URL = "akka://"+ACTOR_SYSTEM_NAME+"/user/";

    private final String DEAL_ID = "DIAAAABLAADV7A3";
    static ActorSystem system;
    static MarketStrategySystem marketStrategySystem = MarketStrategySystem.getInstance();

    @Before
    public  void setup() {
        TradingAPI mockedTradingAPI = mock(TradingAPI.class);
        marketStrategySystem.startMarketStrategySystem(mockedTradingAPI,ACTOR_SYSTEM_NAME);
        system = marketStrategySystem.getActorSystem();
    //    system = ActorSystem.create();
    }

    @After
    public  void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }


    @Test
    public void registerNewActorTest(){
        String dealId = getRandomDealId();
        ActorRegistery registery = new ActorRegistery();
        AbstractActor.ActorContext mockedContext =
                mock(AbstractActor.ActorContext.class);
        Props props =Props.create(TestActor.class);
        when(mockedContext.actorOf(props,dealId))
                .thenReturn(system.actorOf(props,dealId));
        Object msg = new Object();

        ActorRef expectedActor =
                registery.registerActorIfAbscent(mockedContext,props,dealId,msg);

        ActorRef actualActor = registery.getActorForUniqId(dealId);

        assertNotNull(actualActor==expectedActor);

    }

    public String getRandomDealId() {
        return DEAL_ID + Math.round(Math.random()*1000);
    }

    @Test
    public void removeActor(){
       String dealId = getRandomDealId();
       ActorRegistery registery = new ActorRegistery();
        AbstractActor.ActorContext mockedContext =
                mock(AbstractActor.ActorContext.class);
        Props props =Props.create(TestActor.class);
        when(mockedContext.actorOf(props,dealId))
                .thenReturn(system.actorOf(props,dealId));

        Object msg = new Object();

        ActorRef actor =
                registery.registerActorIfAbscent(mockedContext,props,dealId,msg);

        registery.removeActor(actor);

        ActorRef actualActor = registery.getActorForUniqId(dealId);
        assertTrue(null == actualActor);

    }

    @Test
    public void removeActorById(){
        String dealId = getRandomDealId();
       ActorRegistery registery = new ActorRegistery();
        AbstractActor.ActorContext mockedContext =
                mock(AbstractActor.ActorContext.class);
        Props props =Props.create(TestActor.class);
        when(mockedContext.actorOf(props,dealId))
                .thenReturn(system.actorOf(props,dealId));

        Object msg = new Object();

        ActorRef actor =
                registery.registerActorIfAbscent(mockedContext,props,dealId,msg);

        registery.removeActorById(dealId);

        ActorRef actualActor = registery.getActorForUniqId(dealId);

        assertTrue(null == actualActor);

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
