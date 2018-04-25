package com.dario.agenttrader.marketStrategies;

import com.dario.agenttrader.TestPositionProvider;
import com.dario.agenttrader.domain.Direction;
import com.dario.agenttrader.domain.DealConfirmation;
import com.dario.agenttrader.domain.Position;
import com.dario.agenttrader.domain.PositionSnapshot;
import com.dario.agenttrader.utility.IGClientUtility;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PortfolioPositionTrackerTest {
    private final static Logger LOG = LoggerFactory.getLogger(PortfolioPositionTrackerTest.class);


    @Test
    public void testTrackNewPositionBasicAcceptAndRejectConfirmForBUYPosition(){
        PortfolioPositionTracker positionTracker = new PortfolioPositionTracker();
        positionTracker.setPositionReloadSupplier(()->new ArrayList<>());
        positionTracker.setDealConfirmationFunction(dref->new DealConfirmation(dref,"",DealConfirmation.STATUS_REJECTED));

        String epic= "IX.HNG";
        String ref = "AASSD";
        String dealId = "dealID";

        double size = 1.5;
        Direction direction = Direction.BUY();

        Position position1 = new Position(epic,ref,size,direction);
        positionTracker.trackNewPosition(position1);

        List<Position> confirmedPositions =  positionTracker.getConfirmedPositionsOnEpic(epic);
        assertThat(confirmedPositions,is(empty()));
        assertThat(positionTracker.epicHasNoPosition(epic),is(false));

        positionTracker.confirmPosition(epic,"WRONG_REF",dealId,true);
        confirmedPositions =  positionTracker.getConfirmedPositionsOnEpic(epic);
        assertThat(confirmedPositions,is(empty()));
        assertThat(positionTracker.epicHasNoPosition(epic),is(false));

        positionTracker.confirmPosition("WRONG_REF",ref,dealId,true);
        confirmedPositions =  positionTracker.getConfirmedPositionsOnEpic(epic);
        assertThat(confirmedPositions,is(empty()));
        assertThat(positionTracker.epicHasNoPosition(epic),is(false));

        positionTracker.confirmPosition(epic,ref,dealId,true);
        confirmedPositions =  positionTracker.getConfirmedPositionsOnEpic(epic);
        assertThat(confirmedPositions,hasItem(position1));
        assertThat(confirmedPositions,hasSize(1));
        assertThat(positionTracker.epicHasNoPosition(epic),is(false));

    }
    @Test
    public void testTrackNewPositionBasicRejectConfirmForBUYPosition(){
        PortfolioPositionTracker positionTracker = new PortfolioPositionTracker();
        positionTracker.setPositionReloadSupplier(()->new ArrayList<>());
        positionTracker.setDealConfirmationFunction(dref->new DealConfirmation(dref,"",DealConfirmation.STATUS_REJECTED));

        String epic= "IX.HNG";
        String ref = "AASSD";
        String dealId = "dealID";

        double size = 1.5;
        Direction direction = Direction.BUY();

        Position position1 = new Position(epic,ref,size,direction);
        positionTracker.trackNewPosition(position1);

        positionTracker.confirmPosition(epic,ref,dealId,false);
        List<Position> confirmedPositions  =  positionTracker.getConfirmedPositionsOnEpic(epic);
        assertThat(confirmedPositions,is(empty()));
        assertThat(positionTracker.epicHasNoPosition(epic),is(true));
    }
    @Test
    public void testTrackNewPositionBasicAcceptAndRejectConfirmForSELLPosition(){
        PortfolioPositionTracker positionTracker = new PortfolioPositionTracker();
        positionTracker.setPositionReloadSupplier(()->new ArrayList<>());
        positionTracker.setDealConfirmationFunction(dref->new DealConfirmation(dref,"",DealConfirmation.STATUS_REJECTED));

        String epic= "IX.HNG";
        String ref = "AASSD";
        String dealId = "dealID";

        double size = 1.5;
        Direction direction = Direction.SELL();

        Position position1 = new Position(epic,ref,size,direction);
        positionTracker.trackNewPosition(position1);

        List<Position> confirmedPositions =  positionTracker.getConfirmedPositionsOnEpic(epic);
        assertThat(positionTracker.epicHasNoPosition(epic),is(false));
        assertThat(confirmedPositions,is(empty()));

        positionTracker.confirmPosition(epic,"WRONG_REF",dealId,true);
        confirmedPositions =  positionTracker.getConfirmedPositionsOnEpic(epic);
        assertThat(positionTracker.epicHasNoPosition(epic),is(false));
        assertThat(confirmedPositions,is(empty()));

        positionTracker.confirmPosition("WRONG_REF",ref,dealId,true);
        confirmedPositions =  positionTracker.getConfirmedPositionsOnEpic(epic);
        assertThat(positionTracker.epicHasNoPosition(epic),is(false));
        assertThat(confirmedPositions,is(empty()));

        positionTracker.confirmPosition(epic,ref,dealId,true);
        confirmedPositions =  positionTracker.getConfirmedPositionsOnEpic(epic);
        assertThat(positionTracker.epicHasNoPosition(epic),is(false));
        assertThat(confirmedPositions,hasItem(position1));
        assertThat(confirmedPositions,hasSize(1));



    }
    @Test
    public void testTrackNewPositionBasicRejectConfirmForSELLPosition(){
        PortfolioPositionTracker positionTracker = new PortfolioPositionTracker();
        positionTracker.setPositionReloadSupplier(()->new ArrayList<>());
        positionTracker.setDealConfirmationFunction(dref->new DealConfirmation(dref,"",DealConfirmation.STATUS_REJECTED));

        String epic= "IX.HNG";
        String ref = "AASSD";
        String dealId = "dealID";

        double size = 1.5;
        Direction direction = Direction.SELL();

        Position position1 = new Position(epic,ref,size,direction);
        positionTracker.trackNewPosition(position1);

        positionTracker.confirmPosition(epic,ref,dealId,false);
        List<Position> confirmedPositions=  positionTracker.getConfirmedPositionsOnEpic(epic);
        assertThat(positionTracker.epicHasNoPosition(epic),is(true));
        assertThat(confirmedPositions,is(empty()));

    }
    @Test
    public void testTrackNewAcceptedPositionWithConfirmExpiration(){
        int confirmExpiryTimeOutMili = 500;
        int positionReconciliationTimoutMili=5000;
        String dealRef="ADASDASDRQEQREGSGSGSG@£$@£$!!";
        PortfolioPositionTracker positionTracker = getPortfolioPositionTrackerWithPositionReloadSupplierAndAcceptConfirmFunction(
                confirmExpiryTimeOutMili,positionReconciliationTimoutMili,dealRef);
        Position position1 = createTestBuyPosition(dealRef,1000);
        positionTracker.trackNewPosition(position1);
        //
        boolean isConfirmed = positionTracker.isPositionConfirmed(dealRef);
        assertThat(isConfirmed,is(true));
    }
    @Test
    public void testTracAcceptedPositionFollowedByRejectedEdit(){
        int confirmExpiryTimeOutMili = 500;
        int positionReconciliationTimoutMili=5000;
        String dealRef="ADASDASDRQEQREGSGSGSG@£$@£$!!";
        PortfolioPositionTracker positionTracker = getPortfolioPositionTrackerWithPositionReloadSupplierAndAcceptConfirmFunction(
                confirmExpiryTimeOutMili,positionReconciliationTimoutMili,dealRef);
        Position position1 = createTestBuyPosition(dealRef,1000);
        positionTracker.trackNewPosition(position1);
        //
        boolean isConfirmed = positionTracker.isPositionConfirmed(dealRef);
        assertThat(isConfirmed,is(true));
        //
        DealConfirmation dealEditRejected = new DealConfirmation(position1.getEpic(),dealRef,"DEALID!@£!@£",DealConfirmation.STATUS_REJECTED,"ATTACHED_ORDER_REJECTED");
        positionTracker.confirmPosition(dealEditRejected);
        isConfirmed = positionTracker.isPositionConfirmed(dealRef);
        assertThat(isConfirmed,is(true));
    }
    @Test
    public void testTrackNewRejectedPositionWithConfirmExpiration(){
        int confirmExpiryTimeOutMili=500;
        int positionReconciliationTimoutMili=5000;
        PortfolioPositionTracker positionTracker = getPortfolioPositionTrackerWithPositionReloadSupplierAndRejectingConfirmFunction(confirmExpiryTimeOutMili, positionReconciliationTimoutMili);
        //
        String dealRef = "AASSD";

        Position position1 = createTestBuyPosition(dealRef,600);
        positionTracker.trackNewPosition(position1);
        //
        boolean isConfirmed = positionTracker.isPositionConfirmed(dealRef);
        assertThat(isConfirmed,is(false));
        //
        boolean doesPositionExist = positionTracker.doesPositionExist(dealRef);
        assertThat(doesPositionExist,is(false));
        //
        List<Position> listOfPositionsOnEpic = positionTracker.getPositionsOnEpic(position1.getEpic());
        assertThat(listOfPositionsOnEpic,is(empty()));
    }
    @Test
    public void testReconTrackedPositionsWithBrokerWhenBrokerPositionsAreMissing(){
        int confirmExpiryTimeOutMili=500;
        int positionReconciliationTimoutMili=5000;
        String dealRef="WERWERFFDSDFSF";
        PortfolioPositionTracker positionTracker =
                getPortfolioPositionTrackerWithPositionReloadSupplierAndAcceptConfirmFunction(
                        confirmExpiryTimeOutMili
                        ,positionReconciliationTimoutMili
                        ,dealRef
                        );
        //
        boolean isConfirmed = positionTracker.isPositionConfirmed(dealRef);
        assertThat(isConfirmed,is(true));
        //Shouldnt reconcile again before Expiry
        Supplier mockedPositionReloadSupplier =  mock(Supplier.class);
        when(mockedPositionReloadSupplier.get()).thenReturn(new ArrayList<PositionSnapshot>());
        positionTracker.setPositionReloadSupplier(mockedPositionReloadSupplier);

        isConfirmed = positionTracker.isPositionConfirmed(dealRef);
        verifyZeroInteractions(mockedPositionReloadSupplier);
    }
    @Test
    public void testReconTrackedPositionsWithBrokerWhenLocalCacheHasPositionsThatBrokerDoesNotHave(){
        int confirmExpiryTimeOutMili=500;
        int positionReconciliationTimoutMili=5000;
        PortfolioPositionTracker positionTracker = getPortfolioPositionTrackerWithPositionReloadSupplierWithConfirmNotFoundFunction(confirmExpiryTimeOutMili, positionReconciliationTimoutMili);
        //
        String dealRef = "AASSD";
        Position position1 = createTestBuyPosition(dealRef,1000);
        positionTracker.trackNewPosition(position1);
        //
        boolean isConfirmed = positionTracker.isPositionConfirmed(dealRef);
        assertThat(isConfirmed,is(false));
        //
        boolean doesPositionExist = positionTracker.doesPositionExist(dealRef);
        assertThat(doesPositionExist,is(false));
    }
    @Test
    public void testReconTrackedPositionsBeforeConfirmExpiryWithBrokerWhenLocalCacheHasPositionsThatBrokerDoesNotHaveYet(){
        int confirmExpiryTimeOutMili=120000;
        int positionReconciliationTimoutMili=5000;
        PortfolioPositionTracker positionTracker = getPortfolioPositionTrackerWithPositionReloadSupplierWithConfirmNotFoundFunction(confirmExpiryTimeOutMili, positionReconciliationTimoutMili);
        //
        String dealRef = "AASSD";
        Position position1 = createTestBuyPosition(dealRef,10);
        positionTracker.trackNewPosition(position1);
        //
        boolean isConfirmed = positionTracker.isPositionConfirmed(dealRef);
        assertThat(isConfirmed,is(false));
        //
        boolean doesPositionExist = positionTracker.doesPositionExist(dealRef);
        assertThat(doesPositionExist,is(true));


    }
    @Test
    public void testConcurrencyWhenNewPositionOnEpicIsNotConfirmedYetAndNotExpiredButManyThreadsAreQuerying() throws Exception{
        int confirmExpiryTimeOutMili=120000;
        int positionReconciliationTimoutMili=5000;
        final PortfolioPositionTracker positionTracker = getPortfolioPositionTrackerWithPositionReloadSupplierWithConfirmNotFoundFunction(confirmExpiryTimeOutMili, positionReconciliationTimoutMili);
        //
        final String epic = "IX.HNG";
        //
        int numberOfConcurrentTradingSignals = 10;

        ExecutorService service = Executors.newSingleThreadExecutor();

        List<Callable<String>> signals = new ArrayList<>();
        for(int i=0;i<numberOfConcurrentTradingSignals;i++) {
            final int attempt = i+1;
            signals.add(()->{
                String dealRef = "AASSD" + UUID.randomUUID().toString();
                LOG.info("Checking if there is a position on {}",epic);
                Position position1 = createTestBuyPosition(dealRef, 10);
                boolean tracked = positionTracker.trackIfEPICHasNoOtherPosition(position1);
                if (tracked) {
                    LOG.info("Horray created my position on attempt number {}",attempt);
                }
                return  dealRef;
            });
        }
        //
        List<Future<String>> results = service.invokeAll(signals,10,TimeUnit.SECONDS);
        int numberOfPositionsOnEpic = positionTracker.getPositionsOnEpic(epic).size();
        assertThat(numberOfPositionsOnEpic,is(1));

    }

    private PortfolioPositionTracker getPortfolioPositionTrackerWithPositionReloadSupplierWithConfirmNotFoundFunction(int confirmExpiryTimeOutMili, int positionReconciliationTimoutMili) {
        int snapshotAgeInMilis = 1000;
        PositionSnapshot positionSnapshot = getPositionSnapshotWithAge(snapshotAgeInMilis);
        PortfolioPositionTracker positionTracker = getPortfolioPositionTracker(confirmExpiryTimeOutMili, positionSnapshot);
        positionTracker.setPositionReconciliationTimeOutMili(positionReconciliationTimoutMili);
        positionTracker.setDealConfirmationFunction(
                dRef-> null);
        return positionTracker;
    }

    private PositionSnapshot getPositionSnapshotWithAge(int snapshotAgeInMilis) {
        String positionCreatedDate = Instant.now().minusMillis(snapshotAgeInMilis).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return TestPositionProvider.getPositionSnapshot(positionCreatedDate);
    }
    private PortfolioPositionTracker getPortfolioPositionTrackerWithPositionReloadSupplierAndRejectingConfirmFunction(
            int confirmExpiryTimeOutMili
            , int positionReconciliationTimoutMili
    ){
        return getPortfolioPositionTrackerWithPositionReloadSupplierAndRejectingConfirmFunction(
                confirmExpiryTimeOutMili
                ,positionReconciliationTimoutMili
                ,"IWERUWERUWERWERJJFMM");
    }
    private PortfolioPositionTracker getPortfolioPositionTrackerWithPositionReloadSupplierAndRejectingConfirmFunction(
            int confirmExpiryTimeOutMili
            , int positionReconciliationTimoutMili
            ,String dealRef
    ) {
        PositionSnapshot positionSnapshot = getPositionSnapshotWithAge(1000);
        positionSnapshot.getPositionsItem().getPosition().setDealReference(dealRef);
        PortfolioPositionTracker positionTracker = getPortfolioPositionTracker(confirmExpiryTimeOutMili, positionSnapshot);
        positionTracker.setPositionReconciliationTimeOutMili(positionReconciliationTimoutMili);
        positionTracker.setDealConfirmationFunction(
                dRef-> new DealConfirmation(
                        positionSnapshot.getPositionsItem().getPosition().getDealReference()
                        ,positionSnapshot.getPositionsItem().getPosition().getDealId()
                        ,DealConfirmation.STATUS_REJECTED));
        return positionTracker;
    }
    private PortfolioPositionTracker getPortfolioPositionTrackerWithPositionReloadSupplierAndAcceptConfirmFunction(
            int confirmExpiryTimeOutMili
            ,int positionReconciliationTimoutMili
            ,String dealRef) {
        PositionSnapshot positionSnapshot = getPositionSnapshotWithAge(1000);
        positionSnapshot.getPositionsItem().getPosition().setDealReference(dealRef);
        PortfolioPositionTracker positionTracker = getPortfolioPositionTracker(confirmExpiryTimeOutMili, positionSnapshot);
        positionTracker.setPositionReconciliationTimeOutMili(positionReconciliationTimoutMili);
        positionTracker.setDealConfirmationFunction(
                dRef-> new DealConfirmation(
                        dealRef
                        ,"DIASDFSFDS£@£$@£$@£$"
                        ,DealConfirmation.STATUS_ACEPTED));
        return positionTracker;
    }
    private Position createTestBuyPosition(String epic, String dealRef, int positionAgeInMilis) {
        String dealId = "dealID";
        String createdDateTime =
                Instant.now().minusMillis(positionAgeInMilis).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);;
        double size = 1.5;
        Direction direction = Direction.BUY();
        return new Position(epic,dealRef,size,direction,createdDateTime);
    }
    private Position createTestBuyPosition(String dealRef, int positionAgeInMilis) {
        String epic= "IX.HNG";
        String dealId = "dealID";
        return  createTestBuyPosition(epic,dealRef,positionAgeInMilis);
    }
    private PortfolioPositionTracker getPortfolioPositionTracker(int confirmExpiryTimeOutMili, PositionSnapshot positionSnapshot) {
        PortfolioPositionTracker positionTracker = null;
        positionTracker = new PortfolioPositionTracker();
        positionTracker.setConfirmExpiryTimeOutMili(confirmExpiryTimeOutMili);
        positionTracker.setPositionReloadSupplier(()->{
            List<PositionSnapshot> psnapshots = new ArrayList<>();
            psnapshots.add(positionSnapshot);
            return  psnapshots;
        });
        return positionTracker;
    }

    private Position createtestPosition(PositionSnapshot positionSnapshot) {
        String epic= positionSnapshot.getPositionsItem().getMarket().getEpic();
        String ref =  positionSnapshot.getPositionsItem().getPosition().getDealId();
        double size = positionSnapshot.getPositionsItem().getPosition().getSize().doubleValue();
        Direction direction = IGClientUtility.convertFromIGDirection(
            positionSnapshot.getPositionsItem().getPosition().getDirection()
        );
        String createdDateTime = positionSnapshot.getPositionsItem().getPosition().getCreatedDateUTC();

        return new Position(epic,ref,size,direction,createdDateTime);
    }
}
