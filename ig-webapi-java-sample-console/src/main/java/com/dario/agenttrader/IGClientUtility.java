package com.dario.agenttrader;

import com.dario.agenttrader.dto.PositionInfo;
import com.dario.agenttrader.marketStrategies.PositionManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class IGClientUtility {

    static final ObjectMapper mapper = new ObjectMapper();

    public static Locale findLocalForCurrency(String currency){
        Locale  locale = Locale.getDefault();

        if("GBP".equalsIgnoreCase(currency)){
            locale=Locale.UK;
        }

        return locale;
    }

    public static Map<String,String> flatJSontoMap(String json){
        ObjectReader reader = mapper.reader();
        Map<String,String> map = null;
        try {
            map = reader.forType(new TypeReference<Map<String, String>>(){}).readValue(json);
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return map;
    }

    public static Map<String,String[]> findDelta(PositionInfo newPositionInfo, PositionInfo positionInfo) {
        Optional<PositionInfo> optPositionInfo = Optional.ofNullable(positionInfo);
        Map<String,String> oldInfo = optPositionInfo.isPresent()?optPositionInfo.get().getKeyValues():new HashMap<>();

        Map<String,String> newInfo = newPositionInfo.getKeyValues();
        Map<String,String[]> delta = findMapDelta(oldInfo,newInfo);
        return delta;
    }

    public static Map<String,String[]> findMapDelta(Map<String,String> map1, Map<String,String> map2){
        Map<String,String> oldInfo = map1;
        Map<String,String[]> delta = map2.entrySet().stream()
                .filter(entry -> !compareStringEqualOrNull(entry.getValue(),oldInfo.get(entry.getKey())))
                .collect(
                        Collectors.toMap(e->e.getKey(),e->{return new String[]{oldInfo.get(e.getKey()),e.getValue()};})
                );


        return delta;
    }

    public static boolean compareStringEqualOrNull(String str1,String str2){
        boolean isEqual = false;

        isEqual= (str1==null)?str2==null:str1.equalsIgnoreCase(str2);

        return isEqual;
    }

    public static PositionInfo extractPositionInfo(PositionManager.RegisterPositionRequest
                                                           registerPositionRequest) {
        //TODO implement
        return new PositionInfo(new HashMap<>(),"0",0);
    }
}
