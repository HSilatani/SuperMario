package com.dario.agenttrader.utility;


import com.dario.agenttrader.dto.PositionInfo;
import com.dario.agenttrader.dto.PositionSnapshot;
import com.dario.agenttrader.marketStrategies.PositionManager;
import com.dario.agenttrader.TestPositionProvider;
import org.junit.Test;

import java.util.*;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
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

        PositionInfo positionInfoExpected = new PositionInfo(positionFields,"0",0);

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
}
