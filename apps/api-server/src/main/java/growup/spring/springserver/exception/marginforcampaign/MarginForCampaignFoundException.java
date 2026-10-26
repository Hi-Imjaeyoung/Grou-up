package growup.spring.springserver.exception.marginforcampaign;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;

public class MarginForCampaignFoundException extends GrouException {
    private static final ErrorCode errorCode = ErrorCode.MARGIN_FOR_CAMPAIGN_FOUND;

    public MarginForCampaignFoundException() {
        super(errorCode);
    }
}
