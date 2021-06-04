# X-RPC-Framework
X-RPC-Framework 是一款基于 Nacos 实现的 RPC 框架，网络传输实现了基于 Java 原生 Socket 与 Netty 版本，并且实现了多种序列化方式（Json、Kryo 、Hessian 、Protobuf ）与负载均衡算法。

## 架构

![image-20210531135611990](http://guli-edu-avator1.oss-cn-shenzhen.aliyuncs.com/picBed/image-20210531135611990.png)

服务提供者 Provider 向注册中心 Nacos 注册服务，服务消费者 Consumer 通过注册中心拿到服务相关信息，然后再通过网络请求服务提供者 Provider。服务消费者调用服务提供者的方式取决于服务消费者的客户端选择，如选用原生 Socket 则该步调用使用 BIO，如选用 Netty 方式则该步调用使用 NIO。如该调用有返回值，则服务提供者向服务消费者发送返回值的方式同理。

## 设计思路

![image-20210531140954008](http://guli-edu-avator1.oss-cn-shenzhen.aliyuncs.com/picBed/image-20210531140954008.png)

1. **注册中心** ：注册中心负责服务地址的注册与查找，相当于目录服务。服务端启动的时候将服务名称及其对应的地址(ip+port)注册到注册中心，服务消费端根据服务名称找到对应的服务地址。有了服务地址之后，服务消费端就可以通过网络请求服务端了。Nacos、Zookeeper都可作为注册中心。
2. **网络传输** ：既然要调用远程的方法就要发请求，请求中至少要包含你调用的类名、方法名以及相关参数吧！基于 BIO 的 Java 原生 Socket 和基于 NIO 的 Netty 框架均可实现网络传输。
3. **序列化** ：既然涉及到网络传输就一定涉及到序列化，你不可能直接使用 JDK 自带的序列化吧！JDK 自带的序列化效率低并且有安全漏洞。 所以，你还要考虑使用哪种序列化协议，比较常用的有Json、Kryo 、Hessian 、Protobuf。
4. **动态代理** ：因为 RPC 的主要目的就是让我们调用远程方法像调用本地方法一样简单，使用动态代理可以屏蔽远程方法调用的细节比如网络传输。也就是说当你调用远程方法的时候，实际会通过代理对象来传输网络请求，不然的话，怎么可能直接就调用到远程方法呢？
5. **负载均衡** ：负载均衡也是需要的。为啥？举个例子我们的系统中的某个服务的访问量特别大，我们将这个服务部署在了多台服务器上，当客户端发起请求的时候，多台服务器都可以处理这个请求。那么，如何正确选择处理该请求的服务器就很关键。假如，你就要一台服务器来处理该服务的请求，那该服务部署在多台服务器的意义就不复存在了。负载均衡就是为了避免单个服务器响应同一请求，容易造成服务器宕机、崩溃等问题，我们从负载均衡的这四个字就能明显感受到它的意义。

## 特性

- 实现了基于 Java 原生 Socket 传输与 Netty 传输两种网络传输方式
- 实现了四种序列化算法，Json 方式、Kryo 算法、Hessian 算法与 Google Protobuf 方式（默认采用 Kryo方式序列化）
- 实现了两种负载均衡算法：随机算法与轮转算法
- 使用 Nacos 作为注册中心，管理服务提供者信息
- 消费端如采用 Netty 方式，会复用 Channel 避免多次连接
- 如消费端和提供者都采用 Netty 方式，会采用 Netty 的心跳机制，保证连接
- 接口抽象良好，模块耦合度低，网络传输、序列化器、负载均衡算法可配置
- 实现自定义的通信协议
- 服务提供侧自动注册服务

## 项目模块概览

- **rpc-api** —— 通用接口
- **rpc-common** —— 实体对象、工具类等公用类
- **rpc-core** —— 框架的核心实现
- **test-client** —— 测试用消费侧
- **test-server** —— 测试用提供侧

## 传输协议（XRF协议）

调用参数与返回值的传输采用了如下 XRF 协议（ X-RPC-Framework 首字母）以防止粘包：

```
+---------------+---------------+-----------------+-------------+
|  Magic Number |  Package Type | Serializer Type | Data Length |
|    4 bytes    |    4 bytes    |     4 bytes     |   4 bytes   |
+---------------+---------------+-----------------+-------------+
|                          Data Bytes                           |
|                   Length: ${Data Length}                      |
+---------------------------------------------------------------+
```

| 字段            | 解释                                                         |
| --------------- | ------------------------------------------------------------ |
| Magic Number    | 魔数，表识一个 XRF 协议包，0xCAFEBABE                        |
| Package Type    | 包类型，标明这是一个调用请求还是调用响应                     |
| Serializer Type | 序列化器类型，标明这个包的数据的序列化方式                   |
| Data Length     | 数据字节的长度                                               |
| Data Bytes      | 传输的对象，通常是一个`RpcRequest`或`RpcClient`对象，取决于`Package Type`字段，对象的序列化方式取决于`Serializer Type`字段。 |

## 使用

### 定义调用接口

```java
package com.wang.rpc.api;

public interface HelloService {
    String hello(HelloObject object);
}
```

### 在服务提供侧实现该接口

```java
package com.wang.server;

import com.wang.rpc.annotation.Service;
import com.wang.rpc.api.HelloObject;
import com.wang.rpc.api.HelloService;

@Service
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(HelloObject object) {
        return "这是Impl1方法";
    }
}
```

### 编写服务提供者

```java
package com.wang.server;

import com.wang.rpc.annotation.ServiceScan;
import com.wang.rpc.serializer.CommonSerializer;
import com.wang.rpc.transport.RpcServer;
import com.wang.rpc.transport.netty.server.NettyServer;

@ServiceScan
public class NettyTestServer {
    public static void main(String[] args) {
        NettyServer server = new NettyServer("127.0.0.1", 9999, CommonSerializer.PROTOBUF_SERIALIZER);
        server.start();
    }
}
```

这里选用 Netty 传输方式，并且指定序列化方式为 Google Protobuf 方式。

### 在服务消费侧远程调用

```java
package com.wang.client;

import com.wang.rpc.api.ByeService;
import com.wang.rpc.api.HelloObject;
import com.wang.rpc.api.HelloService;
import com.wang.rpc.serializer.CommonSerializer;
import com.wang.rpc.transport.RpcClient;
import com.wang.rpc.transport.RpcClientProxy;
import com.wang.rpc.transport.netty.client.NettyClient;

public class NettyTestClient {

    public static void main(String[] args) {
        RpcClient client = new NettyClient(CommonSerializer.PROTOBUF_SERIALIZER);
        RpcClientProxy rpcClientProxy = new RpcClientProxy(client);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        HelloObject object = new HelloObject(12, "This is a message");
        String res = helloService.hello(object);
        System.out.println(res);
        ByeService byeService = rpcClientProxy.getProxy(ByeService.class);
        System.out.println(byeService.bye("Netty"));
    }
}
```

这里客户端也选用了 Netty 的传输方式，序列化方式采用 Google Protobuf方式，负载均衡策略指定为轮转方式。

### 启动

在此之前请确保 Nacos 运行在本地 `8848` 端口。

首先启动服务提供者，再启动服务消费者。
