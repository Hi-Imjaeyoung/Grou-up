package growup.spring.springserver.margin.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.margin.TypeChangeMargin;
import growup.spring.springserver.margin.converter.MarginConverter;
import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.dto.*;
import growup.spring.springserver.margin.factory.MarginConverterFactory;
import growup.spring.springserver.margin.repository.MarginRepository;
import growup.spring.springserver.margin.util.MarginCalcResult;
import growup.spring.springserver.margin.util.NetSalesKey;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import growup.spring.springserver.marginforcampaignchangedbyperiod.domain.MarginForCampaignChangedByPeriod;
import growup.spring.springserver.marginforcampaignchangedbyperiod.service.MarginForCampaignChangedByPeriodService;
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
    private final MarginForCampaignChangedByPeriodService marginForCampaignChangedByPeriodService;
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


    private List<Campaign> getCampaignsByEmail(String email) {
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
     *  getAllMargin() - 마진 전체 조회
     *  (1) 실제 팔린 데이터가 있는데 Margin 데이터가 없는경우 -> 새로운 Margin 데이터 생성한다.
     *  (2) 실제 팔린 데이터가 있는데 Margin 데이터가 있는경우 -> 마진 계산
     *  (3) 실제 팔린 데이터가 없는데 Margin 데이터가 있는경우 -> 마진 계산 X
     * *
     */
    @Transactional
    public List<MarginResponseDto> getALLMargin(LocalDate start, LocalDate end, Long campaignId, String email) {

    /*
        1. 기간별 Margin 데이터 가져오기
    */
        List<Margin> marginDataByPeriod = getMarginDataByPeriod(start, end, campaignId);
    /*
        2. Campaign Id로 내 캠페인 가져오기
    */
        Campaign myCampaign = campaignService.getMyCampaign(campaignId, email);
    /*
        3. NetSales 에 있는 날짜 리스트 가져오기 (실제로 팔린 날짜들)
    */
        List<LocalDate> datesWithNetSales = netSalesService.getDatesWithNetSalesByEmailAndDateRange(start, end, email);
    /*
        4. 새로운 Margin 리스트 생성
        why? NetSales 에는 있는데 Margin 에는 없는 날짜들에 대해서
    */
        List<Margin> createNewMargin = createNewMargin(datesWithNetSales, myCampaign);
    /*
        5. 업데이트 해야하는 Margin 리스트 추출

    */
        List<Margin> updatableMargins = getUpdatableMargins(marginDataByPeriod, datesWithNetSales, createNewMargin);
    /*
        6.1 날짜별 -> MarginForCampaignId별 변환값 있는지 체크 및 가져옴
        6.2 날짜별 -> NetSalesKey(상품명,타입)별 NetSales 값 가져옴
    */
        Map<LocalDate, Map<Long, MarginForCampaignChangedByPeriod>> getMarginChangesByCampaignAndDateRange = marginChangesByDate(start, end, campaignId);
        Map<LocalDate, Map<NetSalesKey, NetSales>> netSalesMap = getNetSalesMap(start, end, email);
    /*
        6.3 업데이트 해야하는 Margin 리스와, 캐시 된거 들고 마진 계산
    */
        calculateMargin(updatableMargins, campaignId, getMarginChangesByCampaignAndDateRange, netSalesMap);
        marginDataByPeriod.addAll(createNewMargin);

        MarginConverter<MarginResultDto> converter = marginConverterFactory.getResultConverter();

        List<MarginResultDto> marginResultDtos = getMarginResultDtos(marginDataByPeriod, converter);

        return List.of(TypeChangeMargin.createMarginResponseDto(campaignId, marginResultDtos));
    }


    public Map<LocalDate, Map<Long, MarginForCampaignChangedByPeriod>> marginChangesByDate(
            LocalDate start,
            LocalDate end,
            Long campaignId
    ) {
        List<Long> mfcIds = getMfcIdsByCampaignId(campaignId);

        return marginForCampaignChangedByPeriodService.findAllByMfcCbpIdsAndDateRange(
                mfcIds,
                start,
                end
        );
    }
    private List<MarginForCampaign> getMfcListByCampaignId(Long campaignId) {
        return marginForCampaignRepository
                .MarginForCampaignByCampaignId(campaignId);
    }

    private List<Long> getMfcIdsByCampaignId(Long campaignId) {
        return getMfcListByCampaignId(campaignId)
                .stream()
                .map(MarginForCampaign::getId)
                .toList();
    }

    public List<MarginResultDto> getMarginResultDtos(List<Margin> margins, MarginConverter<MarginResultDto> converter) {
        return margins.stream()
                .map(converter::convert)
                .toList();
    }


    public List<Margin> createNewMargin(List<LocalDate> datesWithNetSales,
                                        Campaign myCampaign) {

        Set<LocalDate> existingMarginDatesSet = getExistingMarginDates(datesWithNetSales, myCampaign);

        List<Margin> createNewMargin = datesWithNetSales
                .stream()
                .filter(date -> !existingMarginDatesSet.contains(date))
                .map(date -> TypeChangeMargin.createSaveDefaultMargin(myCampaign, date))
                .toList();
        if (!createNewMargin.isEmpty()) {
            marginRepository.saveAll(createNewMargin);
        }
        return createNewMargin;
    }

    private Set<LocalDate> getExistingMarginDates(List<LocalDate> datesWithNetSales, Campaign myCampaign) {
        return marginRepository.findExistingDatesByCampaignIdAndDateIn(
                myCampaign.getCampaignId(), datesWithNetSales);
    }

    // Margin 업데이트 해야하는 것들만 추출
// NetSales 에 있는 Margin 리스트 + 새로 생성한 Margin 리스트 합치기
    public List<Margin> getUpdatableMargins(List<Margin> margins, List<LocalDate> datesWithNetSales, List<Margin> createNewMargin) {
        Set<LocalDate> datesWithNetSalesSet = new HashSet<>(datesWithNetSales);
        return Stream.concat(
                margins.stream().filter(m -> datesWithNetSalesSet.contains(m.getMarDate())),
                createNewMargin.stream()
        ).toList();
    }

    /*
    마진 하루하루 돌면서, 수정 내역이 있으면 수정내역을 반영해야함 없으면 기본값으로 함
    * */
    public void calculateMargin(
            List<Margin> margins,
            Long campaignId,
            Map<LocalDate, Map<Long, MarginForCampaignChangedByPeriod>> getMarginChangesByCampaignAndDateRange,
            Map<LocalDate, Map<NetSalesKey, NetSales>> netSalesMap
    ) {
        // 캠페인의 모든 옵션 가져오기
        List<MarginForCampaign> mfcList = getMfcListByCampaignId(campaignId);

        // 날짜별 변경내역 확인
        for (Margin margin : margins) {
            LocalDate date = margin.getMarDate();
            // npe 방지
            Map<Long, MarginForCampaignChangedByPeriod> changesForDate = getMarginChangesByCampaignAndDateRange.getOrDefault(date, Collections.emptyMap());

            MarginCalcResult dailyTotal = mfcList
                    .stream()
                    .map(mfc -> dailyCalculate(mfc, date, changesForDate, netSalesMap))
                    .reduce(MarginCalcResult.createZero(), MarginCalcResult::add);

            margin.update(
                    dailyTotal.actualSales(),
                    dailyTotal.adMargin(),
                    dailyTotal.returnCount(),
                    dailyTotal.returnCost()
            );
        }

    }

    private MarginCalcResult dailyCalculate(MarginForCampaign mfc, LocalDate date, Map<Long, MarginForCampaignChangedByPeriod> changesForDate, Map<LocalDate, Map<NetSalesKey, NetSales>> netSalesMap) {
        return getNetSales(netSalesMap, mfc, date)
                .map(netSales -> {
                    MarginForCampaignChangedByPeriod changed = changesForDate.get(mfc.getId());
                    return calculateForOption(mfc, changed, netSales);
                })
                .orElse(MarginCalcResult.createZero());
    }

    private static Optional<NetSales> getNetSales(Map<LocalDate, Map<NetSalesKey, NetSales>> netSalesMap, MarginForCampaign mfc, LocalDate date) {

        NetSalesKey key = new NetSalesKey(mfc.getMfcProductName(), mfc.getMfcType());

        return Optional.ofNullable(netSalesMap.get(date))
                .map(innerMap -> innerMap.get(key));

    }

    private MarginCalcResult calculateForOption(
            MarginForCampaign mfc,
            MarginForCampaignChangedByPeriod changed,
            NetSales netSales
    ) {
        long actualSales = netSales.getNetSalesCount() - netSales.getNetReturnCount();

        long perPiece = (changed != null) ? changed.getSalePrice() : mfc.getMfcPerPiece();
        long returnPrice = (changed != null) ? changed.getReturnPrice() : mfc.getMfcReturnPrice();

        long adMargin = actualSales * perPiece;
        long returnCount = netSales.getNetReturnCount();
        double returnCost = netSales.getNetReturnCount() * (double) returnPrice;

        return new MarginCalcResult(actualSales, adMargin, returnCount, returnCost);
    }


    public List<Margin> getMarginDataByPeriod(LocalDate start, LocalDate end, Long campaignId) {
        return marginRepository.findByCampaignIdAndDates(campaignId, start, end);
    }

    public Map<LocalDate, Map<NetSalesKey, NetSales>> getNetSalesMap(
            LocalDate start,
            LocalDate end,
            String email
    ) {
        List<NetSales> list = netSalesService.getNetSalesByEmailAndDateRange(email, start, end);

        return list.stream()
                .collect(Collectors.groupingBy(
                        NetSales::getNetDate,
                        Collectors.toMap(
                                ns -> new NetSalesKey(ns.getNetProductName(), ns.getNetType()),
                                ns -> ns
                        )
                ));
    }


    public List<DailyMarginSummary> getDailyMarginSummary(List<Campaign> campaignList, LocalDate start, LocalDate end) {

        List<DailyMarginSummary> summaries = new ArrayList<>();

        // 보석, 재영, 은아
        for (Campaign campaign : campaignList) {
            List<Margin> margins = marginRepository.findByCampaignIdAndDates(campaign.getCampaignId(), start, end);
            String productName = campaign.getCamCampaignName();
            DailyMarginSummary dailyMarginSummary = DailyMarginSummary.builder()
                    .marAdMargin(0L)
                    .marNetProfit(0.0)
                    .marProductName(productName)
                    .build();
            for (Margin margin : margins) {
                dailyMarginSummary.setMarAdMargin(margin.getMarAdMargin() + dailyMarginSummary.getMarAdMargin());
                dailyMarginSummary.setMarNetProfit(margin.getMarNetProfit() + dailyMarginSummary.getMarNetProfit());
            }
            summaries.add(dailyMarginSummary);
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
        List<MarginOverviewResponseDto> top5 = allMarginOverviewByCampaignIdsAndDate.subList(0, TOP_N);
        List<MarginOverviewResponseDto> etcDto = allMarginOverviewByCampaignIdsAndDate.subList(Math.min(TOP_N, allMarginOverviewByCampaignIdsAndDate.size()), allMarginOverviewByCampaignIdsAndDate.size());

        // 4. 기타 항목 전체 계산
        MarginOverviewResponseDto othersSummary = TypeChangeMargin.createOthersSummary(etcDto);

        top5.add(othersSummary);

        return top5;
    }

    public List<DailyAdSummaryDto> getMarginOverviewGraph(LocalDate start, LocalDate end, String email) {

        List<Campaign> campaignList = getCampaignsByEmail(email);

        List<Long> campaignIds = extractCampaignIds(campaignList);


        return marginRepository.findMarginOverviewGraphByCampaignIdsAndDate(campaignIds, start, end);
    }

    public int deleteMarginByCampaignIds(List<Long> campaignIds){
        return marginRepository.deleteByCampaignId(campaignIds);
    }
}