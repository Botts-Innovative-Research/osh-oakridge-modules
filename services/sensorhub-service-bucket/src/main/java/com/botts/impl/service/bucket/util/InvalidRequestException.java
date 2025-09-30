package com.botts.impl.service.bucket.util;

import java.io.IOException;

public class InvalidRequestException extends IOException
{

    public enum ErrorCode
    {
        UNSUPPORTED_OPERATION,
        BAD_REQUEST,
        NOT_FOUND,
        BAD_PAYLOAD,
        REQUEST_REJECTED,
        REQUEST_ACCEPTED_TIMEOUT,
        FORBIDDEN,
        INTERNAL_ERROR
    }


    ErrorCode errorCode;


    public InvalidRequestException(ErrorCode errorCode, String msg)
    {
        this(errorCode, msg, null);
    }


    public InvalidRequestException(ErrorCode errorCode, String msg, Throwable e)
    {
        super(msg, e);
        this.errorCode = errorCode;
    }


    public ErrorCode getErrorCode()
    {
        return errorCode;
    }
}
