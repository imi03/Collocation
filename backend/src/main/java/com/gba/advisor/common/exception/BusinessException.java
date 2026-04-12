package com.gba.advisor.common.exception;
import com.gba.advisor.common.result.ResultCode;
import lombok.Getter;
@Getter
public class BusinessException extends RuntimeException {
    private final ResultCode resultCode;
    public BusinessException(ResultCode rc) { super(rc.getMessage()); this.resultCode=rc; }
    public BusinessException(ResultCode rc, String msg) { super(msg); this.resultCode=rc; }
}
