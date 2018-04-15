package com.dario.agenttrader.utility;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;

import java.util.HashSet;
import java.util.Set;

public class SubscriberActorRegistery<U> {
    private Set<ActorRef> subscribers;

    public  SubscriberActorRegistery(){
        subscribers = new HashSet<ActorRef>();

    }

    public void registerSubscriber(ActorRef actor, AbstractActor.ActorContext context){
        subscribers.add(actor);
        context.watch(actor);

    }

    public void removeActor(ActorRef actor){
        subscribers.remove(actor);
    }

    public void informSubscriobers(U updateObject,ActorRef sender){
        subscribers.forEach(s -> s.tell(updateObject,sender));
    }

}
