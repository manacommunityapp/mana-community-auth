package com.manacommunity.auth.exception;

import org.springframework.http.HttpStatus;
import com.manacommunity.common.exception.ManaCommunityException;

/** Thrown when the community invite code provided during registration is invalid. */
public class InvalidInviteCodeException extends ManaCommunityException {

    public InvalidInviteCodeException(String code) {
        super("The invite code is invalid or does not match any community. Please check and try again.",
                HttpStatus.BAD_REQUEST, "INVALID_INVITE_CODE");
    }
}
