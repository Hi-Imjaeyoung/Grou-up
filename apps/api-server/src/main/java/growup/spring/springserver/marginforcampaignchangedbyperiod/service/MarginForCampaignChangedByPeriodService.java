package growup.spring.springserver.marginforcampaignchangedbyperiod.service;

import growup.spring.springserver.marginforcampaign.service.MarginForCampaignService;
import growup.spring.springserver.marginforcampaignchangedbyperiod.TypeChangeMarginForCampaignChangedByPeriod;
import org.springframework.transaction.annotation.Transactional;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaignchangedbyperiod.domain.MarginForCampaignChangedByPeriod;
import growup.spring.springserver.marginforcampaignchangedbyperiod.dto.MarginChangeSaveRequestDto;
import growup.spring.springserver.marginforcampaignchangedbyperiod.repository.MarginForCampaignChangedByPeriodRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class MarginForCampaignChangedByPeriodService {

    private final MarginForCampaignChangedByPeriodRepository marginForCampaignChangedByPeriodRepository;


    private final MarginForCampaignService marginForCampaignService;

    @Transactional
    public void save(MarginChangeSaveRequestDto dto) {
        Long mfcId = dto.mfcId();
        MarginForCampaign mfc = marginForCampaignService.getMyMarginForCampaignById(mfcId);

        Map<LocalDate, MarginForCampaignChangedByPeriod> existingMap = getLocalDateMarginForCampaignChangedByPeriodMap(dto, mfc);
        saveOrUpdateForDate(existingMap, dto, mfc);
    }

    /*
        save() 1. 날짜 범위 내 기존 데이터를 한번에 조회해서 Map 형태로 변환해둔다.
     */
    private Map<LocalDate, MarginForCampaignChangedByPeriod> getLocalDateMarginForCampaignChangedByPeriodMap(MarginChangeSaveRequestDto dto, MarginForCampaign mfc) {

        return marginForCampaignChangedByPeriodRepository
                .findAllByMarginForCampaign_IdAndDateRange(mfc.getId(), dto.startDate(), dto.endDate())
                .stream()
                .collect(
                        Collectors.toMap(
                                MarginForCampaignChangedByPeriod::getDate,
                                existingRecord -> existingRecord)
                );
    }

     /*
        save() 2. 날짜 별로 존재 여부 확인 후 하루 단위로 업데이트 및 생성한다.
     */
    private void saveOrUpdateForDate(Map<LocalDate, MarginForCampaignChangedByPeriod> existingMap, MarginChangeSaveRequestDto dto, MarginForCampaign mfc) {
        // bulk 리스트 생성
        List<MarginForCampaignChangedByPeriod> newRecords = new ArrayList<>();
        for (LocalDate date = dto.startDate(); !date.isAfter(dto.endDate()); date = date.plusDays(1)) {
            if (existingMap.containsKey(date)) {
                // 기존 데이터가 있으면 업데이트
                MarginForCampaignChangedByPeriod existingRecord = existingMap.get(date);
                existingRecord.updateFromDto(dto);
            } else {
                // 기존 데이터가 없으면 새로 생성
                newRecords.add(createNew(dto, mfc, date));
            }
        }
        // 새로운 데이터 일괄 저장
        if (!newRecords.isEmpty()) {
            marginForCampaignChangedByPeriodRepository.saveAll(newRecords);
        }

    }

    /*
        save() 3. 새로운 데이터 생성 메서드
     */
    private MarginForCampaignChangedByPeriod createNew(MarginChangeSaveRequestDto dto, MarginForCampaign mfc, LocalDate date) {
        return TypeChangeMarginForCampaignChangedByPeriod.of(dto, mfc, date);
    }

    /*
        기간내 존재하는 날짜별, MfcCbp Id별 MarginForCampaignChangedByPeriod 맵 조회
    */
    public Map<LocalDate, Map<Long, MarginForCampaignChangedByPeriod>> findAllByMfcCbpIdsAndDateRange(List<Long> mfcIds, LocalDate startDate, LocalDate endDate) {

        List<MarginForCampaignChangedByPeriod> allByMfcIdsAndDateRange = marginForCampaignChangedByPeriodRepository.findAllByMfcIdsAndDateRange(mfcIds, startDate, endDate);
        return allByMfcIdsAndDateRange.stream()
                .collect(Collectors.groupingBy(
                        MarginForCampaignChangedByPeriod::getDate, // 1차 Key: 날짜
                        Collectors.toMap(
                                mcpCbp -> mcpCbp.getMarginForCampaign().getId(), // Key: MfcID
                                mcpCbp -> mcpCbp // Value: 수정내역 객체 (mcpCbp) 있으면
                        )
                ));
    }
    @Transactional
    public void findByMarginForCampaignIdAndDelete(Long id) {
        marginForCampaignChangedByPeriodRepository.deleteByMfcId(id);
    }
}
