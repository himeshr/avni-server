package org.openchs.excel.metadata;

import org.openchs.application.FormType;
import org.openchs.util.Mappings;

import java.util.ArrayList;
import java.util.List;

public class ImportSheetMetaData {
    private String fileName;
    private String userFileType;
    private String sheetName;
    private Class entityType;
    private String programName;
    private String encounterType;

    private List<ImportDefaultField> sheetDefaults = new ArrayList<>();

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUserFileType() {
        return userFileType;
    }

    public void setUserFileType(String userFileType) {
        this.userFileType = userFileType;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public Class getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = Mappings.ENTITY_TYPES.get(entityType);
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getEncounterType() {
        return encounterType;
    }

    public void setEncounterType(String encounterType) {
        this.encounterType = encounterType;
    }

    public void addDefaultValue(String systemFieldName, String defaultValue) {
        ImportDefaultField defaultField = new ImportDefaultField(systemFieldName, defaultValue);
        sheetDefaults.add(defaultField);
    }

    public FormType getFormType() {
        return Mappings.ENTITY_TYPE_FORM_TYPE_MAP.get(this.getEntityType());
    }

    public List<ImportField> getDefaultFields() {
        ArrayList<ImportField> list = new ArrayList<>();
        list.addAll(sheetDefaults);
        return list;
    }
}