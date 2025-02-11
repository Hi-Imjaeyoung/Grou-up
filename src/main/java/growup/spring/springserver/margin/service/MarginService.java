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
import growup.spring.springserver.netsales.domain.NetSales;
import growup.spring.springserver.netsales.repository.NetRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
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

        // 1번
        List<Margin> margins = byCampaignIdAndDates(start, end, campaignId);

        List<Margin> calculateMargin = calculateMargin(margins, campaignId, email);

        return TypeChangeMargin.getMarginDto(calculateMargin, campaignId);

    }


    private List<Margin> calculateMargin(List<Margin> margins, Long campaignId, String email) {
        // 보여줄 데이터
        List<Margin> datas = new ArrayList<>();
        for (Margin margin : margins) {
            // marAdMargin과 marNetProfit이 모두 0일 때 netSales를 호출
            // A 에 대해서 마진이 없음 => A에 대해서 업데이트 쳐야함
            if (margin.getMarAdMargin() == 0 && margin.getMarNetProfit() == 0.0) {
                Margin updateMargin = callNetSales(margin, campaignId, margin.getMarDate(), email);
                datas.add(updateMargin);
            } else {
                datas.add(margin);
            }
        }
        return datas;
    }

    // NetSales 에서 가져와야함, 옵션명이랑 연결되어있음
    private Margin callNetSales(Margin margin, Long campaignId, LocalDate date, String email) {
        // 해당 캠페인의 내가 추가한 모든 옵션들 가져옴
        List<MarginForCampaign> marginForCampaigns = marginForCampaignRepository.MarginForCampaignByCampaignId(campaignId);

        long actualSales = 0; // 순 판매 수
        long adMargin = 0; // 광고 머진

        // MarginForCampaign마다 netSales를 매칭해서 합산
        for (MarginForCampaign data : marginForCampaigns) {
            try {
                NetSales netSales = checkNetSales(date, email, data.getMfcProductName());
                // netSales가 존재하면 합산
                actualSales += netSales.getNetSalesCount();
                adMargin += netSales.getNetSalesCount() * data.getMfcPerPiece();
            } catch (NetSalesNotFoundProductName e) {
                continue;
            }
        }

        margin.update(actualSales, adMargin);

        return margin;
    }


    private List<Margin> byCampaignIdAndDates(LocalDate start, LocalDate end, Long campaignId) {

        return marginRepository.findByCampaignIdAndDates(campaignId, start, end);
    }

    private NetSales checkNetSales(LocalDate date, String email, String productName) {
        return netRepository.findByNetDateAndEmailAndNetProductName(date, email, productName).orElseThrow(
                NetSalesNotFoundProductName::new
        );
    }

    //    기간 별 마진, 바꾸기
    @Transactional
    public void marginUpdatesByPeriod(MfcRequestWithDatesDto mfcRequestWithDatesDto, String email) {
        LocalDate start = mfcRequestWithDatesDto.getStartDate();
        LocalDate end = mfcRequestWithDatesDto.getEndDate();
        Long campaignId = mfcRequestWithDatesDto.getCampaignId();

        // 1. 기간별 마진 데이터 가져옴
        List<Margin> margins = byCampaignIdAndDates(start, end, campaignId);

        // 2. 변경된 옵션 이름 리스트 보석 ,재영 있음
        Set<String> updatedProductNames = mfcRequestWithDatesDto.getData().stream()
                .map(MfcDto::getMfcProductName)
                .collect(Collectors.toSet());

        // 3. 변경된 옵션 데이터를 Map으로 변환 (상품명 기준으로 빠르게 찾기 위해)
        Map<String, MfcDto> updatedMarginsMap = mfcRequestWithDatesDto.getData().stream()
                .collect(Collectors.toMap(MfcDto::getMfcProductName, mfc -> mfc));

        List<MarginForCampaign> marginForCampaigns = marginForCampaignRepository.MarginForCampaignByCampaignId(campaignId);

        for (Margin data : margins) {
            long adMargin = 0; // 광고 머진
            // 보석, 재영, 은아 있음
            for (MarginForCampaign tempData : marginForCampaigns) {
                try {
                    // 해당 날짜 갯수 불러옴
                    NetSales netSalesList = checkNetSales(data.getMarDate(), email, tempData.getMfcProductName());

                    // 변경된 옵션 리스트일경우
                    long perPieceMargin = updatedProductNames.contains(tempData.getMfcProductName())
                            ? updatedMarginsMap.get(tempData.getMfcProductName()).getMfcPerPiece()
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
}