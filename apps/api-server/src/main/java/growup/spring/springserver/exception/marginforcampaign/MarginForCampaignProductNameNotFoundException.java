package growup.spring.springserver.exception.marginforcampaign;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;

public class MarginForCampaignProductNameNotFoundException extends GrouException {
    private static final ErrorCode errorCode = ErrorCode.MARGIN_FOR_CAMPAIGN_PRODUCT_NAME_NOT_FOUND;

    public MarginForCampaignProductNameNotFoundException() {
        super(errorCode);
    }
}
