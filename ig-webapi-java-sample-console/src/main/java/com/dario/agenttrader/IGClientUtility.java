package com.dario.agenttrader;

import com.dario.agenttrader.dto.PositionUpdate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.lightstreamer.ls_client.UpdateInfo;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

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
}
