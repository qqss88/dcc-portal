package org.icgc.dcc.portal.model;

import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;

import java.util.List;
import java.util.Map;

import lombok.Value;
import lombok.val;

import org.icgc.dcc.portal.model.IndexModel.Kind;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Donor")
public class Donor {

  @ApiModelProperty(value = "Donor ID", required = true)
  String id;
  @ApiModelProperty(value = "Submitter Donor ID", required = true)
  String submittedDonorId;
  @ApiModelProperty(value = "Cancer Project", required = true)
  String projectId;
  @ApiModelProperty(value = "Cancer Project Name", required = true)
  String projectName;
  @ApiModelProperty(value = "Primary Site", required = true)
  String primarySite;
  @ApiModelProperty(value = "Tumour Subtype", required = true)
  String tumourSubtype;
  @ApiModelProperty(value = "Tumour Type", required = true)
  String tumourType;
  @ApiModelProperty(value = "SSM Count", required = true)
  Long ssmCount;
  @ApiModelProperty(value = "SSM Affected Genes", required = true)
  Long ssmAffectedGenes;
  @ApiModelProperty(value = "CNSM Data Exists", required = true)
  Boolean cnsmExists;
  @ApiModelProperty(value = "STSM Data Exists", required = true)
  Boolean stsmExists;
  @ApiModelProperty(value = "SGV Data Exists", required = true)
  Boolean sgvExists;

  @ApiModelProperty(value = "METH Seq Data Exists", required = true)
  Boolean methSeqExists;
  @ApiModelProperty(value = "METH Array Data Exists", required = true)
  Boolean methArrayExists;
  @ApiModelProperty(value = "EXP Seq Data Exists", required = true)
  Boolean expSeqExists;
  @ApiModelProperty(value = "EXP Array Data Exists", required = true)
  Boolean expArrayExists;

  @ApiModelProperty(value = "PEXP Data Exists", required = true)
  Boolean pexpExists;
  @ApiModelProperty(value = "miRNA Seq Data Exists", required = true)
  Boolean mirnaSeqExists;

  @ApiModelProperty(value = "JCN Data Exists", required = true)
  Boolean jcnExists;
  @ApiModelProperty(value = "Age At Diagnosis", required = true)
  Long ageAtDiagnosis;
  @ApiModelProperty(value = "Age At Diagnosis Group", required = true)
  String ageAtDiagnosisGroup;
  @ApiModelProperty(value = "Age At Enrollment", required = true)
  Long ageAtEnrollment;
  @ApiModelProperty(value = "Age At Last Followup", required = true)
  Long ageAtLastFollowup;
  @ApiModelProperty(value = "Interval Of Last Followup", required = true)
  Long intervalOfLastFollowup;
  @ApiModelProperty(value = "Relapse Interval", required = true)
  Long relapseInterval;
  @ApiModelProperty(value = "Survival Time", required = true)
  Long survivalTime;
  @ApiModelProperty(value = "Relapse Type", required = true)
  String relapseType;
  @ApiModelProperty(value = "diagnosisIcd10", required = true)
  String diagnosisIcd10;
  @ApiModelProperty(value = "Disease Status Last Followup", required = true)
  String diseaseStatusLastFollowup;
  @ApiModelProperty(value = "Gender", required = true)
  String gender;
  @ApiModelProperty(value = "Vital Status", required = true)
  String vitalStatus;
  @ApiModelProperty(value = "Tumour Stage At Diagnosis", required = true)
  String tumourStageAtDiagnosis;
  @ApiModelProperty(value = "Tumour Staging System At Diagnosis", required = true)
  String tumourStagingSystemAtDiagnosis;
  @ApiModelProperty(value = "Tumour Stage At Diagnosis - Supplemental", required = true)
  String tumourStageAtDiagnosisSupplemental;
  @ApiModelProperty(value = "Available Data Types", required = true)
  List<String> availableDataTypes;
  @ApiModelProperty(value = "Analysis Types", required = true)
  List<String> analysisTypes;

  @ApiModelProperty(value = "Prior Malignancy", required = true)
  String priorMalignancy;

  @ApiModelProperty(value = "Cancer type prior Malignancy", required = true)
  String cancerTypePriorMalignancy;

  @ApiModelProperty(value = "Caner history first degree relative", required = true)
  String cancerHistoryFirstDegreeRelative;

  @ApiModelProperty(value = "Study donor involved in", required = true)
  List<String> studyDonorInvolvedIn;

  @ApiModelProperty(value = "Specimen")
  List<Specimen> specimen;

  @ApiModelProperty(value = "Family")
  List<Family> family;

  @ApiModelProperty(value = "Therapy")
  List<Therapy> therapy;

  @ApiModelProperty(value = "Exposure")
  List<Exposure> exposure;

  @SuppressWarnings("unchecked")
  @JsonCreator
  public Donor(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(Kind.DONOR);
    id = (String) fieldMap.get(fields.get("id"));
    submittedDonorId = (String) fieldMap.get(fields.get("submittedDonorId"));
    projectId = (String) fieldMap.get(fields.get("projectId"));
    projectName = (String) fieldMap.get(fields.get("projectName"));
    primarySite = (String) fieldMap.get(fields.get("primarySite"));
    tumourType = (String) fieldMap.get(fields.get("tumourType"));
    tumourSubtype = (String) fieldMap.get(fields.get("tumourSubtype"));
    ssmAffectedGenes = getLong(fieldMap.get(fields.get("ssmAffectedGenes")));
    ssmCount = getLong(fieldMap.get(fields.get("ssmCount")));
    cnsmExists = (Boolean) fieldMap.get(fields.get("cnsmExists"));
    stsmExists = (Boolean) fieldMap.get(fields.get("stsmExists"));
    sgvExists = (Boolean) fieldMap.get(fields.get("sgvExists"));
    methSeqExists = (Boolean) fieldMap.get(fields.get("methSeqExists"));
    methArrayExists = (Boolean) fieldMap.get(fields.get("methArrayExists"));
    expSeqExists = (Boolean) fieldMap.get(fields.get("expSeqExists"));
    expArrayExists = (Boolean) fieldMap.get(fields.get("expArrayExists"));
    pexpExists = (Boolean) fieldMap.get(fields.get("pexpExists"));
    mirnaSeqExists = (Boolean) fieldMap.get(fields.get("mirnaSeqExists"));
    jcnExists = (Boolean) fieldMap.get(fields.get("jcnExists"));
    ageAtDiagnosis = getLong(fieldMap.get(fields.get("ageAtDiagnosis")));
    ageAtDiagnosisGroup = (String) fieldMap.get(fields.get("ageAtDiagnosisGroup"));
    ageAtEnrollment = getLong(fieldMap.get(fields.get("ageAtEnrollment")));
    ageAtLastFollowup = getLong(fieldMap.get(fields.get("ageAtLastFollowup")));
    intervalOfLastFollowup = getLong(fieldMap.get(fields.get("intervalOfLastFollowup")));
    relapseInterval = getLong(fieldMap.get(fields.get("relapseInterval")));
    survivalTime = getLong(fieldMap.get(fields.get("survivalTime")));
    diagnosisIcd10 = (String) fieldMap.get(fields.get("diagnosisIcd10"));
    diseaseStatusLastFollowup = (String) fieldMap.get(fields.get("diseaseStatusLastFollowup"));
    gender = (String) fieldMap.get(fields.get("gender"));
    vitalStatus = (String) fieldMap.get(fields.get("vitalStatus"));
    tumourStageAtDiagnosis = (String) fieldMap.get(fields.get("tumourStageAtDiagnosis"));
    tumourStagingSystemAtDiagnosis = (String) fieldMap.get(fields.get("tumourStagingSystemAtDiagnosis"));
    tumourStageAtDiagnosisSupplemental = (String) fieldMap.get(fields.get("tumourStageAtDiagnosisSupplemental"));
    relapseType = (String) fieldMap.get(fields.get("relapseType"));
    availableDataTypes = (List<String>) fieldMap.get(fields.get("availableDataTypes"));
    analysisTypes = (List<String>) fieldMap.get(fields.get("analysisTypes"));

    priorMalignancy = (String) fieldMap.get(fields.get("priorMalignancy"));
    cancerTypePriorMalignancy = (String) fieldMap.get(fields.get("cancerTypePriorMalignancy"));
    cancerHistoryFirstDegreeRelative = (String) fieldMap.get(fields.get("cancerHistoryFirstDegreeRelative"));
    studyDonorInvolvedIn = (List<String>) fieldMap.get(fields.get("studyDonorInvolvedIn"));

    specimen = buildSpecimen((List<Map<String, Object>>) fieldMap.get("specimen"));
    therapy = buildTherapy((List<Map<String, Object>>) fieldMap.get("therapy")); // FIXME
    family = buildFamily((List<Map<String, Object>>) fieldMap.get("family")); // FIXME
    exposure = buildExposure((List<Map<String, Object>>) fieldMap.get("exposure")); // FIXME
  }

  private Long getLong(Object field) {
    if (field instanceof Long) return (Long) field;
    else if (field instanceof Integer) return (long) (Integer) field;
    else if (field instanceof Float) return ((Float) field).longValue();
    else
      return null;
  }

  private List<Exposure> buildExposure(List<Map<String, Object>> field) {
    if (field == null) return null;
    val lst = Lists.<Exposure> newArrayList();
    for (Map<String, Object> item : field) {
      lst.add(new Exposure(item));
    }
    return lst;
  }

  private List<Family> buildFamily(List<Map<String, Object>> field) {
    if (field == null) return null;
    val lst = Lists.<Family> newArrayList();
    for (Map<String, Object> item : field) {
      lst.add(new Family(item));
    }
    return lst;
  }

  private List<Therapy> buildTherapy(List<Map<String, Object>> field) {
    if (field == null) return null;
    val lst = Lists.<Therapy> newArrayList();
    for (Map<String, Object> item : field) {
      lst.add(new Therapy(item));
    }
    return lst;
  }

  private List<Specimen> buildSpecimen(List<Map<String, Object>> field) {
    if (field == null) return null;
    val lst = Lists.<Specimen> newArrayList();
    for (Map<String, Object> item : field) {
      lst.add(new Specimen(item));
    }
    return lst;
  }
}
