package com.wang.rpc.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 为序列化方式编号
 */
@AllArgsConstructor
@Getter
public enum SerializerCode {

    KRYO(0),
    JSON(1),
    HESSIAN(2),
    PROTOBUF(3);

    private final int code;
}
