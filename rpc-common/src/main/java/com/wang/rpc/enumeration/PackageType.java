package com.wang.rpc.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 发送的协议包类型
 */
@AllArgsConstructor
@Getter
public enum PackageType {

    REQUEST_PACK(0),
    RESPONSE_PACK(1);

    private final int code;
}
