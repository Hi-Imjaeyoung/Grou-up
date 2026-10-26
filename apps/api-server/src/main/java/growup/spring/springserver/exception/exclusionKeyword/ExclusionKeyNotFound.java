package growup.spring.springserver.exception.exclusionKeyword;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;

public class ExclusionKeyNotFound extends GrouException {

    private static final ErrorCode errorCode = ErrorCode.EXCLUSIONKEY_NOT_FOUND;

    public ExclusionKeyNotFound(){
        super(errorCode);
    }
}
