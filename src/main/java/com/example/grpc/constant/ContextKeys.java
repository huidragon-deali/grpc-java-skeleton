package com.example.grpc.constant;

import io.grpc.Context;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class ContextKeys {

    // 인스턴스로 생성되어있어야 Context 키에 대한 밸류를 얻어올수 있다,
    public Context.Key<String> APP_TRANSACTION = Context.key("app-transaction");
    public Context.Key<String> METHOD = Context.key("grpc-request-method");
    public Context.Key<Long> REQUEST_START_TIME = Context.key("request-time");

    public void transactionIdToMDC() {
        String appId = APP_TRANSACTION.get();
        MDC.put("app-transaction", appId);
    }
}
