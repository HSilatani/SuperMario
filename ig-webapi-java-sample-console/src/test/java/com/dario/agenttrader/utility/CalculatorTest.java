package com.dario.agenttrader.utility;


import com.dario.agenttrader.dto.PriceTick;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZonedDateTime;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class CalculatorTest {

    @Test
    public void testZonedDateTimeFromString() {
        String utmToParse = "1520535600000";
        String utmToParseWithSingleQuote = "'(1520535600000)'";
        String utmToParseWithoutSingleQuote = "(1520535600000)";
        String localDatTimeToParse = "2018-03-08T19:00:00";


        LocalDateTime expectedLocalDateTime = LocalDateTime.of(2018, Month.MARCH, 8, 19, 0,0);
        ZonedDateTime expectedZonedDateTime = expectedLocalDateTime.atZone(Calculator.ZONE_ID_LONDON);
        //
        ZonedDateTime zonedLocalDateActual = Calculator.zonedDateTimeFromString(localDatTimeToParse);
        assertThat(zonedLocalDateActual,equalTo(expectedZonedDateTime));
        //
        ZonedDateTime zonedLocalDateActualUTM = Calculator.zonedDateTimeFromString(utmToParse);
        assertThat(zonedLocalDateActualUTM,equalTo(expectedZonedDateTime));
        //
        ZonedDateTime zonedLocalDateActualUTMWithoutSingleQuote = Calculator.zonedDateTimeFromString(utmToParseWithoutSingleQuote);
        assertThat(zonedLocalDateActualUTMWithoutSingleQuote,equalTo(expectedZonedDateTime));
        //
        ZonedDateTime zonedLocalDateActualUTMSingleQuote = Calculator.zonedDateTimeFromString(utmToParseWithSingleQuote);
        assertThat(zonedLocalDateActualUTMSingleQuote,equalTo(expectedZonedDateTime));
        //
    }
}
