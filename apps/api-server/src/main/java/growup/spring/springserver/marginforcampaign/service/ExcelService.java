package growup.spring.springserver.marginforcampaign.service;

import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.dto.ExcelUploadContext;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.util.*;

@Service
@Slf4j
@AllArgsConstructor
public class ExcelService {
    private final MarginForCampaignRepository marginForCampaignRepository;
    private final JdbcTemplate jdbcTemplate; // 의존성 주입 추가

    @Transactional
    public Map<String,Integer> processUploadedExcel(ExcelUploadContext excelUploadContext) throws IOException {
        deleteOldMfcNotExistExcel(excelUploadContext);
        saveMfcInExcel(excelUploadContext.getEntitiesToInsert());
        bulkUpdateMfc(excelUploadContext.getEntitiesToUpdate());
        return excelUploadContext.getResult();
        }

    public void checkNotExistOptionsAndDelete(List<String> optionNamesInExcels, String email,Long campaignId){
        marginForCampaignRepository.deleteNotIncludeOptionName(optionNamesInExcels,email,campaignId);
    }
    private void deleteOldMfcNotExistExcel(ExcelUploadContext excelUploadContext){
        String email = excelUploadContext.getEmail();
        if (excelUploadContext.getOptionNamesFromExcel().isEmpty()) {
            return;
        }
        for(Long campaignId : excelUploadContext.getOptionNamesFromExcel().keySet()){
            if(excelUploadContext.getOptionNamesFromExcel().get(campaignId).isEmpty()) continue;
            checkNotExistOptionsAndDelete(excelUploadContext.getOptionNamesFromExcel().get(campaignId), email,campaignId);
        }
    }
    private void saveMfcInExcel(List<MarginForCampaign> entitiesToInsert){
        // 1. 순수 벌크 인서트 (새로운 데이터)
        if (!entitiesToInsert.isEmpty()) {
            String sql = "INSERT INTO margin_for_campaign " +
                    "(campaign_id, mfc_product_name, mfc_sale_price, mfc_cost_price, mfc_total_price, mfc_return_price, mfc_type) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.batchUpdate(sql,
                    entitiesToInsert,
                    1000, // Batch Size (DB 설정에 맞게 조절, 보통 500~1000)
                    (PreparedStatement ps, MarginForCampaign mfc) -> {
                        ps.setLong(1, mfc.getCampaign().getCampaignId());
                        ps.setString(2, mfc.getMfcProductName());
                        ps.setLong(3, mfc.getMfcSalePrice());
                        ps.setLong(4, mfc.getMfcCostPrice());
                        ps.setLong(5, mfc.getMfcTotalPrice());
                        ps.setLong(6, mfc.getMfcReturnPrice());
                        ps.setString(7, mfc.getMfcType().name());
                    }
            );
        }
    }
    private void bulkUpdateMfc(List<MarginForCampaign> entitiesToUpdate) {
        if(entitiesToUpdate.isEmpty()) return;
        String sql = "UPDATE margin_for_campaign SET " +
                "mfc_product_name = ?, mfc_sale_price = ?, mfc_cost_price = ?, " +
                "mfc_total_price = ?, mfc_return_price = ?, mfc_type = ? " +
                "WHERE mfc_id = ?"; // 반드시 PK 기준 업데이트

        jdbcTemplate.batchUpdate(sql,
                entitiesToUpdate,
                1000,
                (PreparedStatement ps, MarginForCampaign mfc) -> {
                    ps.setString(1, mfc.getMfcProductName());
                    ps.setLong(2, mfc.getMfcSalePrice());
                    ps.setLong(3, mfc.getMfcCostPrice());
                    ps.setLong(4, mfc.getMfcTotalPrice());
                    ps.setLong(5, mfc.getMfcReturnPrice());
                    ps.setString(6, mfc.getMfcType().name());
                    ps.setLong(7, mfc.getId()); // PK 바인딩
                }
        );
    }
}

