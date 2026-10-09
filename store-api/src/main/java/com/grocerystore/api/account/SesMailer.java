package com.grocerystore.api.account;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.grocerystore.api.common.StoreProperties;

import software.amazon.awssdk.services.sesv2.SesV2Client;

/** Sends through Amazon SES. The sender address must be a verified identity there. */
@Component
@ConditionalOnExpression("!'${store.mail-from:}'.isEmpty()")
public class SesMailer implements Mailer {

    private final SesV2Client ses;
    private final String from;

    public SesMailer(StoreProperties properties) {
        this.from = properties.brandName() + " <" + properties.mailFrom() + ">";
        this.ses = SesV2Client.create();
    }

    @Override
    public void send(String to, String subject, String text) {
        ses.sendEmail(r -> r
                .fromEmailAddress(from)
                .destination(d -> d.toAddresses(to))
                .content(c -> c.simple(m -> m
                        .subject(s -> s.data(subject).charset("UTF-8"))
                        .body(b -> b.text(t -> t.data(text).charset("UTF-8"))))));
    }
}
