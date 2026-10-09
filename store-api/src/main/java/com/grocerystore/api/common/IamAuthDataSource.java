package com.grocerystore.api.common;

import java.net.URI;

import com.zaxxer.hikari.HikariDataSource;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.rds.RdsUtilities;

/**
 * Connection pool for a database that signs users in with IAM instead of a stored password. The pool asks
 * for the password each time it opens a connection, and gets a fresh token that is valid for 15 minutes.
 */
public class IamAuthDataSource extends HikariDataSource {

    private final RdsUtilities rds = RdsUtilities.builder()
            .region(DefaultAwsRegionProviderChain.builder().build().getRegion())
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();

    @Override
    public String getPassword() {
        // "jdbc:postgresql://host:5432/name?..." without the "jdbc:" prefix parses as an ordinary address.
        URI database = URI.create(getJdbcUrl().substring("jdbc:".length()));
        return rds.generateAuthenticationToken(token -> token
                .hostname(database.getHost())
                .port(database.getPort())
                .username(getUsername()));
    }
}
