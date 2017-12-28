package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;

public class PositionManager extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private Map<String,ActorRef> positions = new HashMap<String, ActorRef>();


    public PositionManager(){

    }

    public static final Props props(){
        return Props.create(PositionManager.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(RegisterPosition.class, this::onRegiserPosition).build();
    }

    private void onRegiserPosition(RegisterPosition msg) {
        String regPosId = msg.getPositionId();
        ActorRef positionActor = positions.get(regPosId);
        if(null == positionActor ){
            positionActor = getContext().actorOf(Position.props(regPosId));
            positions.put(regPosId,positionActor);
        }
        positionActor.forward(msg,getContext());
    }

    public static final class RegisterPosition{

        private final String positionId;

        RegisterPosition(String ppositionId){
            this.positionId = ppositionId;
        }

        public String getPositionId() {
            return positionId;
        }

    }

    public static final class PositionRegistered{

        private final String positionId;

        PositionRegistered(String ppositionId){
            this.positionId = ppositionId;
        }

        public String getPositionId() {
            return positionId;
        }
    }
}
