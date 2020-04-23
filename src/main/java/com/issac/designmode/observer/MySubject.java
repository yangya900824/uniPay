package com.issac.designmode.observer;

import java.util.ArrayList;

public class MySubject implements Subject {

    public ArrayList observers=new ArrayList<>();

    public MySubject(ObServer obServer) {
    }

    @Override
    public void registSubject(ObServer obServer) {
        observers.add(obServer);
    }

    @Override
    public void deleteSubject(ObServer obServer) {
        boolean contains = observers.contains(obServer);
        if (contains){
            observers.remove(obServer);
        }
    }

    @Override
    public void noticObServer() {
    }

}
