package com.dario.agenttrader.utility;


import com.dario.agenttrader.dto.PositionInfo;
import com.dario.agenttrader.dto.PositionSnapshot;
import com.dario.agenttrader.dto.PriceTick;
import com.dario.agenttrader.dto.UpdateEvent;
import com.dario.agenttrader.marketStrategies.PositionManager;
import com.dario.agenttrader.TestPositionProvider;
import org.hamcrest.Matchers;
import org.hamcrest.number.BigDecimalCloseTo;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IGClientUtilityTest {

    @Test
    public void findMapDeltaTest(){
        Map<String,String> map1 = generateTestMap("100", "200");
        Map<String,String> map2 = generateTestMap("101", "200");
        Map<String,String[]> expectedDelta = new HashMap<>();
        expectedDelta.put("STOP", new String[]{"100","101"});

        Map<String, String[]> actualDelta = IGClientUtility.findMapDelta(map1,map2);

        assertEquals(1L, actualDelta.size());
        assertTrue(expectedDelta.keySet().equals(actualDelta.keySet()));
        assertTrue(Arrays.deepEquals(expectedDelta.get("STOP"),actualDelta.get("STOP")));

        //Test second element different

        map1 = generateTestMap("100", "201");
        map2 = generateTestMap("100", "200");
        expectedDelta = new HashMap();
        expectedDelta.put("LIMIT",new String[]{"201","200"});

        actualDelta = IGClientUtility.findMapDelta(map1,map2);

        assertEquals(1L, actualDelta.size());
        assertTrue(expectedDelta.keySet().equals(actualDelta.keySet()));
        assertTrue(Arrays.deepEquals(expectedDelta.get("LIMIT"),actualDelta.get("LIMIT")));

        //Test no difference

        map1 = generateTestMap("100", "201");
        map2 = generateTestMap("100", "201");

        actualDelta = IGClientUtility.findMapDelta(map1,map2);

        assertEquals(0, actualDelta.size());

        //Test same numerical value but different string

        map1 = generateTestMap("100", "201");
        map2 = generateTestMap("100", "201.0");

        actualDelta = IGClientUtility.findMapDelta(map1,map2);

        assertEquals(0, actualDelta.size());

        //Test empty list

        map1 = new HashMap<String,String>();
        map2 = generateTestMap("100", "200");
        expectedDelta = new HashMap<>();
        expectedDelta.put("LIMIT",new String[]{null,"200"});
        expectedDelta.put("STOP",new String[]{null,"100"});

        actualDelta = IGClientUtility.findMapDelta(map1,map2);

        assertEquals(2L, actualDelta.size());
        assertTrue(expectedDelta.keySet().equals(actualDelta.keySet()));
        assertTrue(Arrays.deepEquals(expectedDelta.get("STOP"),actualDelta.get("STOP")));
        assertTrue(Arrays.deepEquals(expectedDelta.get("LIMIT"),actualDelta.get("LIMIT")));
        //test one position with an empty value

        map1 = generateTestMap(null, "201");
        map2 = generateTestMap("100", "200");
        expectedDelta = new HashMap();
        expectedDelta.put("LIMIT",new String[]{"201","200"});
        expectedDelta.put("STOP",new String[]{null,"100"});

        actualDelta = IGClientUtility.findMapDelta(map1,map2);

        assertEquals(2L, actualDelta.size());
        assertTrue(expectedDelta.keySet().equals(actualDelta.keySet()));

    }

    private Map<String,String> generateTestMap(String stop, String limit) {
        Map<String, String> map1 = new HashMap<>();
        map1.put("STOP",stop);
        map1.put("LIMIT",limit);

        return map1;
    }

    @Test
    public void extractPositionInfTest() {
        String positionId = TestPositionProvider.DEAL_ID;
        PositionSnapshot positionSnapshot = TestPositionProvider.getPositionSnapshot();

        PositionManager.RegisterPositionRequest registerPositionRequest =
                new PositionManager.RegisterPositionRequest(positionId, positionSnapshot);

        PositionInfo positionInfoActual = IGClientUtility.extractPositionInfo(registerPositionRequest);

        Map<String,String> positionFields =  new Hashtable<>();

        positionFields.put("level" , "510.0");
        positionFields.put("limitLevel" , "602.0");
        positionFields.put("dealId" , "DIAAAABLAADV7A3");
        positionFields.put("epic" , "KA.D.BP.DAILY.IP");
        positionFields.put("controlledRisk" , "false");
        positionFields.put("trailingStopDistance" , "null");
        positionFields.put("createdDate" , "2017/11/13 08:02:44:000");
        positionFields.put("size" , "5.0");
        positionFields.put("stopLevel" , "512.0");
        positionFields.put("createdDateUTC" , "2017-11-13T08:02:44");
        positionFields.put("trailingStep" , "null");
        positionFields.put("currency" , "GBP");
        positionFields.put("contractSize" , "1.0");
        positionFields.put("expiry" , "DFB");
        positionFields.put("direction" , "BUY");

        UpdateEvent positionUpdateEvent = new UpdateEvent(positionFields,UpdateEvent.POSITION_UPDATE);

        PositionInfo positionInfoExpected = new PositionInfo(positionUpdateEvent,"0",0);

        boolean isMapEqual = areMapsEqual(positionInfoActual, positionInfoExpected);

        assertTrue(isMapEqual);



    }

    public boolean areMapsEqual(PositionInfo positionInfoActual, PositionInfo positionInfoExpected) {
        Map<String,String> map1 = positionInfoExpected.getKeyValues();
        Map<String,String> map2 = positionInfoActual.getKeyValues();

        long numberOfMatchesExpected = map1.entrySet().stream().filter(
                k -> {
                    String expectedKey = k.getKey();
                    String actualValue = map2.get(expectedKey);

                    boolean isEqual = IGClientUtility.compareStringEqualOrNull(
                        actualValue,
                        k.getValue());
                    return  isEqual;

                    }).count();
        return (map1.size() == numberOfMatchesExpected) && (map2.size() == numberOfMatchesExpected);
    }


    @Test
    public void testJsonFlatter() {
        Map<String, String> flattenedOPU = IGClientUtility.flatJSontoMap(TestPositionProvider.OPU_MESSAGE);
        assertNotNull(flattenedOPU);
        String dealId = flattenedOPU.get(PositionInfo.DEAL_ID_KEY);
        assertEquals(TestPositionProvider.DEAL_ID, dealId);
    }

    @Test
    public void testConvertCommaSeparatedStringToArrayList(){
        String updateStr = "[ 1016.73, 1028.73, null, 1, 1516227922478, 1051.25, -2.71, 1076.1, 1005.6 ]";

        List<String> updateList = IGClientUtility.convertCommaSeparatedStringToArrayList(updateStr);

        assertEquals(9,updateList.size());
        assertEquals(updateList.get(0),"1016.73");
        assertEquals(updateList.get(1),"1028.73");
        assertEquals(updateList.get(2),"null");
        assertEquals(updateList.get(3),"1");
        assertEquals(updateList.get(4),"1516227922478");
        assertEquals(updateList.get(5),"1051.25");
        assertEquals(updateList.get(6),"-2.71");
        assertEquals(updateList.get(7),"1076.1");
        assertEquals(updateList.get(8),"1005.6");

    }

    @Test
    public void testExtractMarketPriceTick(){
        String updateStr = "[ 1016.73, 1028.73, null, 1, 1516227922478, 1051.25, -2.71, 1076.1, 1005.6 ]";
        PriceTick expectedPriceTick = new PriceTick(
                new Double(1016.73)
                ,new Double(1028.73)
                ,null
                ,new Double(1)
                ,"1516227922478"
                ,new Double( 1051.25)
                ,new Double( -2.71)
                ,new Double( 1076.1)
                ,new Double( 1005.6)
        );

        BigDecimal error =  BigDecimal.valueOf(0);
        PriceTick actualPriceTick = IGClientUtility.extractMarketPriceTick(updateStr);
        assertThat(expectedPriceTick.getBid(), closeTo(actualPriceTick.getBid(),error));
        assertThat(expectedPriceTick.getBid(),closeTo(actualPriceTick.getBid(),error));
        assertThat(expectedPriceTick.getOffer(),closeTo(actualPriceTick.getOffer(),error));
        assertThat(expectedPriceTick.getDayLow(),closeTo(actualPriceTick.getDayLow(),error));
        assertThat(expectedPriceTick.getDayHigh(),closeTo(actualPriceTick.getDayHigh(),error));
        assertThat(expectedPriceTick.getDayOpenMid(),closeTo(actualPriceTick.getDayOpenMid(),error));
        assertThat(expectedPriceTick.getDayPercentageChangeMid(),closeTo(actualPriceTick.getDayPercentageChangeMid(),error));
        assertThat(expectedPriceTick.getGetLastTradeVolume(),closeTo(actualPriceTick.getGetLastTradeVolume(),error));
        assertNull(actualPriceTick.getLastTradePrice());
        assertEquals(expectedPriceTick.getUtm(),actualPriceTick.getUtm());

    }
}
