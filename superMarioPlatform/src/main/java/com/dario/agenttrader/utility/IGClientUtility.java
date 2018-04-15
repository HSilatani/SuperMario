package com.dario.agenttrader.utility;

import com.dario.agenttrader.domain.Direction;
import com.dario.agenttrader.dto.PriceCandle;
import com.dario.agenttrader.dto.PriceTick;
import com.dario.agenttrader.dto.UpdateEvent;
import com.dario.agenttrader.dto.PositionInfo;
import com.dario.agenttrader.marketStrategies.PositionManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Market;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Position;
import com.iggroup.webapi.samples.client.rest.dto.prices.getPricesV3.PricesItem;
import com.lightstreamer.ls_client.UpdateInfo;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
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
    public static Map<String,String> flatConfirmMessageMap(String json){
        ObjectReader reader = mapper.reader();

        Map<String,String> mapToReturn = null;
        List<Map<String,Object>> list = null;
        Map<String,Object> tmpMap = null;

        try {
            Object obj = reader.forType(new TypeReference<Object>(){}).readValue(json);
            list = (obj instanceof List)?(List)obj:new ArrayList<>();
            tmpMap = (obj instanceof Map)?(Map)obj:list.get(0);

        } catch (IOException e)
        {
            e.printStackTrace();
        }

        mapToReturn = tmpMap.entrySet()
                    .stream()
                    .filter(e->e.getValue() instanceof String)
                    .collect(Collectors.toMap(e->e.getKey(),e->(String)e.getValue()));

        return mapToReturn;
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
            boolean isStr1Numeric = Calculator.isStrNumericValue(str1);
            boolean isStr2Numeric = Calculator.isStrNumericValue(str2);

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

        UpdateEvent positionUpdateEvent = new UpdateEvent(positionFields,UpdateEvent.POSITION_UPDATE);

        return new PositionInfo(positionUpdateEvent,"0",0);
    }




    


    public static List<String> convertCommaSeparatedStringToArrayList(String updateStr) {
        updateStr= updateStr.replaceAll("[\\[\\s\\]]","");
        List<String> items = Arrays.asList(updateStr.split("\\s*,\\s*"));
        return items;
    }

    public static void updateBaseMap(Map<String, String> baseMap, Map<String, String> toMerge) {
        baseMap.entrySet().stream()
                .forEach(e->{
                    if(!Calculator.isStrNumericValue(e.getValue())){
                        e.setValue(toMerge.get(e.getKey()));
                    }
                });
    }

    public static PriceTick extractMarketPriceTick(UpdateInfo updateInfo) {
        return extractMarketPriceTick(updateInfo.toString());
    }

    public static PriceTick extractMarketPriceTick(String strUpdateInfo) {
        List<String> items = convertCommaSeparatedStringToArrayList(strUpdateInfo);
        PriceTick priceTick = new PriceTick(
                Calculator.convertStrToBigDecimal(items.get(0)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(1)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(2)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(3)).orElse(null)
                ,items.get(4)
                ,Calculator.convertStrToBigDecimal(items.get(5)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(6)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(7)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(8)).orElse(null)
        );
        return priceTick;
    }
    public static PriceCandle extractMarketPriceCandle(UpdateInfo updateInfo) {
        return extractMarketPriceCandle(updateInfo.toString());
    }
    public static PriceCandle extractMarketPriceCandle(String strUpdateInfo) {
        List<String> items = convertCommaSeparatedStringToArrayList(strUpdateInfo);
        PriceCandle priceCandle = new PriceCandle(
                 Calculator.convertStrToBigDecimal(items.get(0)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(1)).orElse(null)
                ,items.get(2)
                ,Calculator.convertStrToBigDecimal(items.get(3)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(4)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(5)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(6)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(7)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(8)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(9)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(10)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(11)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(12)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(13)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(14)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(15)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(16)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(17)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(18)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(19)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(20)).orElse(null)
                ,Calculator.convertStrToBigDecimal(items.get(21)).orElse(null)
        );
        return priceCandle;
    }

    public static PriceCandle extractMarketPriceCandle(PricesItem pricesItem) {
        PriceCandle priceCandle = new PriceCandle(
                new BigDecimal(pricesItem.getLastTradedVolume())
                , pricesItem.getSnapshotTimeUTC()
                , pricesItem.getOpenPrice().getAsk()
                , pricesItem.getHighPrice().getAsk()
                , pricesItem.getLowPrice().getAsk()
                , pricesItem.getClosePrice().getAsk()
                , pricesItem.getOpenPrice().getBid()
                , pricesItem.getHighPrice().getBid()
                , pricesItem.getLowPrice().getBid()
                , pricesItem.getClosePrice().getBid()
        );
        return priceCandle;
    }

    public static Direction convertFromIGDirection(
            com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Direction igDirection){
        if(igDirection.name().equalsIgnoreCase("buy")){
            return Direction.BUY();
        }else if(igDirection.name().equalsIgnoreCase("sell")){
            return Direction.SELL();
        }else{
            throw new IllegalArgumentException("Invalid Direction ["+igDirection.name()+"]");
        }
    }
}
