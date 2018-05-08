package com.dario.agenttrader.marketStrategies;

import com.dario.agenttrader.domain.*;
import com.dario.agenttrader.utility.IGClientUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PortfolioPositionTracker{
    private final static Logger LOG = LoggerFactory.getLogger(PortfolioPositionTracker.class);


    private volatile Map<String,Map<String, Position>> epicToPositions = new ConcurrentHashMap<String, Map<String, Position>>();
    private volatile Map<String, Position>  dealRefToPosition = new ConcurrentHashMap<String, Position>();
    private long confirmExpiryTimeOutMili = 2000;
    private long positionReconciliationTimeOutMili = 30000;
    private Instant lastPositionReconciliationTime = null;
    private Supplier<List<PositionSnapshot>> positionReloadSupplier;
    private Function<String,DealConfirmation> dealConfirmationFunction;
    private Map<String, Instant> epicsInCoolingOffPeriod = new ConcurrentHashMap<>();
    private Duration epiccoolingOffPeriod = Duration.ofMinutes(2);

    public void trackNewPosition(Position position){
        Map<String, Position> positionsOnEpic = epicToPositions.get(position.getEpic());
        positionsOnEpic = Optional.ofNullable(positionsOnEpic).orElse(new ConcurrentHashMap<String, Position>());
        int sizeBeforeTracking = positionsOnEpic.size();
        positionsOnEpic.put(position.getDealRef(),position);
        registerPosition(position, positionsOnEpic);
        int sizeAfterTracking = epicToPositions.get(position.getEpic()).size();
        LOG.debug("Number of positions on {} incremented from  {} to {}",position.getEpic(),sizeBeforeTracking,sizeAfterTracking);
    }
    public void removePosition(String dealRef) {
        unRegisterPosition(dealRef);
    }
    private  void registerPosition(Position position, Map<String, Position> positionsOnEpic) {
        epicToPositions.put(position.getEpic(),positionsOnEpic);
        dealRefToPosition.put(position.getDealRef(),position);

        int countOfAllPositionsInEpicToPosition = epicToPositions.values().stream().filter(v->v!=null).mapToInt(v->v.size()).sum();
        LOG.debug("Count of positions in DealRefToPosition={}, EpicTpPosition={}",dealRefToPosition.size(),countOfAllPositionsInEpicToPosition);
    }
    private  void unRegisterPosition(String dealRef) {
        Position position = dealRefToPosition.remove(dealRef);
        epicToPositions.get(position.getEpic()).remove(dealRef);
        epicsInCoolingOffPeriod.put(position.getEpic(),Instant.now());

    }

    public void confirmPosition(String epic, String dealRef,String dealId, boolean accepted){
        Map<String, Position> positionsOnEpic = Optional.ofNullable(epicToPositions.get(epic)).orElse(new HashMap<>());
        Position position = positionsOnEpic.get(dealRef);
        LOG.debug("Updating local position cache for position {} with confirm [{},{},{},{}]",position,epic,dealRef,dealId,accepted);
        if(position!=null && !position.isConfirmed()){
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
    public void confirmPosition(Position position, boolean accepted){
        confirmPosition(position.getEpic(),position.getDealRef(),position.getDealId(),accepted);

    }
    public  List<Position> getPositionsOnEpic(String epic){
        reconcilePositionsIfReconciliationIsDue();
        Map<String, Position> positionsOnEpic
                = Optional.ofNullable(epicToPositions.get(epic)).orElse(new ConcurrentHashMap<String, Position>());
        List<Position> listOfPositionsOnEpic =
                positionsOnEpic.values().stream().collect(Collectors.toList());

        return listOfPositionsOnEpic;
    }

    private void reconcilePositionsIfReconciliationIsDue() {
        if(isPositionReconciliationExpired()){
            //TODO:this protective mechanism (if cant reload positions reset the timer to protect end point) should be replaced by circuitBreaker mechanism
            lastPositionReconciliationTime=Instant.now();
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
        Map<String, Position> missingPositions = dealRefToPosition.values().stream().filter(p->{
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
           Position position = dealRefToPosition.get(p.getPositionsItem().getPosition().getDealReference());
           if(position==null){
               position = new Position(
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

    public List<Position> getConfirmedPositionsOnEpic(String epic){
        List<Position> listOfConfirmedPositionsOnEpic = getPositionsOnEpic(epic).stream()
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
        Position position = dealRefToPosition.get(dealRef);
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

    private void checkConfirmWithBrokerIfConfirmWaitIsExpired(String dealRef, Position position) {
        if(isConfirmExpired(position)){
            LOG.debug("Calling broker to confirm {}",position.getDealRef());
            DealConfirmation dealConf = dealConfirmationFunction.apply(position.getDealRef());
            LOG.debug("Retrieved confirm for  {} , conf {}",position,dealConf);
            if(dealConf!=null && dealConf.isAccepted()){
                confirmPosition(position,true);
            }else{
                unRegisterPosition(dealRef);
            }
        }
    }

    private boolean isPositionReconciliationExpired(){
        boolean isPositionReconDue = false;
        long milisSinceLastReconciliation=-1;
        if(lastPositionReconciliationTime==null){
            isPositionReconDue=true;
        }else {
            Instant nowInstant = Instant.now();
            milisSinceLastReconciliation = ChronoUnit.MILLIS.between(lastPositionReconciliationTime, nowInstant);
            if (milisSinceLastReconciliation > positionReconciliationTimeOutMili) {
                isPositionReconDue=true;
            }
        }
        LOG.debug("Position Reconciliation is Due={} , last reconciliation time={},Milis since last reconciliation={}, reconciliation timeout={}",
                isPositionReconDue
                ,lastPositionReconciliationTime
                ,milisSinceLastReconciliation
                ,positionReconciliationTimeOutMili);
        return isPositionReconDue;
    }

    private boolean isConfirmExpired(Position position) {
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

    public boolean isEpicInCoolingOffPeriod(String epic){
        boolean isInCoolingOffPeriodStill = true;

        Instant coolingStartTime = epicsInCoolingOffPeriod.get(epic);
        if (coolingStartTime != null){
            isInCoolingOffPeriodStill=Duration.between(coolingStartTime,Instant.now()).toMillis()<epiccoolingOffPeriod.toMillis();
        }else{
            isInCoolingOffPeriodStill = false;
        }
        LOG.debug("epic:{} is in cooling off period:{} since:{}",epic,isInCoolingOffPeriodStill,coolingStartTime);
        return isInCoolingOffPeriodStill;
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
        Position position = getPositionsOnEpic(signal.getEpic(),signal.getDirection().opposite());
        return position;
    }
    //TODO:TEST
    public Position getPositionsOnEpic(String epic, Direction direction) {
        List<Position> listOfPositions = getPositionsOnEpic(epic);
        Optional<Position> position = listOfPositions.stream()
                .filter(p->p.getDirection().isInSameDirection(direction))
                .findFirst();
        return position.orElse(null);
    }

    public long getConfirmExpiryTimeOutMili() {
        return confirmExpiryTimeOutMili;
    }

    public Instant getLastPositionReconciliationTime() {
        return lastPositionReconciliationTime;
    }

    public Duration getEpiccoolingOffPeriod() {
        return epiccoolingOffPeriod;
    }

    public void setLastPositionReconciliationTime(Instant lastPositionReconciliationTime) {
        this.lastPositionReconciliationTime = lastPositionReconciliationTime;
    }

    public void setEpiccoolingOffPeriod(Duration epiccoolingOffPeriod) {
        this.epiccoolingOffPeriod = epiccoolingOffPeriod;
    }


}
