package com.dario.agenttrader.utility;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.*;
import java.util.function.Consumer;

public class ActorRegistery {
    private Map<String,ActorRef> stringIdToActor = new HashMap<String,ActorRef>();
    private Map<ActorRef,String> actorToStringId = new HashMap<ActorRef, String>();
    private Consumer actorAddRemoveHook = null;



    public ActorRef getActorForUniqId(String uniqId) {
        ActorRef actor = stringIdToActor.get(uniqId);
        return actor;
    }

    private void addNewActor(ActorRef actor, String uniqId) {
        stringIdToActor.put(uniqId,actor);
        actorToStringId.put(actor,uniqId);
        updateAddRemoveListner();
    }

    public String removeActor(ActorRef actor) {

        String uniqId = actorToStringId.get(actor);
        stringIdToActor.remove(uniqId);
        actorToStringId.remove(actor);
        updateAddRemoveListner();
        return  uniqId;
    }

    private void updateAddRemoveListner(){
        Optional.ofNullable(actorAddRemoveHook).ifPresent(actorAddRemoveHook);
    }
    public void actorAddRemoveHook(Consumer consumer){
        actorAddRemoveHook=consumer;
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

    public Map<String,ActorRef> getRegistryuniqIdsAndRefs(){
        return Collections.unmodifiableMap(stringIdToActor);
    }

    public Set<String> getUniqIds() {
        return stringIdToActor.keySet();
    }

    public void removeActorById(String uniqId) {
        removeActor(getActorForUniqId(uniqId));
    }
}
