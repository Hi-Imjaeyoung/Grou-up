package growup.spring.springserver.global.error;

import growup.spring.springserver.exception.global.InvalidDateFormatException;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.exception.RequestError;
import lombok.experimental.UtilityClass;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityNotFoundException;



@UtilityClass
public class DummyControllers {

    @RestController
    static class CampaignNotFoundController {
        @GetMapping("/test/campaignNotFound")
        public void throwCampaignNotFound() {
            throw new CampaignNotFoundException();
        }
    }

    @RestController
    static class RequestErrorController {
        @GetMapping("/test/requestError")
        public void throwRequestError() {
            throw new RequestError();
        }
    }

    @RestController
    public class InvalidDateFormatController {
        @GetMapping("/test/invalidDateFormat")
        public void throwInvalidDateFormat() {
            throw new InvalidDateFormatException();
        }
    }
}
