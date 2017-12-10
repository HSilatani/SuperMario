package com.dario.agenttrader;

import java.util.Locale;

public class IGClientUtility {

    public static Locale findLocalForCurrency(String currency){
        Locale  locale = Locale.getDefault();

        if("GBP".equalsIgnoreCase(currency)){
            locale=Locale.UK;
        }

        return locale;
    }
}
