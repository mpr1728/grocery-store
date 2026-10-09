package com.grocerystore.api.account;

public interface Mailer {

    void send(String to, String subject, String text);
}
