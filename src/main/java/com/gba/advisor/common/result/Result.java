package com.gba.advisor.common.result;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {
    private final int code;
    private final String message;
    private final T data;
    private Result(int code, String message, T data) { this.code=code; this.message=message; this.data=data; }
    public static <T> Result<T> ok(T data) { return new Result<>(ResultCode.SUCCESS.getCode(),"ok",data); }
    public static <T> Result<T> ok() { return new Result<>(ResultCode.SUCCESS.getCode(),"ok",null); }
    public static <T> Result<T> fail(ResultCode rc) { return new Result<>(rc.getCode(),rc.getMessage(),null); }
    public static <T> Result<T> fail(ResultCode rc, String msg) { return new Result<>(rc.getCode(),msg,null); }
    public static <T> Result<T> fail(int code, String msg) { return new Result<>(code,msg,null); }
}
