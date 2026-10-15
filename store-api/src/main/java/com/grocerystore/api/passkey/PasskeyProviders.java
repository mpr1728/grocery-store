package com.grocerystore.api.passkey;

import java.util.Map;
import java.util.Optional;

import com.yubico.webauthn.data.ByteArray;

/**
 * Names for common places a passkey is kept, found from the authenticator's AAGUID, so a customer can tell their
 * passkeys apart ("iCloud Keychain", "Google Password Manager"). Some browsers send an all-zero AAGUID; then the
 * label from the browser is used instead. Source: the community list of passkey provider AAGUIDs.
 */
final class PasskeyProviders {

    private static final Map<String, String> NAMES = Map.of(
            "fbfc3007154e4ecc8c0b6e020557d7bd", "iCloud Keychain",
            "ea9b8d664d011d213ce4b6b48cb575d4", "Google Password Manager",
            "adce000235bcc60a648b0b25f1f05503", "Chrome on Mac",
            "08987058cadc4b81b6e130de50dcbe96", "Windows Hello",
            "9ddd1817af5a4672a2b93e3dd95000a9", "Windows Hello",
            "6028b017b1d44c02b4b3afcdafc96bb2", "Windows Hello",
            "bada5566a7aa401fbd9645619a55120d", "1Password",
            "d548826e79b4db40a3d811116f7e8349", "Bitwarden",
            "53414d53554e47000000000000000000", "Samsung Pass");

    private PasskeyProviders() {
    }

    static Optional<String> nameOf(ByteArray aaguid) {
        return aaguid == null ? Optional.empty() : Optional.ofNullable(NAMES.get(aaguid.getHex()));
    }
}
