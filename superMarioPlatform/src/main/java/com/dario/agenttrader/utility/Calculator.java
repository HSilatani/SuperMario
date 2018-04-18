package com.dario.agenttrader.utility;

import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Direction;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.PositionsItem;
import com.iggroup.webapi.samples.client.rest.dto.prices.getPricesByNumberOfPointsV2.GetPricesByNumberOfPointsV2Response;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import java.util.regex.Pattern;

public class Calculator {

    static final Pattern numbericPatternChecker = Pattern.compile("^[-+]?\\d+(\\.\\d+)?$");
    public static final ZoneId ZONE_ID_LONDON = ZoneId.of("Europe/London");

    public static boolean isStrNumericValue(String str){
        boolean isStr1Numeric = numbericPatternChecker.matcher(str).matches();
        return  isStr1Numeric;
    }
    public static ZonedDateTime zonedDateTimeFromString(String dateTime) {
        boolean isNumeric = false;
        Optional<BigDecimal> decimalValue = Calculator.convertStrToBigDecimal(dateTime);
        if(decimalValue.isPresent()){
            isNumeric = true;
        }
        Long epochTimeLong = null;
        ZonedDateTime zonedDateTime = null;

        if(isNumeric){
            epochTimeLong = decimalValue.get().longValue();
            Instant fromEpochMilli = Instant.ofEpochMilli(Long.valueOf(epochTimeLong));
            zonedDateTime = fromEpochMilli.atZone(ZoneId.of("UTC"));
        }

        if(zonedDateTime == null){
            LocalDateTime localDateTime = LocalDateTime.parse(dateTime);
            zonedDateTime = localDateTime.atZone(ZoneId.of("UTC"));
        }

        return zonedDateTime;
    }


    public BigDecimal calPandL(PositionsItem position, GetPricesByNumberOfPointsV2Response prices) throws Exception {
        BigDecimal openlevel = position.getPosition().getLevel();
        Direction direction = position.getPosition().getDirection();
        BigDecimal size = position.getPosition().getSize();

        BigDecimal priceDiff = calculatePriceDifference(direction,openlevel ,prices);

        BigDecimal pAndL= priceDiff.multiply(size);


        return  pAndL;
    }


    private BigDecimal calculatePriceDifference(Direction direction,
                                               BigDecimal openLevel,
                                               GetPricesByNumberOfPointsV2Response prices) throws Exception {

        if (prices.getPrices().size()<1){
            throw new Exception("Price not Found");
        }
        if(direction.name().equalsIgnoreCase("buy")){

            return prices.getPrices().get(0).getClosePrice().getBid().subtract(openLevel);

        }else if(direction.name().equalsIgnoreCase("sell")){
            return openLevel.subtract(prices.getPrices().get(0).getClosePrice().getAsk());

        }
        throw new Exception("Price not found");
    }

    public BigDecimal calPriceMove(GetPricesByNumberOfPointsV2Response prices) throws Exception{

        if (prices.getPrices().size()<1){
            throw new Exception("Price not Found");
        }

        BigDecimal closePrice = prices.getPrices().get(0).getClosePrice().getAsk();
        BigDecimal openPrice = prices.getPrices().get(0).getOpenPrice().getAsk();


        return closePrice.subtract(openPrice);
    }

    public static Optional<BigDecimal> convertStrToBigDecimal(Optional<String> strValue) {
        BigDecimal doubleValue = null;
        if(strValue.isPresent()) {
            String cleanedStr = strValue.get().replaceAll("\\(", "");
            cleanedStr = cleanedStr.replaceAll("\\)","");
            cleanedStr = cleanedStr.replaceAll("'","");
            if (isStrNumericValue(cleanedStr)) {
                doubleValue = new BigDecimal(cleanedStr);
            }
        }
        return Optional.ofNullable(doubleValue);
    }

    public static Optional<BigDecimal> convertStrToBigDecimal(String strValue) {
        return  convertStrToBigDecimal(Optional.ofNullable(strValue));
    }
}
