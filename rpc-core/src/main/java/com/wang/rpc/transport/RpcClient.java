package com.wang.rpc.transport;

import com.wang.rpc.entity.RpcRequest;
import com.wang.rpc.serializer.CommonSerializer;

/**
 * 客户端类通用接口serializer
 */
public interface RpcClient {

    int DEFAULT_SERIALIZER = CommonSerializer.KRYO_SERIALIZER;

    Object sendRequest(RpcRequest rpcRequest);

}
