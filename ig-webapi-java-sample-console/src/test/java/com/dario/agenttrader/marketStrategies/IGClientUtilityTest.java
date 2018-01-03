package com.dario.agenttrader.marketStrategies;


import com.dario.agenttrader.IGClientUtility;
import org.junit.Test;

import java.util.*;

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
        assertTrue("Not implemented",false);
    }
}
