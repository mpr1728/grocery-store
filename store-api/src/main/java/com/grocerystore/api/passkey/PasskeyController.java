package com.grocerystore.api.passkey;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.api.account.AccountDtos.SignedIn;
import com.grocerystore.api.account.AccountService;
import com.grocerystore.api.passkey.PasskeyDtos.CeremonyOptions;
import com.grocerystore.api.passkey.PasskeyDtos.CeremonyResponse;
import com.grocerystore.api.passkey.PasskeyDtos.PasskeyView;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class PasskeyController {

    private final PasskeyService passkeys;
    private final AccountService accounts;

    public PasskeyController(PasskeyService passkeys, AccountService accounts) {
        this.passkeys = passkeys;
        this.accounts = accounts;
    }

    @PostMapping("/auth/passkey/options")
    public CeremonyOptions signInOptions() {
        return passkeys.startSignIn();
    }

    @PostMapping("/auth/passkey")
    public SignedIn signIn(@Valid @RequestBody CeremonyResponse response) {
        return passkeys.finishSignIn(response);
    }

    @GetMapping("/me/passkeys")
    public List<PasskeyView> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return passkeys.list(accounts.require(authorization).getId());
    }

    @PostMapping("/me/passkeys/options")
    public CeremonyOptions registrationOptions(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return passkeys.startRegistration(accounts.require(authorization).getId());
    }

    @PostMapping("/me/passkeys")
    @ResponseStatus(HttpStatus.CREATED)
    public PasskeyView register(@RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CeremonyResponse response) {
        return passkeys.finishRegistration(accounts.require(authorization).getId(), response);
    }

    @DeleteMapping("/me/passkeys/{passkeyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long passkeyId) {
        passkeys.remove(accounts.require(authorization).getId(), passkeyId);
    }
}
