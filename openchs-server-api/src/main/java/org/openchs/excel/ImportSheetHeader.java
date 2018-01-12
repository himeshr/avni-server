package org.openchs.excel;

import org.apache.poi.xssf.usermodel.XSSFRow;

import java.util.HashMap;
import java.util.Map;

public class ImportSheetHeader {
    private Map<String, Integer> map = new HashMap<>();

    public ImportSheetHeader(XSSFRow row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            String text = ExcelUtil.getText(row, i);
            if (text.isEmpty()) break;

            map.put(text, i);
        }
    }

    public int getPosition(String userFieldName) {
        return map.get(userFieldName);
    }
}