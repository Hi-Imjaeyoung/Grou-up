package growup.spring.springserver.exception.netsales;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;

public class NetSalesNotFoundProductName extends GrouException {
    private static final ErrorCode errorCode = ErrorCode.NET_SALES_NOT_FOUND_PRODUCT_NAME;

    public NetSalesNotFoundProductName() {
        super(errorCode);
    }
}
