package growup.spring.springserver.global.annotaion;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidDateRangeValidator.class)
@Target({ ElementType.TYPE })  // 클래스(=DTO) 레벨에서 적용
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {
    String message() default "시작일자는 종료일자보다 이전이어야 합니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    // 애노테이션 속성으로, DTO 내부에서 날짜 필드 이름을 지정할 수 있도록 함
    String startField();     // 예: "startDate"
    String endField();       // 예: "endDate"
}