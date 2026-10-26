package growup.spring.springserver.exception.exclusionKeyword;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;

public class ExistExclusionKeyword extends GrouException {

    private static final ErrorCode errorCode = ErrorCode.ALREADY_EXIST_KEYWORD;

    public ExistExclusionKeyword(){
        super(errorCode);
    }
}
