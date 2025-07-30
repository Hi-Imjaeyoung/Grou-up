package growup.spring.springserver.margin.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.exception.netsales.NetSalesNotFoundProductName;
import growup.spring.springserver.margin.TypeChangeMargin;
import growup.spring.springserver.margin.converter.MarginConverter;
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
import growup.spring.springserver.netsales.service.NetSalesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@AllArgsConstructor
@Slf4j
public class MarginService {

    private static final int TOP_N = 5;
    private final MarginRepository marginRepository;
    private final CampaignService campaignService;
    private final MarginForCampaignRepository marginForCampaignRepository;
    private final NetRepository netRepository;
    private final MarginConverterFactory marginConverterFactory;
    private final NetSalesService netSalesService;
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

    @Transactional
    public List<MarginResponseDto> getALLMargin(LocalDate start, LocalDate end, Long campaignId, String email) {

        // 1. 기간별 마진 데이터 가져옴
        List<Margin> margins = byCampaignIdAndDates(start, end, campaignId);

        // 2. 내가 가진 캠페인id 가져옴
        Campaign myCampaign = campaignService.getMyCampaign(campaignId, email);

        // 3. NetSales에서 데이터가 있는 날짜들만 가져온다.
        List<LocalDate> datesWithNetSales = netSalesService.getDatesWithNetSalesByEmailAndDateRange(start, end, email);

        // 4. NetSales에는 있지만, Margin에는 없는 날짜들 새로 만들어서 추가
        List<Margin> createNewMargin = createNewMargin(datesWithNetSales, margins, myCampaign);

        // 5. 업데이트 해야하는 Margin 리스트
        List<Margin> updatableMargins = getUpdatableMargins(margins, datesWithNetSales, createNewMargin);

        // 6. 업데이트 해야하는 Margin 리스트를 가지고 계산
        calculateMargin(updatableMargins, campaignId, email);

        // 7. 전략패턴 적용
        MarginConverter<MarginResultDto> converter = marginConverterFactory.getResultConverter();

        // 8. Margin 객체를 MarginResultDto로 변환
        List<MarginResultDto> marginResultDtos = getMarginResultDtos(margins, converter);

        // 9. 새로운 Margin 객체를 MarginResultDto로 변환
        return List.of(TypeChangeMargin.createMarginResponseDto(campaignId, marginResultDtos));

    }

    public List<MarginResultDto> getMarginResultDtos(List<Margin> margins, MarginConverter<MarginResultDto> converter) {
        return margins.stream()
                .map(converter::convert)
                .toList();
    }

    public List<Margin> createNewMargin(List<LocalDate> datesWithNetSales, List<Margin> margins,
                                        Campaign myCampaign) {
        List<Margin> createNewMargin = datesWithNetSales.stream()
                .filter(date -> margins.stream().noneMatch(m -> m.getMarDate().equals(date)))
                .map(date -> TypeChangeMargin.createSaveDefaultMargin(myCampaign, date))
                .toList();
        if (!createNewMargin.isEmpty()) {
            marginRepository.saveAll(createNewMargin);
        }
        return createNewMargin;
    }

    public List<Margin> getUpdatableMargins(List<Margin> margins, List<LocalDate> datesWithNetSales, List<Margin> createNewMargin ) {

        Set<LocalDate> datesWithNetSalesSet = new HashSet<>(datesWithNetSales); // Set으로 변환

        return Stream.concat(
                        margins.stream()
                                .filter(m -> m.getMarUpdated() == null || m.getMarUpdated())
                                .filter(m -> datesWithNetSalesSet.contains(m.getMarDate())),
                        createNewMargin.stream()
                )
                .toList();
    }


    public void calculateMargin(List<Margin> margins, Long campaignId, String email) {
        for (Margin margin : margins) {
            callNetSales(margin, campaignId, margin.getMarDate(), email);
        }
    }

    // NetSales 에서 가져와야함, 옵션명이랑 연결되어있음
    public void callNetSales(Margin margin, Long campaignId, LocalDate date, String email) {
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

                // 옵션별 순판매 - 순반품
                long mfcCount = netSales.getNetSalesCount() - netSales.getNetReturnCount();

                // 모든 옵션 포함 총 갯수에 옵션별 순 판매 더해줌
                actualSales += mfcCount;
                adMargin += mfcCount * data.getMfcPerPiece();
                returnCount += netSales.getNetReturnCount();
                returnCost += netSales.getNetReturnCount() * data.getMfcReturnPrice();
            } catch (NetSalesNotFoundProductName e) {
                continue;
            }
        }
        margin.update(actualSales, adMargin, returnCount, returnCost);

    }


    public List<Margin> byCampaignIdAndDates(LocalDate start, LocalDate end, Long campaignId) {

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

    public List<Margin> getAllMyCampaignMargin(LocalDate targetDate, List<Long> campaignList) {
        return marginRepository.findAllByCampaignCampaignIdInAndMarDate(campaignList, targetDate);
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

    @Transactional
    public int deleteMarginsForNetSale(LocalDate date, List<Long> campaignIds) {
        List<Margin> margins = getAllMyCampaignMargin(date, campaignIds);

        if (margins.isEmpty()) return 0;

        int count = 0;
        for (Margin m : margins) {
            m.deleteMarginAboutNetSale();
            count++;
        }
        return count;
    }

    public List<MarginOverviewResponseDto> getMarginOverview(LocalDate start, LocalDate end, String email) {

        List<Campaign> campaignList = getCampaignsByEmail(email);

        // 1. 내가 가진 캠페인 IDs 추출
        List<Long> campaignIds = extractCampaignIds(campaignList);

        // 2. 기간별 마진 오버뷰 전체 추출
        List<MarginOverviewResponseDto> allMarginOverviewByCampaignIdsAndDate = marginRepository.findMarginOverviewByCampaignIdsAndDate(start, end, campaignIds);

        // 2.1 사이즈가 6 이하면 그대로 보여주면 됨
        if (allMarginOverviewByCampaignIdsAndDate.size() <= TOP_N) {
            return allMarginOverviewByCampaignIdsAndDate;
        }
        // 3. top5 와, etc 분리,
        List<MarginOverviewResponseDto> top5 = allMarginOverviewByCampaignIdsAndDate.stream().limit(TOP_N).toList();
        List<MarginOverviewResponseDto> etcDto = allMarginOverviewByCampaignIdsAndDate.stream().skip(TOP_N).toList();

        // 4. 기타 항목 전체 계산
        MarginOverviewResponseDto othersSummary = TypeChangeMargin.createOthersSummary(etcDto);

        return Stream.concat(top5.stream(), Stream.of(othersSummary)).toList();
    }
}