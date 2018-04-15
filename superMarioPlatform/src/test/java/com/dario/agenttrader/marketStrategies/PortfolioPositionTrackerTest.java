package com.dario.agenttrader.marketStrategies;

import com.dario.agenttrader.TestPositionProvider;
import com.dario.agenttrader.domain.Direction;
import com.dario.agenttrader.dto.DealConfirmation;
import com.dario.agenttrader.dto.PositionSnapshot;
import com.dario.agenttrader.utility.Calculator;
import com.dario.agenttrader.utility.IGClientUtility;
import org.junit.Test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PortfolioPositionTrackerTest {

    @Test
    public void testTrackNewPositionConfirm(){
        PortfolioPositionTracker positionTracker = new PortfolioPositionTracker();
        positionTracker.setPositionReloadSupplier(()->new ArrayList<>());
        positionTracker.setDealConfirmationFunction(dref->new DealConfirmation(dref,"",PortfolioPositionTracker.STATUS_REJECTED));

        String epic= "IX.HNG";
        String ref = "AASSD";
        String dealId = "dealID";

        double size = 1.5;
        Direction direction = Direction.BUY();

        PortfolioPositionTracker.Position position1 = new PortfolioPositionTracker.Position(epic,ref,size,direction);
        positionTracker.trackNewPosition(position1);

        List<PortfolioPositionTracker.Position> confirmedPositions =  positionTracker.getConfirmedPositionsOnEpic(epic);
        assertThat(confirmedPositions,is(empty()));

        positionTracker.confirmPosition(epic,"WRONG_REF",dealId,true);
        confirmedPositions =  positionTracker.getConfirmedPositionsOnEpic(epic);
        assertThat(confirmedPositions,is(empty()));

        positionTracker.confirmPosition("WRONG_REF",ref,dealId,true);
        confirmedPositions =  positionTracker.getConfirmedPositionsOnEpic(epic);
        assertThat(confirmedPositions,is(empty()));

        positionTracker.confirmPosition(epic,ref,dealId,true);
        confirmedPositions =  positionTracker.getConfirmedPositionsOnEpic(epic);
        assertThat(confirmedPositions,hasItem(position1));
        assertThat(confirmedPositions,hasSize(1));

        positionTracker.confirmPosition(epic,ref,dealId,false);
        confirmedPositions =  positionTracker.getConfirmedPositionsOnEpic(epic);
        assertThat(confirmedPositions,is(empty()));
    }

    @Test
    public void testTrackNewPositionWithConfirmExpiration(){
        int confirmExpiryTimeOutMili = 500;
        String positionCreatedDate = Instant.now().minusMillis(1000).atZone(Calculator.ZONE_ID_LONDON).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        PositionSnapshot positionSnapshot  = TestPositionProvider.getPositionSnapshot(positionCreatedDate);
        PortfolioPositionTracker positionTracker = getPortfolioPositionTracker(confirmExpiryTimeOutMili, positionSnapshot);
        positionTracker.setDealConfirmationFunction(
                dRef-> new DealConfirmation(dRef,dRef,PortfolioPositionTracker.STATUS_ACEPTED));
        PortfolioPositionTracker.Position position1 = createtestPosition(positionSnapshot);
        positionTracker.trackNewPosition(position1);
        //
        String dealRef = positionSnapshot.getPositionId();
        boolean isConfirmed = positionTracker.isPositionConfirmed(dealRef);
        assertThat(isConfirmed,is(true));
    }

    @Test
    public void testTrackNewRejectedPositionWithConfirmExpiration(){
        int confirmExpiryTimeOutMili = 500;
        String positionCreatedDate = Instant.now().minusMillis(1000).atZone(Calculator.ZONE_ID_LONDON).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        PositionSnapshot positionSnapshot  = TestPositionProvider.getPositionSnapshot(positionCreatedDate);
        //
        String epic= "IX.HNG";
        String dealRef = "AASSD";
        String dealId = "dealID";
        String createdDateTime = positionSnapshot.getPositionsItem().getPosition().getCreatedDateUTC();
        double size = 1.5;
        Direction direction = Direction.BUY();
        //
        PortfolioPositionTracker positionTracker = getPortfolioPositionTracker(confirmExpiryTimeOutMili, positionSnapshot);
        positionTracker.setDealConfirmationFunction(
                dRef-> new DealConfirmation(dealRef,dealId,PortfolioPositionTracker.STATUS_REJECTED));
        PortfolioPositionTracker.Position position1 = new PortfolioPositionTracker.Position(epic,dealRef,size,direction,createdDateTime);
        positionTracker.trackNewPosition(position1);
        //
        boolean isConfirmed = positionTracker.isPositionConfirmed(dealRef);
        assertThat(isConfirmed,is(false));
        //
        List<PortfolioPositionTracker.Position> listOfPositionsOnEpic = positionTracker.getPositionsOnEpic(epic);
        assertThat(listOfPositionsOnEpic,is(empty()));
    }

    @Test
    public void testReconTrackedPositionsWithBrokerWhenBrokerPositionsAreMissing(){
        int confirmExpiryTimeOutMili=500;
        int positionReconciliationTimoutMili=5000;
        String positionCreatedDate = Instant.now().minusMillis(1000).atZone(Calculator.ZONE_ID_LONDON).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        PositionSnapshot positionSnapshot  = TestPositionProvider.getPositionSnapshot(positionCreatedDate);
        PortfolioPositionTracker positionTracker = getPortfolioPositionTracker(confirmExpiryTimeOutMili, positionSnapshot);
        positionTracker.setPositionReconciliationTimeOutMili(positionReconciliationTimoutMili);
        positionTracker.setDealConfirmationFunction(
                dRef-> new DealConfirmation(
                        positionSnapshot.getPositionsItem().getPosition().getDealReference()
                        ,positionSnapshot.getPositionsItem().getPosition().getDealId()
                        ,PortfolioPositionTracker.STATUS_ACEPTED));
        //
        String dealRef = positionSnapshot.getPositionsItem().getPosition().getDealReference();
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
        String positionCreatedDate = Instant.now().minusMillis(1000).atZone(Calculator.ZONE_ID_LONDON).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        PositionSnapshot positionSnapshot  = TestPositionProvider.getPositionSnapshot(positionCreatedDate);
        PortfolioPositionTracker positionTracker = getPortfolioPositionTracker(confirmExpiryTimeOutMili, positionSnapshot);
        positionTracker.setPositionReconciliationTimeOutMili(positionReconciliationTimoutMili);
        positionTracker.setDealConfirmationFunction(
                dRef-> new DealConfirmation(
                        positionSnapshot.getPositionsItem().getPosition().getDealReference()
                        ,positionSnapshot.getPositionsItem().getPosition().getDealId()
                        ,PortfolioPositionTracker.STATUS_REJECTED));
        //
        String epic= "IX.HNG";
        String dealRef = "AASSD";
        String dealId = "dealID";
        String createdDateTime = positionSnapshot.getPositionsItem().getPosition().getCreatedDateUTC();
        double size = 1.5;
        Direction direction = Direction.BUY();
        PortfolioPositionTracker.Position position1 = new PortfolioPositionTracker.Position(epic,dealRef,size,direction,createdDateTime);
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
        int confirmExpiryTimeOutMili=5000;
        int positionReconciliationTimoutMili=5000;
        String positionCreatedDate = Instant.now().minusMillis(1000).atZone(Calculator.ZONE_ID_LONDON).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        PositionSnapshot positionSnapshot  = TestPositionProvider.getPositionSnapshot(positionCreatedDate);
        PortfolioPositionTracker positionTracker = getPortfolioPositionTracker(confirmExpiryTimeOutMili, positionSnapshot);
        positionTracker.setPositionReconciliationTimeOutMili(positionReconciliationTimoutMili);
        positionTracker.setDealConfirmationFunction(
                dRef-> new DealConfirmation(
                        positionSnapshot.getPositionsItem().getPosition().getDealReference()
                        ,positionSnapshot.getPositionsItem().getPosition().getDealId()
                        ,PortfolioPositionTracker.STATUS_REJECTED));
        //
        String epic= "IX.HNG";
        String dealRef = "AASSD";
        String dealId = "dealID";
        String createdDateTime = positionSnapshot.getPositionsItem().getPosition().getCreatedDateUTC();
        double size = 1.5;
        Direction direction = Direction.BUY();
        PortfolioPositionTracker.Position position1 = new PortfolioPositionTracker.Position(epic,dealRef,size,direction,createdDateTime);
        positionTracker.trackNewPosition(position1);
        //
        boolean isConfirmed = positionTracker.isPositionConfirmed(dealRef);
        assertThat(isConfirmed,is(false));
        //
        boolean doesPositionExist = positionTracker.doesPositionExist(dealRef);
        assertThat(doesPositionExist,is(true));


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

    private PortfolioPositionTracker.Position createtestPosition(PositionSnapshot positionSnapshot) {
        String epic= positionSnapshot.getPositionsItem().getMarket().getEpic();
        String ref =  positionSnapshot.getPositionsItem().getPosition().getDealId();
        double size = positionSnapshot.getPositionsItem().getPosition().getSize().doubleValue();
        Direction direction = IGClientUtility.convertFromIGDirection(
            positionSnapshot.getPositionsItem().getPosition().getDirection()
        );
        String createdDateTime = positionSnapshot.getPositionsItem().getPosition().getCreatedDateUTC();

        return new PortfolioPositionTracker.Position(epic,ref,size,direction,createdDateTime);
    }
}
