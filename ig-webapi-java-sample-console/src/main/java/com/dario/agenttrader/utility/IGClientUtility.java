package com.dario.agenttrader.utility;

import com.dario.agenttrader.dto.PositionInfo;
import com.dario.agenttrader.marketStrategies.PositionManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Market;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Position;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IGClientUtility {

    static final ObjectMapper mapper = new ObjectMapper();

    static final Pattern numbericPatternChecker = Pattern.compile("^[-+]?\\d+(\\.\\d+)?$");

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

        if(!isEqual && str1!=null && str2!=null){
            boolean isStr1Numeric = numbericPatternChecker.matcher(str1).matches();
            boolean isStr2Numeric = numbericPatternChecker.matcher(str2).matches();

            isEqual = (isStr1Numeric && isStr2Numeric)? Double.parseDouble(str1)==Double.parseDouble(str2):isEqual;
        }

        return isEqual;
    }

    public static PositionInfo extractPositionInfo(PositionManager.RegisterPositionRequest
                                                           registerPositionRequest) {
        Map<String,String> positionFields = new HashMap<>();
        Position position = registerPositionRequest.getPositionSnapshot().getPositionsItem().getPosition();
        Market market = registerPositionRequest.getPositionSnapshot().getPositionsItem().getMarket();

        positionFields.put(PositionInfo.CREATED_DATE_KEY,position.getCreatedDate());
        positionFields.put(PositionInfo.CREATED_DATE_UTC_KEY,position.getCreatedDateUTC());
        positionFields.put(PositionInfo.CURRENCY_KEY,position.getCurrency());
        positionFields.put(PositionInfo.DEAL_ID_KEY,position.getDealId());
        positionFields.put(PositionInfo.CONTRACT_SIZE_KEY,String.valueOf(position.getContractSize()));
        positionFields.put(PositionInfo.CONTROLLED_RISK_KEY,String.valueOf(position.getControlledRisk()));
        positionFields.put(PositionInfo.DIRECTION_KEY,String.valueOf(position.getDirection()));
        positionFields.put(PositionInfo.LEVEL_KEY,String.valueOf(position.getLevel()));
        positionFields.put(PositionInfo.LIMIT_LEVEL_KEY,String.valueOf(position.getLimitLevel()));
        positionFields.put(PositionInfo.SIZE_KEY,String.valueOf(position.getSize()));
        positionFields.put(PositionInfo.STOP_LEVEL_KEY,String.valueOf(position.getStopLevel()));
        positionFields.put(PositionInfo.TRAILING_STEP_KEY,String.valueOf(position.getTrailingStep()));
        positionFields.put(PositionInfo.TRAILING_STOP_DISTANCE_KEY,String.valueOf(position.getTrailingStopDistance()));
        positionFields.put(PositionInfo.EPIC_KEY,market.getEpic());
        positionFields.put(PositionInfo.EXPIRY_KEY,market.getExpiry());


        return new PositionInfo(positionFields,"0",0);
    }
}