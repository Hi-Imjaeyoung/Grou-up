package growup.spring.springserver.exception.global;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;

public class RequestException extends GrouException {
  private static final ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

  public RequestException(){
    super(errorCode);
  }
}
