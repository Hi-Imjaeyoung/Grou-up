package growup.spring.springserver.exception;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;

public class RequestError extends GrouException {
  private static final ErrorCode errorCode = ErrorCode.EXECUTION_REQUEST_ERROR;

  public RequestError(){
    super(errorCode);
  }
}
