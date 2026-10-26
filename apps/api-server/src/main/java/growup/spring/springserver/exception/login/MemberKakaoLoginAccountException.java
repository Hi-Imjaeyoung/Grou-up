package growup.spring.springserver.exception.login;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import lombok.Getter;

@Getter
public class MemberKakaoLoginAccountException extends GrouException {

    private static final ErrorCode errorCode = ErrorCode.MEMBER_KAKAO_LOGIN_FOUND;

    public MemberKakaoLoginAccountException(){
        super(errorCode);
    }
}
