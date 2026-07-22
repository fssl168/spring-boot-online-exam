/***********************************************************
 * @Description : 全局异常处理器，统一封装异常为标准 ResultVO 响应
 ***********************************************************/
package lsgwr.exam.exception;

import lsgwr.exam.vo.ResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：
 * - ExamException: 业务异常，使用其携带的 code
 * - 参数校验异常: 返回 400
 * - 其它未捕获异常: 返回 500（不泄露内部异常信息）
 * - 角色鉴权失败由 RoleInterceptor 直接返回 403，不进入此处
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常
     */
    @ExceptionHandler(ExamException.class)
    public ResultVO<Void> handleExamException(ExamException e) {
        return new ResultVO<>(e.getCode(), e.getMessage(), null);
    }

    /**
     * 参数校验失败（@Valid 触发）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultVO<Void>> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败";
        ResultVO<Void> body = new ResultVO<>(1, msg, null);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * 参数格式错误（如 @RequestBody 反序列化失败、类型不匹配）
     */
    @ExceptionHandler({IllegalArgumentException.class, org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<ResultVO<Void>> handleBadRequest(Exception e) {
        ResultVO<Void> body = new ResultVO<>(1, "请求参数不正确", null);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * 其它未捕获异常
     * E-07 修复：移除 403 死代码分支（RoleInterceptor 已直接返回 403）
     * A-12 修复：不向客户端泄露内部异常信息，仅返回通用提示
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultVO<Void>> handleException(Exception e) {
        // 记录完整异常到日志，便于排查
        log.error("未捕获异常: {}", e.getMessage(), e);
        ResultVO<Void> body = new ResultVO<>(-99, "服务器内部错误，请稍后重试", null);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
