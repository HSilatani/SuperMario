package com.dario.agenttrader.marketStrategies;

import com.dario.agenttrader.dto.DealConfirmation;
import com.dario.agenttrader.dto.Position;
import com.dario.agenttrader.dto.PositionSnapshot;
import com.dario.agenttrader.dto.TradingSignal;
import com.dario.agenttrader.utility.IGClientUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PortfolioPositionTracker{
    private final static Logger LOG = LoggerFactory.getLogger(PortfolioPositionTracker.class);


    private volatile Map<String,Map<String, com.dario.agenttrader.dto.Position>> epicToPositions = new ConcurrentHashMap<String, Map<String, com.dario.agenttrader.dto.Position>>();
    private volatile Map<String, com.dario.agenttrader.dto.Position>  dealRefToPosition = new ConcurrentHashMap<String, com.dario.agenttrader.dto.Position>();
    private long confirmExpiryTimeOutMili = 2000;
    private long positionReconciliationTimeOutMili = 30000;
    private Instant lastPositionReconciliationTime = null;
    private Supplier<List<PositionSnapshot>> positionReloadSupplier;
    private Function<String,DealConfirmation> dealConfirmationFunction;

    public void trackNewPosition(com.dario.agenttrader.dto.Position position){
        Map<String, com.dario.agenttrader.dto.Position> positionsOnEpic = epicToPositions.get(position.getEpic());
        positionsOnEpic = Optional.ofNullable(positionsOnEpic).orElse(new ConcurrentHashMap<String, com.dario.agenttrader.dto.Position>());
        positionsOnEpic.put(position.getDealRef(),position);
        registerPosition(position, positionsOnEpic);
    }

    private  void registerPosition(com.dario.agenttrader.dto.Position position, Map<String, com.dario.agenttrader.dto.Position> positionsOnEpic) {
        epicToPositions.put(position.getEpic(),positionsOnEpic);
        dealRefToPosition.put(position.getDealRef(),position);
    }
    private  void unRegisterPosition(String dealRef) {
        com.dario.agenttrader.dto.Position position = dealRefToPosition.remove(dealRef);
        epicToPositions.get(position.getEpic()).remove(dealRef);

    }

    public void confirmPosition(String epic, String dealRef,String dealId, boolean accepted){
        Map<String, com.dario.agenttrader.dto.Position> positionsOnEpic = Optional.ofNullable(epicToPositions.get(epic)).orElse(new HashMap<>());
        com.dario.agenttrader.dto.Position position = positionsOnEpic.get(dealRef);
        if(position!=null){
            if(accepted) {
                position.setConfirmed(true);
                position.setDealId(dealId);
            }else {
                unRegisterPosition(dealRef);
            }
        }
    }
    public void confirmPosition(DealConfirmation dealConf){

        confirmPosition(
                dealConf.getEpic()
                ,dealConf.getDealRef()
                ,dealConf.getDealId()
                ,dealConf.isAccepted());

    }
    public void confirmPosition(com.dario.agenttrader.dto.Position position, boolean accepted){
        confirmPosition(position.getEpic(),position.getDealRef(),position.getDealId(),accepted);

    }
    public  List<com.dario.agenttrader.dto.Position> getPositionsOnEpic(String epic){
        reconcilePositionsIfReconciliationIsDue();
        Map<String, com.dario.agenttrader.dto.Position> positionsOnEpic
                = Optional.ofNullable(epicToPositions.get(epic)).orElse(new ConcurrentHashMap<String, com.dario.agenttrader.dto.Position>());
        List<com.dario.agenttrader.dto.Position> listOfPositionsOnEpic =
                positionsOnEpic.values().stream().collect(Collectors.toList());

        return listOfPositionsOnEpic;
    }

    private void reconcilePositionsIfReconciliationIsDue() {
        if(isPositionReconciliationExpired()){
            List<PositionSnapshot> psnapshots = positionReloadSupplier.get();
            reconcilePositions(psnapshots);
        }
    }

    public void reconcilePositions(List<PositionSnapshot> psnapshots ) {
       lastPositionReconciliationTime=Instant.now();
       addMissingPositionInTheLocalCache(psnapshots);
       removeMistmatchPositionsInLocalCache(psnapshots);
    }

    private void removeMistmatchPositionsInLocalCache(List<PositionSnapshot> psnapshots) {
        Map<String,PositionSnapshot> dealRefToSnapshot = psnapshots.stream().collect(
                 Collectors.toMap(k->k.getPositionsItem().getPosition().getDealReference(), v->v));
        Map<String, com.dario.agenttrader.dto.Position> missingPositions = dealRefToPosition.values().stream().filter(p->{
             PositionSnapshot positionSnapshot = dealRefToSnapshot.get(p.getDealRef());
             if(positionSnapshot==null){
                 boolean isExpired = isConfirmExpired(p);
                 return  isExpired;
             }else{
                 return false;
             }
        }).collect(Collectors.toMap(v->v.getDealRef(),v->v));

        missingPositions.values().stream().forEach(p->checkConfirmWithBrokerIfConfirmWaitIsExpired(p.getDealRef(),p));
    }

    private void addMissingPositionInTheLocalCache(List<PositionSnapshot> psnapshots) {
        psnapshots.stream().forEach(p->{
           com.dario.agenttrader.dto.Position position = dealRefToPosition.get(p.getPositionsItem().getPosition().getDealReference());
           if(position==null){
               position = new com.dario.agenttrader.dto.Position(
                       p.getPositionsItem().getMarket().getEpic()
                       ,true
                       ,p.getPositionsItem().getPosition().getDealReference()
                       ,p.getPositionsItem().getPosition().getDealId()
                       ,p.getPositionsItem().getPosition().getSize().doubleValue()
                     , IGClientUtility.convertFromIGDirection(p.getPositionsItem().getPosition().getDirection())
                     ,p.getPositionsItem().getPosition().getCreatedDateUTC());
               trackNewPosition(position);
           }
        });
    }

    public List<com.dario.agenttrader.dto.Position> getConfirmedPositionsOnEpic(String epic){
        List<com.dario.agenttrader.dto.Position> listOfConfirmedPositionsOnEpic = getPositionsOnEpic(epic).stream()
                .filter(p->p.isConfirmed())
                .collect(Collectors.toList());

        return listOfConfirmedPositionsOnEpic;
    }

    public void setConfirmExpiryTimeOutMili(long confirmExpiryTimeOutMili) {
        this.confirmExpiryTimeOutMili = confirmExpiryTimeOutMili;
    }

    public void setPositionReloadSupplier(Supplier<List<PositionSnapshot>> positionReloadSupplier) {
        this.positionReloadSupplier = positionReloadSupplier;
    }

    public Supplier<List<PositionSnapshot>> getPositionReloadSupplier() {
        return positionReloadSupplier;
    }

    public Function<String, DealConfirmation> getDealConfirmationFunction() {
        return dealConfirmationFunction;
    }

    public void setDealConfirmationFunction(Function<String, DealConfirmation> dealConfirmationFunction) {
        this.dealConfirmationFunction = dealConfirmationFunction;
    }

    public long getPositionReconciliationTimeOutMili() {
        return positionReconciliationTimeOutMili;
    }

    public void setPositionReconciliationTimeOutMili(long positionReconciliationTimeOutMili) {
        this.positionReconciliationTimeOutMili = positionReconciliationTimeOutMili;
    }

    public boolean isPositionConfirmed(String dealRef) {
        reconcilePositionsIfReconciliationIsDue();
        com.dario.agenttrader.dto.Position position = dealRefToPosition.get(dealRef);
        if(position==null){
            return false;
        }

        if(position.isConfirmed()){
            return true;
        }
        checkConfirmWithBrokerIfConfirmWaitIsExpired(dealRef, position);
        boolean isConfirmed = position.isConfirmed();
        return  isConfirmed;
    }

    private void checkConfirmWithBrokerIfConfirmWaitIsExpired(String dealRef, com.dario.agenttrader.dto.Position position) {
        if(isConfirmExpired(position)){
            DealConfirmation dealConf = dealConfirmationFunction.apply(position.getDealRef());
            if(dealConf!=null && dealConf.isAccepted()){
                confirmPosition(position,true);
            }else{
                unRegisterPosition(dealRef);
            }
        }
    }

    private boolean isPositionReconciliationExpired(){
        if(lastPositionReconciliationTime==null){
            return true;
        }
        Instant nowInstant = Instant.now();
        long milisSinceLastReconciliation = ChronoUnit.MILLIS.between(lastPositionReconciliationTime,nowInstant);
        if(milisSinceLastReconciliation>positionReconciliationTimeOutMili){
            return true;
        }

        return false;
    }

    private boolean isConfirmExpired(com.dario.agenttrader.dto.Position position) {
        Instant nowInstant = Instant.now();
        Instant confExpiryTime = position.getCreatedTime().plusMillis(confirmExpiryTimeOutMili);
        boolean isExpired = nowInstant.isAfter(confExpiryTime);

        return isExpired;
    }

    public boolean doesPositionExist(String dealRef) {
        return dealRefToPosition.get(dealRef)!=null;
    }

    public boolean epicHasNoPosition(String epic) {
        boolean emptyEpic = getPositionsOnEpic(epic).isEmpty();
        return  emptyEpic;
    }

    public boolean trackIfEPICHasNoOtherPosition(Position position){
        boolean empty = epicHasNoPosition(position.getEpic());
        if(empty){
            trackNewPosition(position);
        }
        return empty;
    }

    //TODO:test
    public Position getOppositePosition(TradingSignal signal) {
        List<Position> listOfPositions = getPositionsOnEpic(signal.getEpic());
        Optional<Position> position = listOfPositions.stream()
                .filter(p->p.getSize()==signal.getSize().doubleValue() && p.getDirection().isInOppositDirection(signal.getDirection()))
                .findFirst();
        return position.orElse(null);
    }
}
