package com.yonyoucloud.fi.cmp.common;

public class CtmException extends com.yonyou.yonbip.ctm.error.CtmException {

    public CtmException(CtmErrorCode errorCode, java.lang.String message) {
        super(errorCode,message);
    }

    public CtmException(java.lang.String message) {
        super(message);
    }

    public CtmException(CtmErrorCode errorCode, java.lang.String message, java.lang.Throwable cause) {
        super(errorCode,message,cause);
    }

    public CtmException(java.lang.String message, java.lang.Throwable cause) {
        super(message,cause);
    }

}
