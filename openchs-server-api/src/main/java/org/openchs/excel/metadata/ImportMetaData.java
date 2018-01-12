package org.openchs.excel.metadata;

import java.util.ArrayList;
import java.util.List;

public class ImportMetaData {
    private ImportNonCalculatedFields nonCalculatedFields;
    private ImportCalculatedFields calculatedFields;
    private ImportSheetMetaDataList importSheets;

    public ImportNonCalculatedFields getNonCalculatedFields() {
        return nonCalculatedFields;
    }

    public void setNonCalculatedFields(ImportNonCalculatedFields nonCalculatedFields) {
        this.nonCalculatedFields = nonCalculatedFields;
    }

    public ImportCalculatedFields getCalculatedFields() {
        return calculatedFields;
    }

    public void setCalculatedFields(ImportCalculatedFields calculatedFields) {
        this.calculatedFields = calculatedFields;
    }

    public ImportSheetMetaDataList getImportSheets() {
        return importSheets;
    }

    public void setImportSheets(ImportSheetMetaDataList importSheets) {
        this.importSheets = importSheets;
    }

    public List<ImportField> getAllFields(ImportSheetMetaData importSheetMetaData) {
        ArrayList<ImportField> importFields = new ArrayList<>();
        importFields.addAll(nonCalculatedFields.getFieldsFor(importSheetMetaData));
        importFields.addAll(calculatedFields.getFieldsFor(importSheetMetaData));
        importFields.addAll(importSheetMetaData.getDefaultFields());
        return importFields;
    }
}