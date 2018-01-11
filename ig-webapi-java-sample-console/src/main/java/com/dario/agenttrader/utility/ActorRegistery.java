package com.dario.agenttrader.utility;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ActorRegistery {
    private Map<String,ActorRef> stringIdToActor = new HashMap<String,ActorRef>();
    private Map<ActorRef,String> actorToStringId = new HashMap<ActorRef, String>();



    public ActorRef getActorForUniqId(String uniqId) {
        ActorRef actor = stringIdToActor.get(uniqId);
        return actor;
    }

    private void addNewActor(ActorRef actor, String uniqId) {
        stringIdToActor.put(uniqId,actor);
        actorToStringId.put(actor,uniqId);
    }

    public String removeActor(ActorRef actor) {

        String uniqId = actorToStringId.get(actor);
        stringIdToActor.remove(uniqId);
        actorToStringId.remove(actor);
        return  uniqId;
    }

    public ActorRef registerActorIfAbscent(AbstractActor.ActorContext context
                                        , Props props
                                        , String uniqId
                                        , Object msg){
        ActorRef actor = this.getActorForUniqId(uniqId);

        if (actor == null) {
            actor = context.actorOf(props,uniqId);
            context.watch(actor);
            addNewActor(actor, uniqId);
            actor.forward(msg, context);
        }

        return actor;
    }

    public Set<String> getUniqIds() {
        return stringIdToActor.keySet();
    }
}
