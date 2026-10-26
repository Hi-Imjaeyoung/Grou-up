package growup.spring.springserver.execution;

import growup.spring.springserver.execution.domain.Execution;
import growup.spring.springserver.execution.dto.ExecutionMarginResDto;
import growup.spring.springserver.execution.dto.ExecutionResponseDto;

public class TypeChangeExecution {

    public static ExecutionMarginResDto entityToDto(Execution execution) {
        return ExecutionMarginResDto
                .builder()
                .exeId(execution.getExeId())
                .exeDetailCategory(execution.getExeProductName()) // 일단 카테고리 대신에 넣음
                .exeSalePrice(execution.getExeSalePrice())
                .exeTotalPrice(execution.getExeTotalPrice())
                .exeCostPrice(execution.getExeCostPrice())
                .build();
    }
    public static ExecutionResponseDto ExcutionEntityToDto(Execution execution) {
        return ExecutionResponseDto
                .builder()
                .exeId(execution.getExeId())
                .exeProductName(execution.getExeProductName())
                .build();
    }
    public static ExecutionResponseDto entityToDto(int requestDataSize, int dataSize) {
        return ExecutionResponseDto
                .builder()
                .requestNumber(requestDataSize)
                .responseNumber(dataSize)
                .build();
    }
}
