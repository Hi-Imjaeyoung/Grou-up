package growup.spring.springserver.exception.memo;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import lombok.Getter;

@Getter
public class MemoNotFoundException extends GrouException {
    private static final ErrorCode errorCode = ErrorCode.MEMO_NOT_FOUND;
    public MemoNotFoundException() {
        super(errorCode);
    }
}
