package com.grocerystore.api.account;

import java.time.Instant;
import java.util.List;

import com.grocerystore.api.common.PhoneNumbers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record SignUpRequest(
            @NotBlank @Size(max = 100) String name,
            // Optional. An account can be made with the email address alone.
            @Pattern(regexp = PhoneNumbers.PATTERN, message = PhoneNumbers.MESSAGE) String phone,
            @NotBlank @Email @Size(max = 254) String email,
            // BCrypt reads at most 72 bytes of a password.
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    /** login is the mobile number or the email address of the account. */
    public record SignInRequest(@NotBlank @Size(max = 254) String login, @NotBlank @Size(max = 72) String password) {
    }

    public record ConfirmSignUpRequest(@NotBlank @Size(max = 100) String signupId,
            @NotBlank @Pattern(regexp = "[0-9]{6}", message = "must be the 6 digit code") String code) {
    }

    /**
     * Either the account is ready (token and account are set), or a code was emailed and the sign-up
     * finishes when it is confirmed (signupId and sentTo are set).
     */
    public record SignUpResponse(String token, AccountView account, String signupId, String sentTo) {
    }

    public record AccountView(String name, String phone, String email, int pointsBalance,
            List<PointView> recentPoints) {
    }

    public record PointView(int points, String reason, String orderNumber, Instant createdAt) {
    }

    public record SignedIn(String token, AccountView account) {
    }

    public record EmailCodeRequest(@NotBlank @Email @Size(max = 254) String email) {
    }

    /** codeId identifies this code in the next step. The address itself is not said to have an account or not. */
    public record EmailCodeSent(String codeId, String sentTo) {
    }

    public record VerifyEmailCodeRequest(@NotBlank @Size(max = 100) String codeId,
            @NotBlank @Pattern(regexp = "[0-9]{6}", message = "must be the 6 digit code") String code) {
    }

    /** Either signed in (token and account), or the address is new and needsProfile asks for a name first. */
    public record EmailVerified(String token, AccountView account, boolean needsProfile) {
    }

    public record FinishEmailSignUpRequest(@NotBlank @Size(max = 100) String codeId,
            @NotBlank @Size(max = 100) String name,
            @Pattern(regexp = PhoneNumbers.PATTERN, message = PhoneNumbers.MESSAGE) String phone) {
    }
}
