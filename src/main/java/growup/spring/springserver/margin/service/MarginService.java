package growup.spring.springserver.margin.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.exception.netsales.NetSalesNotFoundProductName;
import growup.spring.springserver.margin.TypeChangeMargin;
import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.dto.*;
import growup.spring.springserver.margin.repository.MarginRepository;
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
        Long campaignId = campaign.getCampaignId();
        List<Margin> campaignMargins = marginMap.getOrDefault(campaignId, new ArrayList<>());

        return campaignMargins.stream()
                .filter(m -> m.getMarDate().equals(date))
                .findFirst()
                .orElse(TypeChangeMargin.createDefaultMargin(campaign, date));
    }

    /*
     * TODO
     *  1. startDate ~ endDate 까지 마진데이터 다 가져온다.
     *  2.1 startDate 부터 하루씩 늘리면서 계산해서 넣는 것들이 있는 지 확인한다.
     *  2.2 만약에 비어있는 경우, NetSales 호출 후 계산해서 다시 집어넣어준다.
     * */
    @Transactional
    public List<MarginResponseDto> getALLMargin(LocalDate start, LocalDate end, Long campaignId, String email) {
        // 1. 기존 마진 데이터 조회
        List<Margin> margins = byCampaignIdAndDates(start, end, campaignId);
        Campaign myCampaign = campaignService.getMyCampaign(campaignId, email);

        // 2. 조회된 날짜를 Set으로 변환
        Set<LocalDate> existingDates = margins.stream()
                .map(Margin::getMarDate)
                .collect(Collectors.toSet());

        // NetSales 데이터가 있는 날짜만 필터링
        List<LocalDate> datesWithNetSales = netRepository.findDatesWithNetSalesByEmailAndDateRange(
                email, start, end);

        List<LocalDate> newMarginDates = datesWithNetSales.stream()
                .filter(date -> !existingDates.contains(date))
                .toList();

        // 4. 필요한 기본 Margin 생성 (NetSales 있는 날짜만)
        List<Margin> newMargins = newMarginDates.stream()
                .map(date -> TypeChangeMargin.createSaveDefaultMargin(myCampaign, date)) // 메서드 호출로 변경
                .toList();

        // 5. 새 Margin 데이터 저장
        if (!newMargins.isEmpty()) {
            marginRepository.saveAll(newMargins);
        }

        // 6. 기존 Margin + 새로 생성된 Margin 합쳐서 계산
        List<Margin> fullMargins = new ArrayList<>(margins);
        fullMargins.addAll(newMargins);

        // 7. NetSales 데이터를 활용해 마진 업데이트
        List<Margin> calculateMargin = calculateMargin(fullMargins, campaignId, email);

        return TypeChangeMargin.getMarginDto(calculateMargin, campaignId);
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

//        // 2. 변경된 옵션 이름 리스트 보석 ,재영 있음
//        Set<String> updatedProductNames = mfcRequestWithDatesDto.getData().stream()
//                .map(MfcDto::getMfcProductName)
//                .collect(Collectors.toSet());
//
//        // 3. 변경된 옵션 데이터를 Map으로 변환 (상품명 기준으로 빠르게 찾기 위해)
//        Map<String, MfcDto> updatedMarginsMap = mfcRequestWithDatesDto.getData().stream()
//                .collect(Collectors.toMap(MfcDto::getMfcProductName, mfc -> mfc));

        Map<MfcKey, MfcDto> updatedMarginsMap = mfcRequestWithDatesDto.getData().stream()
                .collect(Collectors.toMap(
                        mfc -> new MfcKey(mfc.getMfcProductName(), mfc.getMfcType()),
                        mfc -> mfc
                ));

        List<MarginForCampaign> marginForCampaigns = marginForCampaignRepository.MarginForCampaignByCampaignId(campaignId);

        for (Margin data : margins) {
            long adMargin = 0;

            for (MarginForCampaign tempData : marginForCampaigns) {
                try {
                    // 해당 날짜 순 매출 건수 및 반품 건수
                    NetSales netSalesList = checkNetSales(data.getMarDate(), email, tempData.getMfcProductName(), tempData.getMfcType());

                    // MfcKey 생성
                    MfcKey key = new MfcKey(tempData.getMfcProductName(), tempData.getMfcType());

                    // 변경된 값이 있으면 가져오고, 없으면 기존 값 사용
                    long perPieceMargin = updatedMarginsMap.containsKey(key)
                            ? updatedMarginsMap.get(key).getMfcPerPiece()
                            : tempData.getMfcPerPiece();

                    adMargin += netSalesList.getNetSalesCount() * perPieceMargin;
                } catch (NetSalesNotFoundProductName e) {
                    continue;
                }
            }
            data.update(adMargin);
        }
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

    public int deleteKeywordByCampaignIdsAndDate(List<Long> campaignIds, LocalDate start, LocalDate end){
        return marginRepository.deleteByCampaignIdAndDate(start,end,campaignIds);
    }
}