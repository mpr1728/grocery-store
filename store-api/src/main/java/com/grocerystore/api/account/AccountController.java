package com.grocerystore.api.account;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.api.account.AccountDtos.AccountView;
import com.grocerystore.api.account.AccountDtos.ConfirmSignUpRequest;
import com.grocerystore.api.account.AccountDtos.SignInRequest;
import com.grocerystore.api.account.AccountDtos.SignUpRequest;
import com.grocerystore.api.account.AccountDtos.SignUpResponse;
import com.grocerystore.api.account.AccountDtos.SignedIn;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignUpResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return accounts.signUp(request);
    }

    @PostMapping("/auth/signup/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public SignedIn confirmSignUp(@Valid @RequestBody ConfirmSignUpRequest request) {
        return accounts.confirmSignUp(request);
    }

    @PostMapping("/auth/signin")
    public SignedIn signIn(@Valid @RequestBody SignInRequest request) {
        return accounts.signIn(request);
    }

    @PostMapping("/auth/signout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void signOut(@RequestHeader(value = "Authorization", required = false) String authorization) {
        accounts.signOut(authorization);
    }

    @GetMapping("/me")
    public AccountView me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return accounts.view(accounts.require(authorization));
    }
}
