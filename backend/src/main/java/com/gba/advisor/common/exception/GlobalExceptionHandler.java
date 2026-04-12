package com.gba.advisor.common.exception;
import com.gba.advisor.common.result.Result;
import com.gba.advisor.common.result.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getResultCode(), e.getMessage());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField()+": "+fe.getDefaultMessage()).findFirst().orElse("参数校验失败");
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBind(BindException e) {
        String msg = e.getFieldError()!=null ? e.getFieldError().getDefaultMessage() : "参数错误";
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraint(ConstraintViolationException e) { return Result.fail(ResultCode.BAD_REQUEST, e.getMessage()); }
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAccess(AccessDeniedException e) { return Result.fail(ResultCode.FORBIDDEN); }
    @ExceptionHandler(BadCredentialsException.class)
    public Result<?> handleBadCreds(BadCredentialsException e) { return Result.fail(ResultCode.PASSWORD_ERROR); }
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleAll(Exception e) { log.error("未知异常: ", e); return Result.fail(ResultCode.INTERNAL_ERROR); }
}
