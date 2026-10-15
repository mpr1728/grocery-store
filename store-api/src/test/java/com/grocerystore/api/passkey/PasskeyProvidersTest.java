package com.grocerystore.api.passkey;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.yubico.webauthn.data.ByteArray;

class PasskeyProvidersTest {

    @Test
    void knownProvidersAreNamedAndUnknownOnesAreLeftToTheBrowserLabel() throws Exception {
        assertThat(PasskeyProviders.nameOf(ByteArray.fromHex("fbfc3007154e4ecc8c0b6e020557d7bd"))).contains("iCloud Keychain");
        assertThat(PasskeyProviders.nameOf(ByteArray.fromHex("ea9b8d664d011d213ce4b6b48cb575d4"))).contains("Google Password Manager");
        // An all-zero AAGUID says nothing about where the passkey lives.
        assertThat(PasskeyProviders.nameOf(ByteArray.fromHex("00000000000000000000000000000000"))).isEmpty();
        assertThat(PasskeyProviders.nameOf(null)).isEmpty();
    }
}
