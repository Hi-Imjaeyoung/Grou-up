package growup.spring.springserver.global.error;

import growup.spring.springserver.campaignoptiondetails.dto.CampaignOptionDetailsResponseDto;
import growup.spring.springserver.exception.RequestError;
import growup.spring.springserver.exception.global.DateRangeViolationException;
import growup.spring.springserver.exception.global.InvalidDateFormatException;

import growup.spring.springserver.exception.global.RequestException;
import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice
public class ExceptionHandlerAdvice {

    @ExceptionHandler(GrouException.class)
    public ResponseEntity<ErrorResponseDto> grouException(GrouException e) {
        log.error("Handler campaignNotFoundException message: {}", e.getErrorCode());
        return ErrorResponseDto.of(HttpStatus.BAD_REQUEST, e.getErrorCode().getMessage());
    }

    @Order(value = Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler({
            InvalidDateFormatException.class,       // InvalidDateFormatException이 발생하면 잡습니다.
    })
    public ResponseEntity<CommonResponse<List<?>>> handleValidationException(Exception ex) {
        log.error("Handler catch Exception: 날짜 검증 오류"); // 어떤 예외가 잡혔는지 로그에 출력
        return ResponseEntity.ok(
                CommonResponse.<List<?>>builder("요청 날짜 형식 오류로 인한 빈 값 리턴.")
                        .data(Collections.emptyList())
                        .build()
        );
    }

    // Request 값 관련 예외
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class
    })
    public ResponseEntity<ErrorResponseDto> requestBindingError(){
        return grouException(new RequestException());
    }
}

