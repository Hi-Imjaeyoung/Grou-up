package growup.spring.springserver.global.annotaion;

import growup.spring.springserver.exception.global.DateRangeViolationException;
import growup.spring.springserver.exception.global.InvalidDateFormatException;
import growup.spring.springserver.exception.global.RequestException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;


import java.time.LocalDate;

@Slf4j
public class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

    private String startFieldName;
    private String endFieldName;
    private String message;
    /**
     * ValidDateRange 애노테이션이 적용된 DTO의 필드 이름을 초기화.
     * @param constraintAnnotation ValidDateRange 애노테이션
     */
    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.startFieldName = constraintAnnotation.startField();
        this.endFieldName   = constraintAnnotation.endField();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // null (검증 대상이 아예 없을 때) 이거나 DTO 자체가 null 이면 패스
        if (value == null) {
            return true;
        }
        // BeanWrapper 를 사용하면 getter 호출을 통해 프로퍼티 값을 가져올 수 있다.
        BeanWrapper wrapper = new BeanWrapperImpl(value);

        Object startObj = wrapper.getPropertyValue(startFieldName);
        Object endObj   = wrapper.getPropertyValue(endFieldName);
        // 프로퍼티가 LocalDate 타입이 아니면 잘못된 설정으로 본다.
        if (!(startObj instanceof LocalDate startDate) || !(endObj instanceof LocalDate endDate)) {
            log.warn("invalid date type");
            context.disableDefaultConstraintViolation(); // 기본 메시지 비활성화
            context.buildConstraintViolationWithTemplate("날짜 필드 타입이 올바르지 않습니다.").addConstraintViolation();
            throw new InvalidDateFormatException();
        }
        // 실제 날짜 비교 로직
        if(startDate.isAfter(endDate)){
            log.error("DateRangeViolationException ");
            throw new InvalidDateFormatException();
        }

        return true;
    }
}