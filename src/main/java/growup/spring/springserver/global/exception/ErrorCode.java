package growup.spring.springserver.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    //Login
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    MEMBER_KAKAO_LOGIN_FOUND(HttpStatus.BAD_REQUEST,"해당 계정은 카카오 로그인 전용입니다."),
    MEMBER_INVALID_PASSWORD_EXCEPTION(HttpStatus.BAD_REQUEST, "비밀번호가 틀렸습니다."),

    // Campaign
    CAMPAIGN_NOT_FOUND(HttpStatus.NOT_FOUND, "현재 등록된 캠페인이 없습니다."),

    // CampaignOptiondetails
    CAMPAIGN_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 캠페인의 옵션이 없습니다."),
    CAMPAIGN_OPTION_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 옵션의 데이터가 존재하지 않습니다."),

    // Keyword
    CAMPAIGN_KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 캠페인의 키워드가 없습니다."),

    //Global
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "날짜 형식이 이상합니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST,"잘못된 요청 형식입니다."),
    VIOLATION_DATE_RANGE(HttpStatus.BAD_REQUEST,"시작날짜가 끝 날짜보다 클 수 없습니다."),

    //KeywordBid
    KEYWORDBID_NOT_FOUND(HttpStatus.BAD_REQUEST,"입찰가 정보가 없습니다."),

    //ExclusionKeyword
    ALREADY_EXIST_KEYWORD(HttpStatus.BAD_REQUEST,"이미 제외 키워드가 존재합니다"),
    EXCLUSIONKEY_NOT_FOUND(HttpStatus.BAD_REQUEST,"해당 제외키워드가 없습니다"),

    // Execution
    EXECUTION_REQUEST_ERROR(HttpStatus.BAD_REQUEST,"잘못된 요청값 입니다."),

    // MarginForCampaign
    MARGIN_FOR_CAMPAIGN_PRODUCT_NAME_NOT_FOUND(HttpStatus.BAD_REQUEST, "현재 캠페인 제외하고 없는 상품입니다."),
    MARGIN_FOR_CAMPAIGN_ID_NOT_FOUND(HttpStatus.BAD_REQUEST,"없는 ID 입니다."),
    MARGIN_FOR_CAMPAIGN_FOUND(HttpStatus.BAD_REQUEST,"현재 캠페인의 있는 상품명 입니다."),

    // NetSales
    NET_SALES_NOT_FOUND_PRODUCT_NAME(HttpStatus.BAD_REQUEST, "없는 상품아이디 입니다."),

    // Memo
    MEMO_NOT_FOUND(HttpStatus.BAD_REQUEST,"해당 메모가 존재하지 않습니다"),

    // File
    FILE_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 파일이 존재하지 않습니다."),
    FILE_INVALID_DATA_FORM(HttpStatus.BAD_REQUEST,"파일 내 누락된 값이 존재합니다.")
    ;


    ErrorCode(HttpStatus httpStatus, String message){
        this.httpStatus = httpStatus;
        this.message = message;
    }

    private HttpStatus httpStatus;
    private String message;

}
