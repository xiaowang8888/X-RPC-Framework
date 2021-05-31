package com.wang.server;


import com.wang.rpc.annotation.Service;
import com.wang.rpc.api.ByeService;

/**
 *
 */
@Service
public class ByeServiceImpl implements ByeService {

    @Override
    public String bye(String name) {
        return "bye, " + name;
    }
}
