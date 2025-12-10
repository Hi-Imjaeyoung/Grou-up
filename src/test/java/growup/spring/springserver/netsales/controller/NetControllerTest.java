package growup.spring.springserver.netsales.controller;

import growup.spring.springserver.annotation.WithAuthUser;
import growup.spring.springserver.global.config.JwtTokenProvider;
import growup.spring.springserver.margin.controller.MarginController;
import growup.spring.springserver.margin.service.MarginService;
import growup.spring.springserver.netsales.dto.TotalSalesDto;
import growup.spring.springserver.netsales.service.NetSalesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(NetController.class)
class NetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NetSalesService netSalesService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @WithAuthUser
    @Test
    @DisplayName("getMyTotalSales")
    void getMyTotalSales() throws Exception {
        final String url = "/api/net/getMyTotalSales?start=2025-12-01&end=2025-12-01";

        TotalSalesDto dto = TotalSalesDto.builder().totalSalesPrice(1000.0).totalCancelPrice(100.0).build();

        when(netSalesService.getMyTotalSales(any(LocalDate.class), any(LocalDate.class), any(String.class)))
                .thenReturn(dto);

        ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(url)
                .with(csrf()));
        result.andExpectAll(
                status().isOk(),
                jsonPath("$.message").value("success : getMyTotalSales")
        );
    }
}