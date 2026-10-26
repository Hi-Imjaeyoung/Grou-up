package growup.spring.springserver.exception.campaignoptiondetails;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import lombok.Getter;

@Getter
public class CampaignOptionNotFoundException extends GrouException {

    private static final ErrorCode errorCode = ErrorCode.CAMPAIGN_OPTION_NOT_FOUND;

    public CampaignOptionNotFoundException(){
        super(errorCode);
    }
}
