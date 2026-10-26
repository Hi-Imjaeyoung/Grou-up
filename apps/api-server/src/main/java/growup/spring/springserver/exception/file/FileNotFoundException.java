package growup.spring.springserver.exception.file;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import lombok.Getter;

@Getter
public class FileNotFoundException extends GrouException {

    private static final ErrorCode errorCode = ErrorCode.FILE_NOT_FOUND;

    public FileNotFoundException(){
        super(errorCode);
    }
}
