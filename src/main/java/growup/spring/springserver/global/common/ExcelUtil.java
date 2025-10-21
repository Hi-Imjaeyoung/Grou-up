package growup.spring.springserver.global.common;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.List;
import java.util.Map;

public class ExcelUtil {
    public static Workbook createExcelFile(List<Map<String, Object>> dataList, List<String> headers, List<String> dataKeys) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("사용자 목록");
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
        }
        for (int i = 0; i < dataList.size(); i++) {
            Row dataRow = sheet.createRow(i + 1);
            Map<String, Object> data = dataList.get(i);
            for(int j=0; j<dataKeys.size(); j++) {
                Cell cell = dataRow.createCell(j);
                Object value = data.get(dataKeys.get(j));
                if (value instanceof String) {
                    cell.setCellValue((String) value);
                } else if (value instanceof Integer) {
                    cell.setCellValue((Integer) value);
                } else if (value instanceof Long) {
                    cell.setCellValue((Long) value);
                }
            }
        }
        // 5. 컬럼 너비 자동 조정
        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i); // 2. 현재 너비를 가져온다.
            // 3. 현재 너비에 추가 여백(약 5글자 분량)을 더해 다시 설정한다.
            sheet.setColumnWidth(i, currentWidth + (5 * 256));
        }
        return workbook;
    }
}
