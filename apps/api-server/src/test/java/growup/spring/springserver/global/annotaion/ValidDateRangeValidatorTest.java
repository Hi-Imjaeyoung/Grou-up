package growup.spring.springserver.global.annotaion;// src/test/java/growup/spring/springserver/global/validation/ValidDateRangeValidatorTest.java

import growup.spring.springserver.global.dto.req.DateRangeRequest;
import jakarta.validation.*;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidDateRangeValidatorTest {

    private static Validator validator;


    @BeforeAll
    static void setUpValidator() {

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }


    @Test
    @DisplayName("ValidDateRangeValidator failCase 1. startDate > endDate: ")
    void testStartAfterEnd() {
        DateRangeRequest dto = new DateRangeRequest();
        dto.setStart(LocalDate.of(2024, 11, 10));
        dto.setEnd(LocalDate.of(2024, 11, 5));
        ValidationException thrown = assertThrows(ValidationException.class,()->{
           validator.validate(dto);
        });
        assertThat(thrown)
                .isNotNull();
//        Set<ConstraintViolation<DateRangeRequest>> violations = validator.validate(dto);
//
//        assertThat(violations)
//                .isNotEmpty()
//                .anyMatch(v -> v.getMessage().equals("시작일자가 종료일자보다 클 수 없습니다."));
    }

    @Test
    @DisplayName("ValidDateRangeValidator failCase 2. null Value")
    void testNullStartOrEnd() {
        DateRangeRequest dto = new DateRangeRequest();
        dto.setStart(LocalDate.of(2024, 11, 10));
        dto.setEnd(null);
        ValidationException thrown = assertThrows(ValidationException.class,()->{
            validator.validate(dto);
        });
        assertThat(thrown)
                .isNotNull();
//        assertThat(violations)
//                .isNotEmpty()
//                .anyMatch(v -> v.getMessage().equals("시작일자가 종료일자보다 클 수 없습니다."));
    }

    @Test
    @DisplayName("ValidDateRangeValidator successCase 1. startDate == endDate: ValidDateRange 검증 통과")
    void testStartEqualsEnd() {
        DateRangeRequest dto = new DateRangeRequest();
        dto.setStart(LocalDate.of(2024, 11, 5));
        dto.setEnd(LocalDate.of(2024, 11, 5));

        Set<ConstraintViolation<DateRangeRequest>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ValidDateRangeValidator successCase 2. startDate < endDate: ")
    void testStartBeforeEnd() {
        DateRangeRequest dto = new DateRangeRequest();
        dto.setStart(LocalDate.of(2024, 10, 1));
        dto.setEnd(LocalDate.of(2024, 11, 5));

        Set<ConstraintViolation<DateRangeRequest>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }
}
