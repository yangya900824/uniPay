package com.issac.designmode.observer;


import com.issac.config.SwiftpassConfig;

public class YangyaObServer implements ObServer {

    private String userName;


    @Override
    public void accept(String msg) {
        SwiftpassConfig config = new SwiftpassConfig();
    }
}
