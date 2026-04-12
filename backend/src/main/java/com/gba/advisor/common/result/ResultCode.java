package com.gba.advisor.common.result;
import lombok.Getter;
@Getter
public enum ResultCode {
    SUCCESS(200,"ok"), BAD_REQUEST(400,"请求参数错误"), UNAUTHORIZED(401,"未登录或Token已过期"),
    FORBIDDEN(403,"无权限访问"), NOT_FOUND(404,"资源不存在"), CONFLICT(409,"数据已存在"),
    USERNAME_EXIST(5001,"用户名已存在"), PASSWORD_ERROR(5002,"用户名或密码错误"),
    TOKEN_INVALID(5003,"Token无效"), OCR_FAILED(5010,"图片识别失败，请重试"),
    PANEL_NOT_FOUND(5020,"未找到面板数据，请先上传面板"), INTERNAL_ERROR(500,"服务器内部错误");
    private final int code;
    private final String message;
    ResultCode(int code, String message){ this.code=code; this.message=message; }
}
