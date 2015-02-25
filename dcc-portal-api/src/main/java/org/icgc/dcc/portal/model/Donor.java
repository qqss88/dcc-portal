package org.icgc.dcc.portal.model;

import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getBoolean;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getLong;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getString;

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
  @ApiModelProperty(value = "Specimen")
  List<Specimen> specimen;

  @SuppressWarnings("unchecked")
  @JsonCreator
  public Donor(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(Kind.DONOR);
    id = getString(fieldMap.get(fields.get("id")));
    submittedDonorId = getString(fieldMap.get(fields.get("submittedDonorId")));
    projectId = getString(fieldMap.get(fields.get("projectId")));
    projectName = getString(fieldMap.get(fields.get("projectName")));
    primarySite = getString(fieldMap.get(fields.get("primarySite")));
    tumourType = getString(fieldMap.get(fields.get("tumourType")));
    tumourSubtype = getString(fieldMap.get(fields.get("tumourSubtype")));
    ssmAffectedGenes = getLong(fieldMap.get(fields.get("ssmAffectedGenes")));
    ssmCount = getLong(fieldMap.get(fields.get("ssmCount")));
    cnsmExists = getBoolean(fieldMap.get(fields.get("cnsmExists")));
    stsmExists = getBoolean(fieldMap.get(fields.get("stsmExists")));
    sgvExists = getBoolean(fieldMap.get(fields.get("sgvExists")));
    methSeqExists = getBoolean(fieldMap.get(fields.get("methSeqExists")));
    methArrayExists = getBoolean(fieldMap.get(fields.get("methArrayExists")));
    expSeqExists = getBoolean(fieldMap.get(fields.get("expSeqExists")));
    expArrayExists = getBoolean(fieldMap.get(fields.get("expArrayExists")));
    pexpExists = getBoolean(fieldMap.get(fields.get("pexpExists")));
    mirnaSeqExists = getBoolean(fieldMap.get(fields.get("mirnaSeqExists")));
    jcnExists = getBoolean(fieldMap.get(fields.get("jcnExists")));
    ageAtDiagnosis = getLong(fieldMap.get(fields.get("ageAtDiagnosis")));
    ageAtDiagnosisGroup = getString(fieldMap.get(fields.get("ageAtDiagnosisGroup")));
    ageAtEnrollment = getLong(fieldMap.get(fields.get("ageAtEnrollment")));
    ageAtLastFollowup = getLong(fieldMap.get(fields.get("ageAtLastFollowup")));
    intervalOfLastFollowup = getLong(fieldMap.get(fields.get("intervalOfLastFollowup")));
    relapseInterval = getLong(fieldMap.get(fields.get("relapseInterval")));
    survivalTime = getLong(fieldMap.get(fields.get("survivalTime")));
    diagnosisIcd10 = getString(fieldMap.get(fields.get("diagnosisIcd10")));
    diseaseStatusLastFollowup = getString(fieldMap.get(fields.get("diseaseStatusLastFollowup")));
    gender = getString(fieldMap.get(fields.get("gender")));
    vitalStatus = getString(fieldMap.get(fields.get("vitalStatus")));
    tumourStageAtDiagnosis = getString(fieldMap.get(fields.get("tumourStageAtDiagnosis")));
    tumourStagingSystemAtDiagnosis = getString(fieldMap.get(fields.get("tumourStagingSystemAtDiagnosis")));
    tumourStageAtDiagnosisSupplemental = getString(fieldMap.get(fields.get("tumourStageAtDiagnosisSupplemental")));
    relapseType = getString(fieldMap.get(fields.get("relapseType")));
    availableDataTypes = (List<String>) fieldMap.get(fields.get("availableDataTypes"));
    analysisTypes = (List<String>) fieldMap.get(fields.get("analysisTypes"));
    specimen = buildSpecimen((List<Map<String, Object>>) fieldMap.get("specimen"));
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
