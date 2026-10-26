package growup.spring.springserver.exception.campaign;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import lombok.Getter;

@Getter
public class CampaignNotFoundException extends GrouException {

    private static final ErrorCode errorCode = ErrorCode.CAMPAIGN_NOT_FOUND;

    public CampaignNotFoundException(){
        super(errorCode);
    }
}
