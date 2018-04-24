package com.dario.agenttrader.tradingservices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class TradingAPIAuditor implements InvocationHandler{

    private final Logger API_LOGGER = LoggerFactory.getLogger("API_LOGGER");

    private final Map<String, Method> methods = new HashMap<>();
    private Object target;

    public TradingAPIAuditor(Object target){
        this.target = target;

        for(Method method: target.getClass().getDeclaredMethods()) {
            this.methods.put(method.getName(), method);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long start = System.nanoTime();
        Object result = methods.get(method.getName()).invoke(target, args);
        long elapsed = System.nanoTime() - start;

        API_LOGGER.info("API_CALL:{},args:{},result:{},exec_latency:{}", method.getName(),args,result,
                elapsed);

        return result;
    }
}
