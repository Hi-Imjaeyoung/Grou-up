package growup.spring.springserver.margin.converter;

import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.dto.MarginResultDto;

public class MarginToMarginResultConverter implements MarginConverter<MarginResultDto> {

    @Override
    public MarginResultDto convert(Margin margin) {
        return MarginResultDto.builder()
                .id(margin.getId())
                .marDate(margin.getMarDate())
                .marImpressions(margin.getMarImpressions())
                .marClicks(margin.getMarClicks())
                .marAdConversionSales(margin.getMarAdConversionSales())
                .marAdConversionSalesCount(margin.getMarAdConversionSalesCount())
                .marReturnCount(margin.getMarReturnCount())
                .marReturnCost(margin.getMarReturnCost())
                .marAdCost(margin.getMarAdCost())
                .marSales(margin.getMarSales())
                .marAdMargin(margin.getMarAdMargin())
                .marNetProfit(margin.getMarNetProfit())
                .marTargetEfficiency(margin.getMarTargetEfficiency())
                .marAdBudget(margin.getMarAdBudget())
                .marActualSales(margin.getMarActualSales())
                .build();
    }
}
