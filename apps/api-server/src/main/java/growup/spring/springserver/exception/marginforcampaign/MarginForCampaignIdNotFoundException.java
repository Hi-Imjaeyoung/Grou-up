package growup.spring.springserver.exception.marginforcampaign;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;

public class MarginForCampaignIdNotFoundException extends GrouException {
    private static final ErrorCode errorCode = ErrorCode.MARGIN_FOR_CAMPAIGN_ID_NOT_FOUND;

    public MarginForCampaignIdNotFoundException() {
        super(errorCode);
    }
}
