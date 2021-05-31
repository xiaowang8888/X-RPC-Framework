package com.wang.server;


import com.wang.rpc.annotation.ServiceScan;
import com.wang.rpc.serializer.CommonSerializer;
import com.wang.rpc.transport.RpcServer;
import com.wang.rpc.transport.socket.server.SocketServer;

/**
 * 测试用服务提供方（服务端）
 *
 */
@ServiceScan
public class SocketTestServer {

    public static void main(String[] args) {
        RpcServer server = new SocketServer("127.0.0.1", 9998, CommonSerializer.HESSIAN_SERIALIZER);
        server.start();
    }

}
