package growup.spring.springserver.margin.util;

import growup.spring.springserver.marginforcampaign.support.MarginType;

public interface MfcKeyProvider {
    String getMfcProductName();
    MarginType getMfcType();
}

// MfcDto, MarginForCampaign 은 다른 클래스지만, 공통 속성 가짐
// 그것을 추상화한 인터페이스

