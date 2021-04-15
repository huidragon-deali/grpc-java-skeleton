package com.example.grpc.interceptor;


import com.example.grpc.constant.ContextKeys;
import io.grpc.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GrpcInterceptor implements ServerInterceptor {

    final ContextKeys keys;

    // 클라이언트에서 전송한 Metadata에서 "app-transaction"을 추출
    public String getTransactionId(Metadata metadata) {
        Metadata.Key<String> key = Metadata.Key.of("app-transaction",Metadata.ASCII_STRING_MARSHALLER);
        String transactionId = metadata.get(key);
        return !ObjectUtils.isEmpty(transactionId) ? transactionId : UUID.randomUUID().toString();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata,
                                                                 ServerCallHandler<ReqT, RespT> serverCallHandler) {

        // grpc 하나의 요청 라이프 사이클에서 사용할 Context 데이터를 설정
        Context ctx = Context.current();
        ctx = ctx.withValue(keys.APP_TRANSACTION, getTransactionId(metadata));
        ctx = ctx.withValue(keys.METHOD, serverCall.getMethodDescriptor().getFullMethodName());
        ctx = ctx.withValue(keys.REQUEST_START_TIME, System.currentTimeMillis());

        // Contexts.interceptCall api를 사용하면 Context 객체의 일관성을 유지할수 있다.
        ServerCall.Listener<ReqT> listener = Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);

        // MDC사용을 위한 wapper
        return new ServerCallListener(listener, ctx);
    }

    // grpc는 어떤 쓰레드가 로직을 수행할지 알수가 없으므로, 내부적으로 MDC를 사용하는 경우 Context에서 MDC로 데이터를 복사해 주어야 하는 로직이 필요
    class ServerCallListener<ReqT> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        private final Context context;

        protected ServerCallListener(ServerCall.Listener<ReqT> listener, Context ctx) {
            super(listener);
            this.context = ctx;
        }

        // 실제 구현체를 호출하기 전 이벤트
        @Override
        public void onHalfClose() {

            Context previous = context.attach();
            keys.transactionIdToMDC();
            try {
                super.onHalfClose();
            } finally {
                context.detach(previous);
            }
        }
    }
}