package growup.spring.springserver.exception.global;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import lombok.Getter;

@Getter
public class InvalidDateFormatException extends GrouException {

    private static final ErrorCode errorCode = ErrorCode.INVALID_DATE_FORMAT;

    public InvalidDateFormatException(){
        super(errorCode);
    }
}
