package growup.spring.springserver.exception.keywordBid;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;

public class KeywordBidNotFound extends GrouException {
    private static final ErrorCode errorCode = ErrorCode.KEYWORDBID_NOT_FOUND;

    public KeywordBidNotFound() {
        super(errorCode);
    }
}
