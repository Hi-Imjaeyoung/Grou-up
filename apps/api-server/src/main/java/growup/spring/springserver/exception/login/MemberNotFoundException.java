package growup.spring.springserver.exception.login;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import lombok.Getter;

@Getter
public class MemberNotFoundException extends GrouException {

    private static final ErrorCode errorCode = ErrorCode.MEMBER_NOT_FOUND;

    public MemberNotFoundException(){
        super(errorCode);
    }
}
