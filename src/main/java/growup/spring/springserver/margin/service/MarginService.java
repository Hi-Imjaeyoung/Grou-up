package growup.spring.springserver.margin.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.exception.netsales.NetSalesNotFoundProductName;
import growup.spring.springserver.margin.TypeChangeMargin;
import growup.spring.springserver.margin.converter.MarginConverter;
import growup.spring.springserver.margin.converter.MarginToMarginResultConverter;
import growup.spring.springserver.margin.converter.MarginToSimpleMarginConverter;
import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.dto.*;
import growup.spring.springserver.margin.factory.MarginConverterFactory;
import growup.spring.springserver.margin.repository.MarginRepository;
import growup.spring.springserver.margin.util.MfcKeyUtils;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.dto.MfcDto;
import growup.spring.springserver.marginforcampaign.dto.MfcRequestWithDatesDto;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import growup.spring.springserver.marginforcampaign.support.MarginType;
import growup.spring.springserver.netsales.domain.NetSales;
import growup.spring.springserver.netsales.repository.NetRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
@Slf4j
public class MarginService {

    private final MarginRepository marginRepository;
    private final CampaignService campaignService;
    private final MarginForCampaignRepository marginForCampaignRepository;
    private final NetRepository netRepository;
    private final MarginConverterFactory marginConverterFactory;
//    private final OAuth2ClientRegistrationRepositoryConfiguration oAuth2ClientRegistrationRepositoryConfiguration;

    /*
     * TODO
     *  getCampaignAllSales()
     *  대시보드 3사분면
     * */
    public List<MarginSummaryResponseDto> getCampaignAllSales(String email, LocalDate targetDate) {
        LocalDate difTargetDate = targetDate.minusDays(1);

        List<Campaign> campaignList = getCampaignsByEmail(email);
        List<Long> campaignIds = extractCampaignIds(campaignList);

        List<Margin> margins = marginRepository.findByCampaignIdsAndDates(campaignIds, difTargetDate, targetDate);
        Map<Long, List<Margin>> marginMap = margins.stream()
                .collect(Collectors.groupingBy(m -> m.getCampaign().getCampaignId()));
        List<MarginSummaryResponseDto> summaries = new ArrayList<>();

        for (Campaign campaign : campaignList) {
            // 오늘과 어제 데이터 필터링
            Margin todayMargin = getMarginForDateOrDefault(marginMap, campaign, targetDate);
            Margin yesterdayMargin = getMarginForDateOrDefault(marginMap, campaign, difTargetDate);
            MarginSummaryResponseDto summary = TypeChangeMargin.entityToMarginSummaryResponseDto(
                    campaign, todayMargin, yesterdayMargin);
            summaries.add(summary);
        }

        return summaries;
    }

    /*
     * TODO
     *  getDailyAdSummary()
     *  1, 2 사분면
     * */
    public List<DailyAdSummaryDto> findByCampaignIdsAndDates(String email, LocalDate targetDate) {
        LocalDate difTargetDate = targetDate.minusDays(7);

        List<Campaign> campaignList = getCampaignsByEmail(email);
        List<Long> campaignIds = extractCampaignIds(campaignList);

        return marginRepository.find7daysTotalsByCampaignIds(campaignIds, difTargetDate, targetDate);
    }

    private List<Campaign> getCampaignsByEmail(String email) {
//        Member member = memberRepository.findByEmail(email).orElseThrow(
//                MemberNotFoundException::new
//        );
//        List<Campaign> campaignList = campaignRepository.findAllByMember(member);
//        if (campaignList.isEmpty()) {
//            throw new CampaignNotFoundException();
//        }
//        List<Campaign> campaignList = campaignService.getCampaignsByEmail(email);
//        return campainList;
        return campaignService.getCampaignsByEmail(email);
    }

    private List<Long> extractCampaignIds(List<Campaign> campaignList) {
        return campaignList.stream()
                .map(Campaign::getCampaignId)
                .toList();
    }

    private Margin getMarginForDateOrDefault(Map<Long, List<Margin>> marginMap, Campaign campaign, LocalDate date) {
        List<Margin> campaignMargins = getMarginsForCampaign(marginMap, campaign.getCampaignId());
        return findMarginByDate(campaignMargins, date)
                .orElse(TypeChangeMargin.createDefaultMargin(campaign, date));
    }

    private List<Margin> getMarginsForCampaign(Map<Long, List<Margin>> marginMap, Long campaignId) {
        return marginMap.getOrDefault(campaignId, new ArrayList<>());
    }

    private Optional<Margin> findMarginByDate(List<Margin> margins, LocalDate date) {
        return margins.stream()
                .filter(m -> m.getMarDate().equals(date))
                .findFirst(); // Optional로 반환
    }

    /*
     * TODO
     *  1. startDate ~ endDate 까지 마진데이터 다 가져온다.
     *  2.1 startDate 부터 하루씩 늘리면서 계산해서 넣는 것들이 있는 지 확인한다.
     *  2.2 만약에 비어있는 경우, NetSales 호출 후 계산해서 다시 집어넣어준다.
     * */
    @Transactional
    public List<MarginResponseDto> getALLMargin(LocalDate start, LocalDate end, Long campaignId, String email) {
        List<Margin> margins = byCampaignIdAndDates(start, end, campaignId);
        Campaign myCampaign = campaignService.getMyCampaign(campaignId, email);

        Set<LocalDate> existingDates = margins.stream()
                .map(Margin::getMarDate)
                .collect(Collectors.toSet());

        List<LocalDate> datesWithNetSales = netRepository.findDatesWithNetSalesByEmailAndDateRange(
                email, start, end);

        List<LocalDate> newMarginDates = datesWithNetSales.stream()
                .filter(date -> !existingDates.contains(date))
                .toList();

        List<Margin> newMargins = newMarginDates.stream()
                .map(date -> TypeChangeMargin.createSaveDefaultMargin(myCampaign, date))
                .toList();

        if (!newMargins.isEmpty()) {
            marginRepository.saveAll(newMargins);
        }

        List<Margin> fullMargins = new ArrayList<>(margins);
        fullMargins.addAll(newMargins);

        List<Margin> calculateMargin = calculateMargin(fullMargins, campaignId, email);

        // 전략패턴 적용
        MarginConverter<MarginResultDto> converter = marginConverterFactory.getResultConverter();

        List<MarginResultDto> marginResultDtos = calculateMargin.stream()
                .map(converter::convert)
                .toList();

        MarginResponseDto marginResponseDto = MarginResponseDto.builder()
                .campaignId(campaignId)
                .data(marginResultDtos)
                .build();

        return List.of(marginResponseDto);
    }



    public List<Margin> calculateMargin(List<Margin> margins, Long campaignId, String email) {
        // 보여줄 데이터
        List<Margin> datas = new ArrayList<>();
        for (Margin margin : margins) {
            // marAdMargin과 marNetProfit이 모두 0일 때 netSales를 호출
            // A 에 대해서 마진이 없음 => A에 대해서 업데이트 쳐야함
            if (margin.getMarAdMargin() == 0 && margin.getMarNetProfit() == 0.0 || margin.getMarReturnCost() == 0) {
                Margin updateMargin = callNetSales(margin, campaignId, margin.getMarDate(), email);
                datas.add(updateMargin);
            } else {
                datas.add(margin);
            }
        }
        return datas;
    }

    // NetSales 에서 가져와야함, 옵션명이랑 연결되어있음
    public Margin callNetSales(Margin margin, Long campaignId, LocalDate date, String email) {
        // 해당 캠페인의 내가 추가한 모든 옵션들 가져옴
        List<MarginForCampaign> marginForCampaigns = marginForCampaignRepository.MarginForCampaignByCampaignId(campaignId);

        long actualSales = 0; // 순 판매 수
        long adMargin = 0; // 광고 머진
        long returnCount = 0; // 반품 수
        double returnCost = 0.0; // 캠페인 별 총 반품 비용

        // MarginForCampaign마다 netSales를 매칭해서 합산
        for (MarginForCampaign data : marginForCampaigns) {
            try {
                NetSales netSales = checkNetSales(date, email, data.getMfcProductName(), data.getMfcType());
                // netSales가 존재하면 합산
                actualSales += netSales.getNetSalesCount();
                adMargin += netSales.getNetSalesCount() * data.getMfcPerPiece();
                returnCount += netSales.getNetReturnCount();
                returnCost += netSales.getNetReturnCount() * data.getMfcReturnPrice();
            } catch (NetSalesNotFoundProductName e) {
                continue;
            }
        }

        margin.update(actualSales, adMargin, returnCount, returnCost);

        return margin;
    }


    private List<Margin> byCampaignIdAndDates(LocalDate start, LocalDate end, Long campaignId) {

        return marginRepository.findByCampaignIdAndDates(campaignId, start, end);
    }

    public NetSales checkNetSales(LocalDate date, String email, String productName, MarginType marginType) {
        return netRepository.findByNetDateAndEmailAndNetProductNameAndNetMarginType(date, email, productName, marginType)
                .orElseThrow(NetSalesNotFoundProductName::new);
    }

    //    기간 별 마진, 바꾸기
    @Transactional
    public void marginUpdatesByPeriod(MfcRequestWithDatesDto mfcRequestWithDatesDto, String email) {
        LocalDate start = mfcRequestWithDatesDto.getStartDate();
        LocalDate end = mfcRequestWithDatesDto.getEndDate();
        Long campaignId = mfcRequestWithDatesDto.getCampaignId();

        // 1. 기간별 마진 데이터 가져옴
        List<Margin> margins = byCampaignIdAndDates(start, end, campaignId);

        // 2. 프론트에서 넘겨준 변경된 마진 데이터를 Map으로 변환
        Map<MfcKey, MfcDto> updatedMarginsMap = MfcKeyUtils.toMfcMap(mfcRequestWithDatesDto.getData());

        // 3. 해당 캠페인의 옵션 데이터를 Map으로 변환
        List<MarginForCampaign> marginForCampaignList = marginForCampaignRepository.MarginForCampaignByCampaignId(campaignId);
        Map<MfcKey, MarginForCampaign> marginForCampaignMap = MfcKeyUtils.toMfcMap(marginForCampaignList);

        // 4. 기간별 마진 업데이트
        margins.forEach(margin -> updateMargin(margin, updatedMarginsMap, marginForCampaignMap, email));
    }

    /**
     * 특정 기간의 마진 데이터를 업데이트
     *
     * @param margin               기존 마진 데이터
     * @param updatedMarginsMap    프론트에서 받은 *변경된* 마진 정보 (상품명 + 마진 타입 기준)
     * @param marginForCampaignMap 해당 캠페인에 속한 옵션 정보 (상품명 + 마진 타입 기준)
     * @param email                사용자 이메일
     */

    private void updateMargin(Margin margin, Map<MfcKey, MfcDto> updatedMarginsMap,
                              Map<MfcKey, MarginForCampaign> marginForCampaignMap, String email) {
        long adMargin = 0;
        long returnPrice = 0;

        // 1. 해당 캠페인의 각 옵션을 순회하면서 마진 업데이트
        for (Map.Entry<MfcKey, MarginForCampaign> entry : marginForCampaignMap.entrySet()) {
            MfcKey key = entry.getKey();
            MarginForCampaign tempData = entry.getValue();

            try {
                // 2. 해당 상품의 순 매출 및 반품 건수를 가져온다.
                NetSales netSales = checkNetSales(margin.getMarDate(), email, key.getProductName(), key.getType());

                // 3. 프론트에서 넘어온 수정된 값이 있으면 사용하고, 없으면 기존 값을 사용
                long perPieceMargin = Optional.ofNullable(updatedMarginsMap.get(key))
                        .map(MfcDto::getMfcPerPiece)
                        .orElse(tempData.getMfcPerPiece());

                long perReturnPrice = Optional.ofNullable(updatedMarginsMap.get(key))
                        .map(MfcDto::getMfcReturnPrice)
                        .orElse(tempData.getMfcReturnPrice());

                // 4. 해당 기간 동안 발생한 순 매출 및 반품을 반영하여 마진 계산
                adMargin += netSales.getNetSalesCount() * perPieceMargin;
                returnPrice += netSales.getNetReturnCount() * perReturnPrice;
            } catch (NetSalesNotFoundProductName e) {
                // 5. 해당 상품의 순 매출 데이터가 없으면 예외 발생, 무시하고 다음 옵션으로 진행

                continue;
            }
        }

        margin.update(adMargin, returnPrice);
    }
    public List<DailyMarginSummary> getDailyMarginSummary(String email, LocalDate targetDate) {

        List<DailyMarginSummary> summaries = new ArrayList<>();

        List<Campaign> campaignList = getCampaignsByEmail(email);

        // 보석, 재영, 은아
        for (Campaign campaign : campaignList) {
            try {
                Margin margin = getMargin(targetDate, campaign);
                String productName = campaign.getCamCampaignName();

                summaries.add(TypeChangeMargin.getDailyMarginSummary(margin, productName));
            } catch (CampaignNotFoundException ex) {
                continue;
            }

        }
        return summaries;
    }


    // 기간별 마진의 총 합
    public List<DailyNetProfitResponseDto> getDailyTotalMarginListResDto(LocalDate start, LocalDate end, String email) {
        return marginRepository.findTotalMarginByDateRangeAndEmail(start, end, email);
    }

    private Margin getMargin(LocalDate targetDate, Campaign campaign) {
        return marginRepository.findByCampaignIdAndDate(campaign.getCampaignId(), targetDate).orElseThrow(CampaignNotFoundException::new);
    }

    @Transactional
    public MarginUpdateResponseDto updateEfficiencyAndAdBudget(MarginUpdateRequestDtos requestDtos) {
        int requestCount = requestDtos.getData().size();

        // 실패 데이터를 스트림을 통해 수집
        Map<LocalDate, Map<String, Double>> failData = requestDtos
                .getData()
                .stream()
                .filter(data -> !updateIfValid(data))
                .collect(Collectors.toMap( // 실패한 애들
                        MarginUpdateRequestDto::getMarDate, // 키
                        this::createFailData // 값
                ));

        int successCount = requestCount - failData.size();

        return TypeChangeMargin.marginValidationResponse(successCount, requestCount, failData);
    }

    // ID 유효성 검증 및 업데이트 시도, 실패 시 false 반환
    private boolean updateIfValid(MarginUpdateRequestDto data) {
        return marginRepository.findById(data.getId())
                .map(margin -> {
                    margin.updateMarginData(data.getMarTargetEfficiency(), data.getMarAdBudget());
                    return true;
                })
                .orElse(false);
    }

    // 실패 데이터 생성 메서드
    private Map<String, Double> createFailData(MarginUpdateRequestDto data) {
        return Map.of(
                "targetEfficiency", data.getMarTargetEfficiency(),
                "adBudget", data.getMarAdBudget()
        );
    }

    @Transactional
    public int deleteKeywordByCampaignIdsAndDate(List<Long> campaignIds, LocalDate start, LocalDate end) {
        return marginRepository.deleteByCampaignIdAndDate(start, end, campaignIds);
    }

    public Long createMarginTable(LocalDate targetDate, Long campaignId, String email) {

        Campaign myCampaign = campaignService.getMyCampaign(campaignId, email);

        // Db에 이미 있는 마진 데이터 인지 확인 ID는 모르니 CAMPAIGN_ID와 날짜로 확인

        try {
            Margin margin = getMargin(targetDate, myCampaign);
            return margin.getId();
        } catch (CampaignNotFoundException e) {
            log.warn("Margin not exist so create new margin");
            Margin saveDefaultMargin = TypeChangeMargin.createSaveDefaultMargin(myCampaign, targetDate);
            Margin savedMargin = marginRepository.save(saveDefaultMargin);
            return savedMargin.getId();
        }
    }

    public SimpleMarginResponseForStaticGraph3Dto getSimpleMargin(LocalDate start, LocalDate end, Long campaignId, String email) {
        List<Margin> margins = marginRepository.findByCampaignIdAndDates(campaignId, start, end);

        // 책임을 팩토리에 넘김
        MarginConverter<SimpleMarginResponseDto> converter = marginConverterFactory.getSimpleConverter();

        List<SimpleMarginResponseDto> simpleMarginResponseDtos = margins.stream()
                .map(converter::convert)
                .toList();

        return SimpleMarginResponseForStaticGraph3Dto.builder()
                .campaignId(campaignId)
                .data(simpleMarginResponseDtos)
                .build();
    }

    public LocalDate findLatestMarginDateByEmail(String email) {
        return marginRepository
                .findLatestMarginDateByEmail(email)
                .orElseGet(LocalDate::now);
    }
}