package growup.spring.springserver.exception.login;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import lombok.Getter;


@Getter
public class MemberInvalidPasswordException extends GrouException {

    private static final ErrorCode errorCode = ErrorCode.MEMBER_INVALID_PASSWORD_EXCEPTION;

    public MemberInvalidPasswordException(){
        super(errorCode);
    }
}