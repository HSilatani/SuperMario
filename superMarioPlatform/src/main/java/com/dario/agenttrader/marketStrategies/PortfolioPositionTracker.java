package com.dario.agenttrader.marketStrategies;

import com.dario.agenttrader.domain.Direction;
import com.dario.agenttrader.dto.DealConfirmation;
import com.dario.agenttrader.dto.PositionSnapshot;
import com.dario.agenttrader.utility.Calculator;
import com.dario.agenttrader.utility.IGClientUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PortfolioPositionTracker{
    private final static Logger LOG = LoggerFactory.getLogger(PortfolioPositionTracker.class);
    public static final String STATUS_ACEPTED = "ACEPTED";
    public static final String STATUS_REJECTED = "REJECTED";

    private volatile Map<String,Map<String,Position>> epicToPositions = new ConcurrentHashMap<String, Map<String,Position>>();
    private volatile Map<String,Position>  dealRefToPosition = new ConcurrentHashMap<String,Position>();
    private int confirmExpiryTimeOutMili = 2000;
    private int positionReconciliationTimeOutMili = 30000;
    private Instant lastPositionReconciliationTime = null;
    private Supplier<List<PositionSnapshot>> positionReloadSupplier;
    private Function<String,DealConfirmation> dealConfirmationFunction;

    public void trackNewPosition(Position position){
        Map<String,Position> positionsOnEpic = epicToPositions.get(position.getEpic());
        positionsOnEpic = Optional.ofNullable(positionsOnEpic).orElse(new ConcurrentHashMap<String,Position>());
        positionsOnEpic.put(position.getDealRef(),position);
        registerPosition(position, positionsOnEpic);
    }

    private  void registerPosition(Position position, Map<String, Position> positionsOnEpic) {
        epicToPositions.put(position.getEpic(),positionsOnEpic);
        dealRefToPosition.put(position.getDealRef(),position);
    }
    private  void unRegisterPosition(String dealRef) {
        Position position = dealRefToPosition.remove(dealRef);
        epicToPositions.get(position.getEpic()).remove(dealRef);

    }

    public void confirmPosition(String epic, String dealRef,String dealId, boolean accepted){
        Map<String,Position> positionsOnEpic = Optional.ofNullable(epicToPositions.get(epic)).orElse(new HashMap<>());
        Position position = positionsOnEpic.get(dealRef);
        if(position!=null){
            if(accepted) {
                position.setConfirmed(true);
                position.setDealId(dealId);
            }else {
                unRegisterPosition(dealRef);
            }
        }
    }
    public void confirmPosition(Position position, boolean accepted){
        confirmPosition(position.getEpic(),position.getDealRef(),position.dealId,accepted);

    }
    public  List<Position> getPositionsOnEpic(String epic){
        reconcilePositionsIfReconciliationIsDue();
        Map<String,Position> positionsOnEpic
                = Optional.ofNullable(epicToPositions.get(epic)).orElse(new ConcurrentHashMap<String, Position>());
        List<Position> listOfPositionsOnEpic =
                positionsOnEpic.values().stream().collect(Collectors.toList());

        return listOfPositionsOnEpic;
    }

    private void reconcilePositionsIfReconciliationIsDue() {
        if(isPositionReconciliationExpired()){
            reconcilePositions();
        }
    }

    private void reconcilePositions() {

       lastPositionReconciliationTime=Instant.now();

       List<PositionSnapshot> psnapshots = positionReloadSupplier.get();

       addMissingPositionInTheLocalCache(psnapshots);

       removeMistmatchPositionsInLocalCache(psnapshots);
    }

    private void removeMistmatchPositionsInLocalCache(List<PositionSnapshot> psnapshots) {
        Map<String,PositionSnapshot> dealRefToSnapshot = psnapshots.stream().collect(
                 Collectors.toMap(k->k.getPositionsItem().getPosition().getDealReference(), v->v));
        Map<String,Position> missingPositions = dealRefToPosition.values().stream().filter(p->{
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
                       p.getPositionsItem().getPosition().getDealId()
                       ,p.getPositionsItem().getPosition().getDealReference()
                       ,p.getPositionsItem().getPosition().getSize().doubleValue()
                     , IGClientUtility.convertFromIGDirection(p.getPositionsItem().getPosition().getDirection())
                     ,p.getPositionsItem().getPosition().getCreatedDateUTC());
               trackNewPosition(position);
           }
        });
    }

    public List<Position> getConfirmedPositionsOnEpic(String epic){
        List<Position> listOfConfirmedPositionsOnEpic = getPositionsOnEpic(epic).stream()
                .filter(p->p.confirmed)
                .collect(Collectors.toList());

        return listOfConfirmedPositionsOnEpic;
    }

    public void setConfirmExpiryTimeOutMili(int confirmExpiryTimeOutMili) {
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

    public int getPositionReconciliationTimeOutMili() {
        return positionReconciliationTimeOutMili;
    }

    public void setPositionReconciliationTimeOutMili(int positionReconciliationTimeOutMili) {
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
        boolean isConfirmed = position.confirmed;
        return  isConfirmed;
    }

    private void checkConfirmWithBrokerIfConfirmWaitIsExpired(String dealRef, Position position) {
        if(isConfirmExpired(position)){
            DealConfirmation dealConf = dealConfirmationFunction.apply(position.dealRef);
            if(dealConf!=null && STATUS_ACEPTED.equalsIgnoreCase(dealConf.getStatus())){
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

    private boolean isConfirmExpired(Position position) {



        Instant nowInstant = Instant.now();
        Instant confExpiryTime = position.getCreatedTime().plusMillis(confirmExpiryTimeOutMili);
        boolean isExpired = nowInstant.isAfter(confExpiryTime);

        return isExpired;
    }

    public boolean doesPositionExist(String dealRef) {
        return dealRefToPosition.get(dealRef)!=null;
    }


    public static final class Position{
        private String epic;
        private boolean confirmed;
        private String dealRef;
        private String dealId;
        private double size;
        private Direction direction;
        private Instant createdTime;

        public Position(String pepic,String dealRef, double size, Direction direction) {
           this(pepic,false,dealRef,null,size,direction,null);
        }
        public Position(String pepic,String dealRef, double size, Direction direction,String pCreatedDateTime) {
            this(pepic,false,dealRef,null,size,direction,pCreatedDateTime);
        }
        public Position(
                String pepic
                ,boolean confirmed
                , String dealRef
                , String dealId
                , double size
                , Direction direction
                , String strOpenUTM) {

            this.epic = pepic;
            this.confirmed = confirmed;
            this.dealRef = dealRef;
            this.dealId = dealId;
            this.size = size;
            this.direction = direction;
            createdTime = (strOpenUTM==null)?Instant.now():Calculator.zonedDateTimeFromString(strOpenUTM).toInstant();
        }

        public boolean isConfirmed() {
            return confirmed;
        }

        public String getDealRef() {
            return dealRef;
        }

        public String getDealId() {
            return dealId;
        }

        public double getSize() {
            return size;
        }

        public Direction getDirection() {
            return direction;
        }

        public String getEpic() {
            return epic;
        }

        public Instant getCreatedTime() {
            return createdTime;
        }

        public void setConfirmed(boolean confirmed) {
            this.confirmed = confirmed;
        }

        public void setDealId(String dealId) {
            this.dealId = dealId;
        }
    }
}
