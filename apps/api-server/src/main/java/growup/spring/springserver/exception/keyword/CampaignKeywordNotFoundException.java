package growup.spring.springserver.exception.keyword;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import lombok.Getter;

@Getter
public class CampaignKeywordNotFoundException extends GrouException {

    private static final ErrorCode errorCode = ErrorCode.CAMPAIGN_KEYWORD_NOT_FOUND;

    public CampaignKeywordNotFoundException(){
        super(errorCode);
    }
}
