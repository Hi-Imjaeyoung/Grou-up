package growup.spring.springserver.exception.global;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;

public class DateRangeViolationException extends GrouException {
    private static final ErrorCode errorCode = ErrorCode.VIOLATION_DATE_RANGE;

    public DateRangeViolationException(){
        super(errorCode);
    }
}
