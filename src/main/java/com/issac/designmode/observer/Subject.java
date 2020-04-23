package com.issac.designmode.observer;

public interface Subject {

    public void registSubject(ObServer obServer);

    public void deleteSubject(ObServer obServer);

    public void noticObServer();
}
