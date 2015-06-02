package org.icgc.dcc.portal.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@Component
public class IndexModel {

  /**
   * Constants.
   */
  public static final int MAX_FACET_TERM_COUNT = 1024;
  public static final String IS = "is";
  public static final String ALL = "all";
  public static final String NOT = "not";
  public static final String MISSING = "_missing";

  public static final Set<String> GENES_SORT_FIELD_NAMES = ImmutableSet.of("symbol", "name", "start", "type",
      "affectedDonorCountFiltered");

  /**
   * Special cases for term lookups
   */
  public static final String API_ENTITY_LIST_ID_FIELD_NAME = "entitySetId";

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  public static enum Kind {
    PROJECT("project"),
    DONOR("donor"),
    GENE("gene"),
    MUTATION("mutation"),
    PATHWAY("pathway"),

    GENE_SET("geneSet"),
    REPOSITORY_FILE("file"),

    CONSEQUENCE("consequence"),
    TRANSCRIPT("transcript"),
    OCCURRENCE("occurrence"),
    EMB_OCCURRENCE("embOccurrence"),
    OBSERVATION("observation"),
    RELEASE("release"),
    KEYWORD(""),
    SPECIMEN(""),
    SAMPLE(""),
    SEQ_DATA(""),
    DOMAIN(""),
    EXON(""),
    FAMILY(""),
    EXPOSURE(""),
    THERAPY("");

    private final String id;
  }

  // Index document type
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  public static enum Type {
    PROJECT("project"),
    DONOR("donor"),
    GENE("gene"),
    MUTATION("mutation"),
    RELEASE("release"),
    PATHWAY("pathway"),
    GENE_SET("gene-set"),
    DONOR_CENTRIC("donor-centric"),
    GENE_CENTRIC("gene-centric"),
    MUTATION_CENTRIC("mutation-centric"),
    OCCURRENCE_CENTRIC("observation-centric"),
    REPOSITORY_FILE("file"),

    DONOR_TEXT("donor-text"),
    GENE_TEXT("gene-text"),
    MUTATION_TEXT("mutation-text"),
    PATHWAY_TEXT("pathway-text"),
    GENESET_TEXT("gene-set-text"),
    REPOSITORY_FILE_TEXT("file-text"),
    PROJECT_TEXT("project-text");

    private final String id;
  }

  private static final ImmutableMap<String, String> REPOSITORY_FILE_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "id")
          .put("repositoryEntityId", "repository.repo_entity_id")
          .put("repositoryDataPath", "repository.repo_data_path")
          .put("repositoryBaseURLs", "repository.repo_server.repo_base_url") // This is a list

          .put("repositoryType", "repository.repo_type")
          .put("repositoryCountries", "repository.repo_server.repo_country")
          .put("repositoryNames", "repository.repo_server.repo_name")
          .put("fileName", "repository.file_name")
          .put("fileSize", "repository.file_size")
          .put("checksum", "repository.file_md5sum")
          .put("lastUpdate", "repository.last_modified")
          .put("projectCode", "donor.project_code")
          .put("primarySite", "donor.primary_site")
          .put("study", "study")
          .put("donorStudy", "donor.study")
          .put("dataType", "data_types.data_type")
          .put("dataFormat", "data_types.data_format")
          .put("access", "access")
          .put("donorId", "donor.donor_id")
          .put("donorSubmitterId", "donor.submitted_donor_id")
          .put("specimenId", "donor.specimen_id")
          .put("specimenSubmitterId", "donor.submitted_specimen_id")
          .put("sampleId", "donor.sample_id")
          .put("sampleSubmitterId", "donor.submitted_sample_id")

          .put("program", "donor.program") // For search
          .build();

  private static final ImmutableMap<String, String> PROJECTS_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "_project_id")
          .put("icgcId", "icgc_id")
          .put("primarySite", "primary_site")
          .put("name", "project_name")
          .put("tumourType", "tumour_type")
          .put("tumourSubtype", "tumour_subtype")
          .put("pubmedIds", "pubmed_ids")
          .put("primaryCountries", "primary_countries").put("partnerCountries", "partner_countries")
          .put("availableDataTypes", "_summary._available_data_type")
          .put("ssmTestedDonorCount", "_summary._ssm_tested_donor_count")
          .put("cnsmTestedDonorCount", "_summary._cnsm_tested_donor_count")
          .put("stsmTestedDonorCount", "_summary._stsm_tested_donor_count")
          .put("sgvTestedDonorCount", "_summary._sgv_tested_donor_count")
          .put("methSeqTestedDonorCount", "_summary._meth_seq_tested_donor_count")
          .put("methArrayTestedDonorCount", "_summary._meth_array_tested_donor_count")
          .put("expSeqTestedDonorCount", "_summary._exp_seq_tested_donor_count")
          .put("expArrayTestedDonorCount", "_summary._exp_array_tested_donor_count")
          .put("pexpTestedDonorCount", "_summary._pexp_tested_donor_count")
          .put("mirnaSeqTestedDonorCount", "_summary._mirna_seq_tested_donor_count")
          .put("jcnTestedDonorCount", "_summary._jcn_tested_donor_count")
          // .put("totalDonorCount", "_summary._total_donor_count")
          .put("totalDonorCount", "_summary._total_complete_donor_count")
          .put("affectedDonorCount", "_summary._affected_donor_count")
          .put("experimentalAnalysisPerformedDonorCounts", "_summary.experimental_analysis_performed_donor_count")
          .put("experimentalAnalysisPerformedSampleCounts", "_summary.experimental_analysis_performed_sample_count")
          .put("repository", "_summary.repository")
          .build();

  private static final ImmutableMap<String, String> DONORS_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "_donor_id")
          .put("submittedDonorId", "donor_id")
          .put("projectId", "project._project_id")
          .put("primarySite", "project.primary_site")
          .put("projectName", "project.project_name")
          .put("tumourType", "project.tumour_type")
          .put("tumourSubtype", "project.tumour_subtype")
          .put("ssmCount", "_summary._ssm_count")
          .put("cnsmExists", "_summary._cnsm_exists")
          .put("stsmExists", "_summary._stsm_exists")
          .put("sgvExists", "_summary._sgv_exists")
          .put("pexpExists", "_summary._pexp_exists")
          .put("mirnaSeqExists", "_summary._mirna_seq_exists")
          .put("methSeqExists", "_summary._meth_seq_exists")
          .put("methArrayExists", "_summary._meth_array_exists")
          .put("expSeqExists", "_summary._exp_seq_exists")
          .put("expArrayExists", "_summary._exp_array_exists")
          .put("jcnExists", "_summary._jcn_exists")
          .put("ageAtDiagnosis", "donor_age_at_diagnosis")
          .put("ageAtDiagnosisGroup", "_summary._age_at_diagnosis_group")
          .put("ageAtEnrollment", "donor_age_at_enrollment")
          .put("ageAtLastFollowup", "donor_age_at_last_followup")
          .put("diagnosisIcd10", "donor_diagnosis_icd10")
          .put("diseaseStatusLastFollowup", "disease_status_last_followup")
          .put("intervalOfLastFollowup", "donor_interval_of_last_followup")
          .put("gender", "donor_sex")
          .put("vitalStatus", "donor_vital_status")
          .put("tumourStageAtDiagnosis", "donor_tumour_stage_at_diagnosis")
          .put("tumourStagingSystemAtDiagnosis", "donor_tumour_staging_system_at_diagnosis")
          .put("tumourStageAtDiagnosisSupplemental", "donor_tumour_stage_at_diagnosis_supplemental")
          .put("relapseType", "donor_relapse_type")
          .put("relapseInterval", "donor_relapse_interval")
          .put("survivalTime", "donor_survival_time")
          .put("availableDataTypes", "_summary._available_data_type")
          .put("analysisTypes", "_summary.experimental_analysis_performed")
          .put("studies", "_summary._studies")
          .put("ssmAffectedGenes", "_score")
          .put("priorMalignancy", "prior_malignancy")
          .put("cancerTypePriorMalignancy", "cancer_type_prior_malignancy")
          .put("cancerHistoryFirstDegreeRelative", "cancer_history_first_degree_relative")
          .put("complete", "_summary._complete")
          .put(API_ENTITY_LIST_ID_FIELD_NAME, API_ENTITY_LIST_ID_FIELD_NAME)
          .build();

  private static final ImmutableMap<String, String> FAMILY_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("donorHasRelativeWithCancerHistory", "donor_has_relative_with_cancer_history")
          .put("relationshipType", "relationship_type")
          .put("relationshipTypeOther", "relationship_type_other")
          .put("relationshipSex", "relationship_sex")
          .put("relationshipAge", "relationship_age")
          .put("relationshipDiseaseICD10", "relationship_disease_icd10")
          .put("relationshipDisease", "relationship_disease")
          .build();

  private static final ImmutableMap<String, String> EXPOSURE_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("exposureType", "exposure_type")
          .put("exposureIntensity", "exposure_intensity")
          .put("tabaccoSmokingHistoryIndicator", "tabacco_smoking_history_indicator")
          .put("tabaccoSmokingIntensity", "tabacco_smoking_intensity")
          .put("alcoholHistory", "alcohol_history")
          .put("alcoholHistoryIntensity", "alcolhol_history_intensity")
          .build();

  private static final ImmutableMap<String, String> THERAPY_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("firstTherapyType", "first_therapy_type")
          .put("firstTherapyTherapeuticIntent", "first_therapy_therapeutic_intent")
          .put("firstTherapyStartInterval", "first_therapy_start_interval")
          .put("firstTherapyDuration", "first_therapy_duration")
          .put("firstTherapyResponse", "first_therapy_response")
          .put("secondTherapyType", "second_therapy_type")
          .put("secondTherapyTherapeuticIntent", "second_therapy_therapeutic_intent")
          .put("secondTherapyStartInterval", "second_therapy_start_interval")
          .put("secondTherapyDuration", "second_therapy_duration")
          .put("secondTherapyResponse", "second_therapy_response")
          .put("otherTherapy", "other_therapy")
          .put("otherTherapyResponse", "other_therapy_response")
          .build();

  private static final ImmutableMap<String, String> SPECIMEN_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "_specimen_id")
          .put("submittedId", "specimen_id")
          .put("available", "specimen_available")
          .put("digitalImageOfStainedSection", "digital_image_of_stained_section")
          .put("dbXref", "specimen_db_xref")
          .put("biobank", "specimen_biobank")
          .put("biobankId", "specimen_biobank_id")
          .put("treatmentType", "specimen_donor_treatment_type")
          .put("treatmentTypeOther", "specimen_donor_treatment_type_other")
          .put("processing", "specimen_processing")
          .put("processingOther", "specimen_processing_other")
          .put("storage", "specimen_storage")
          .put("storageOther", "specimen_storage_other")
          .put("type", "specimen_type")
          .put("typeOther", "specimen_type_other")
          .put("uri", "specimen_uri")
          .put("tumourConfirmed", "tumour_confirmed")
          .put("tumourGrade", "tumour_grade")
          .put("tumourGradeSupplemental", "tumour_grade_supplemental")
          .put("tumourHistologicalType", "tumour_histological_type")
          .put("tumourStage", "tumour_stage")
          .put("tumourStageSupplemental", "tumour_stage_supplemental")
          .put("tumourStageSystem", "tumour_stage_system")
          .put("percentCellularity", "percent_cellularity")
          .put("levelOfCellularity", "level_of_cellularity")
          .build();

  private static final ImmutableMap<String, String> SAMPLE_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "_sample_id")
          .put("analyzedId", "analyzed_sample_id")
          .put("analyzedInterval", "analyzed_sample_interval")
          .put("availableRawSequenceData", "available_raw_sequence_data")
          .put("study", "study")
          .build();

  private static final ImmutableMap<String, String> RAWSEQDATA_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "analysis_id")
          .put("platform", "platform")
          .put("state", "state")
          .put("type", "sample_type")
          .put("libraryStrategy", "library_strategy")
          .put("analyteCode", "analyte_code")
          .put("dataUri", "analysis_data_uri")
          .put("repository", "repository")
          .put("filename", "filename")
          .put("rawDataAccession", "raw_data_accession")
          .build();

  private static final ImmutableMap<String, String> GENES_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "_gene_id")
          .put("symbol", "symbol")
          .put("name", "name")
          .put("type", "biotype")
          .put("chromosome", "chromosome")
          .put("start", "start")
          .put("end", "end")
          .put("strand", "strand")
          .put("description", "description")
          .put("synonyms", "synonyms") // Is a multi field
          .put("externalDbIds", "external_db_ids")
          .put("affectedDonorCountTotal", "_summary._affected_donor_count")
          .put("affectedDonorCountFiltered", "_score")
          .put("affectedTranscriptIds", "_summary._affected_transcript_id")
          .put("location", "location")
          .put("pathwayId", "pathways.pathway_id")
          .put("pathwayName", "pathways.pathway_name")
          .put("pathways", "pathways")
          .put("sets", "sets")
          .put(API_ENTITY_LIST_ID_FIELD_NAME, API_ENTITY_LIST_ID_FIELD_NAME)

          .build();

  private static final ImmutableMap<String, String> MUTATIONS_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "_mutation_id")
          .put("mutation", "mutation")
          .put("type", "mutation_type")
          .put("chromosome", "chromosome")
          .put("start", "chromosome_start")
          .put("end", "chromosome_end")
          .put("affectedDonorCountTotal", "_summary._affected_donor_count")
          .put("testedDonorCount", "_summary._tested_donor_count")
          .put("consequenceType", "consequence_type")
          .put("consequenceTypeNested", "transcript.consequence.consequence_type")
          .put("platform", "platform")
          .put("platformNested", "ssm_occurrence.observation.platform")
          .put("verificationStatus", "verification_status")
          .put("verificationStatusNested", "ssm_occurrence.observation.verification_status")
          .put("assemblyVersion", "assembly_version")
          .put("referenceGenomeAllele", "reference_genome_allele")
          .put("affectedProjectCount", "_summary._affected_project_count")
          .put("affectedProjectIds", "_summary._affected_project_ids")
          .put("affectedDonorCountFiltered", "_score")
          .put("transcriptId", "transcript.id")
          .put("functionalImpact", "functional_impact_prediction_summary")
          .put("functionalImpactNested", "transcript.functional_impact_prediction_summary")
          .put("location", "location")
          .put("sequencingStrategy", "sequencing_strategy")
          .put("sequencingStrategyNested", "ssm_occurrence.observation.sequencing_strategy")
          .put(API_ENTITY_LIST_ID_FIELD_NAME, API_ENTITY_LIST_ID_FIELD_NAME)

          .build();

  private static final ImmutableMap<String, String> TRANSCRIPT_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "id")
          .put("name", "name")
          .put("type", "biotype")
          .put("isCanonical", "is_canonical")
          .put("start", "start")
          .put("end", "end")
          .put("cdnaCodingStart", "cdna_coding_start")
          .put("cdnaCodingEnd", "cdna_coding_end")
          .put("codingRegionStart", "coding_region_start")
          .put("codingRegionEnd", "coding_region_end")
          .put("startExon", "start_exon")
          .put("endExon", "end_exon")
          .put("length", "length")
          .put("lengthAminoAcid", "length_amino_acid")
          .put("lengthCds", "length_cds")
          .put("numberOfExons", "number_of_exons")
          .put("seqExonStart", "seq_exon_start")
          .put("seqExonEnd", "seq_exon_end")
          .put("translationId", "translation_id")
          .put("functionalImpact", "functional_impact_prediction_summary")
          .build();

  private static final ImmutableMap<String, String> CONSEQUENCE_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("geneAffectedId", "gene_affected")
          .put("geneAffectedSymbol", "gene_symbol")
          .put("transcriptAffected", "transcript_affected")
          .put("aaMutation", "aa_mutation")
          .put("geneStrand", "gene_strand")
          .put("cdsMutation", "cds_mutation")
          .put("type", "consequence_type")
          .put("functionalImpact", "functional_impact_prediction_summary")
          .build();

  private static final ImmutableMap<String, String> DOMAIN_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("interproId", "interpro_id")
          .put("hitName", "hit_name")
          .put("description", "description")
          .put("gffSource", "gff_source")
          .put("start", "start")
          .put("end", "end")
          .build();

  private static final ImmutableMap<String, String> EXON_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("start", "start")
          .put("end", "end")
          .put("cdnaCodingStart", "cdna_coding_start")
          .put("cdnaCodingEnd", "cdna_coding_end")
          .put("genomicCodingStart", "genomic_coding_start")
          .put("genomicCodingEnd", "genomic_coding_end")
          .put("cdnaStart", "cdna_start")
          .put("cdnaEnd", "cdna_end")
          .put("endPhase", "end_phase")
          .put("startPhase", "startPhase")
          .build();

  // This need to be used for ssm_occurrence in Mutations
  // FIXME: remove extra fields that are now in observations
  private static final ImmutableMap<String, String> EMB_OCCURRENCE_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("donorId", "_donor_id")
          .put("submittedMutationId", "mutation_id")
          .put("mutationId", "_mutation_id")
          .put("matchedSampleId", "_matched_sample_id")
          .put("submittedMatchedSampleId", "matched_sample_id")
          .put("projectId", "_project_id")
          .put("primarySite", "project.primary_site")
          .put("tumourType", "project.tumour_type")
          .put("tumourSubtype", "project.tumour_subtype")
          .put("sampleId", "_sample_id")
          .put("specimenId", "_specimen_id")
          .put("analysisId", "analysis_id")
          .put("analyzedSampleId", "analyzed_sample_id")
          .put("baseCallingAlgorithm", "base_calling_algorithm")
          .put("strand", "chromosome_strand")
          .put("chromosome", "chromosome")
          .put("start", "chromosome_start")
          .put("end", "chromosome_end")
          .put("controlGenotype", "control_genotype")
          .put("experimentalProtocol", "experimental_protocol")
          .put("expressedAllele", "expressed_allele")
          .put("platform", "platform")
          .put("probability", "probability")
          .put("qualityScore", "quality_score")
          .put("rawDataAccession", "raw_data_accession")
          .put("rawDataRepository", "raw_data_repository")
          .put("readCount", "read_count")
          .put("refsnpAllele", "refsnp_allele")
          .put("seqCoverage", "seq_coverage")
          .put("sequencingStrategy", "sequencing_strategy")
          .put("ssmMDbXref", "ssm_m_db_xref")
          .put("ssmMUri", "ssm_m_uri")
          .put("ssmPUri", "ssm_p_uri")
          .put("tumourGenotype", "tumour_genotype")
          .put("variationCallingAlgorithm", "variation_calling_algorithm")
          .put("verificationPlatform", "verification_platform")
          .put("verificationStatus", "verification_status")
          .put("xrefEnsemblVarId", "xref_ensembl_var_id")
          .build();

  // FIXME: remove extra fields that are now in observations
  private static final ImmutableMap<String, String> OCCURRENCE_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("donorId", "ssm._donor_id")
          .put("submittedMutationId", "ssm.mutation_id")
          .put("mutationId", "ssm._mutation_id")
          .put("matchedSampleId", "ssm._matched_sample_id")
          .put("submittedMatchedSampleId", "ssm.matched_sample_id")
          .put("projectId", "ssm._project_id")
          .put("primarySite", "ssm.project.primary_site")
          .put("tumourType", "ssm.project.tumour_type")
          .put("tumourSubtype", "ssm.project.tumour_subtype")
          .put("sampleId", "ssm._sample_id")
          .put("specimenId", "ssm._specimen_id")
          .put("analysisId", "ssm.analysis_id")
          .put("analyzedSampleId", "ssm.analyzed_sample_id")
          .put("baseCallingAlgorithm", "ssm.base_calling_algorithm")
          .put("strand", "ssm.chromosome_strand")
          .put("chromosome", "ssm.chromosome")
          .put("start", "ssm.chromosome_start")
          .put("end", "ssm.chromosome_end")
          .put("controlGenotype", "ssm.control_genotype")
          .put("experimentalProtocol", "ssm.experimental_protocol")
          .put("expressedAllele", "ssm.expressed_allele")
          .put("platform", "ssm.platform")
          .put("probability", "ssm.probability")
          .put("qualityScore", "ssm.quality_score")
          .put("rawDataAccession", "ssm.raw_data_accession")
          .put("rawDataRepository", "ssm.raw_data_repository")
          .put("readCount", "ssm.read_count")
          .put("refsnpAllele", "ssm.refsnp_allele")
          .put("seqCoverage", "ssm.seq_coverage")
          .put("sequencingStrategy", "ssm.sequencing_strategy")
          .put("ssmMDbXref", "ssm.ssm_m_db_xref")
          .put("ssmMUri", "ssm.ssm_m_uri")
          .put("ssmPUri", "ssm.ssm_p_uri")
          .put("tumourGenotype", "ssm.tumour_genotype")
          .put("variationCallingAlgorithm", "ssm.variation_calling_algorithm")
          .put("verificationPlatform", "ssm.verification_platform")
          .put("verificationStatus", "ssm.verification_status")
          .put("xrefEnsemblVarId", "ssm.xref_ensembl_var_id")
          .put("mutation", "ssm.mutation")
          .put("observation", "ssm.observation")
          .put(API_ENTITY_LIST_ID_FIELD_NAME, API_ENTITY_LIST_ID_FIELD_NAME)

          .build();

  private static final ImmutableMap<String, String> OBSERVATION_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("alignmentAlgorithm", "alignment_algorithm")
          .put("analysisId", "analysis_id")
          .put("analyzedSampleId", "analyzed_sample_id")
          .put("baseCallingAlgorithm", "base_calling_algorithm")
          .put("chromosomeStrand", "chromosome_strand")
          .put("controlGenotype", "controlGenotype")
          .put("donorId", "donor_id")
          .put("experimentalProtocol", "experimental_protocol")
          .put("expressedAllele", "expressed_allele")
          .put("isAnnotated", "is_annotated")
          .put("note", "note")
          .put("otherAnalysisAlgorithm", "other_analysis_algorithm")
          .put("platform", "platform")
          .put("probability", "probability")
          .put("projectId", "project_id")
          .put("qualityScore", "quality_score")
          .put("rawDataAccession", "raw_data_accession")
          .put("rawDataRepository", "raw_data_repository")
          .put("readCount", "read_count")
          .put("referenceGenomeAllele", "reference_genome_allele")
          .put("refsnpAllele", "refsnp_allele")
          .put("refsnpStrand", "refsnp_strand")
          .put("seqCoverage", "seq_coverage")
          .put("sequencingStrategy", "sequencing_strategy")
          .put("tumourGenotype", "tumour_genotype")
          .put("validationPlatform", "validation_platform")
          .put("validationStatus", "validation_status")
          .put("variationCallingAlgorithm", "variation_calling_algorithm")
          .put("verificationStatus", "verification_status")
          .put("xrefEnsemblVarId", "xref_ensembl_var_id")
          .put("matchedICGCSampleId", "_matched_sample_id")
          .put("icgcSampleId", "_sample_id")
          .put("icgcSpecimenId", "_specimen_id")
          .put("submittedSampleId", "analyzed_sample_id")
          .put("submittedMatchedSampleId", "matched_sample_id")
          .put(API_ENTITY_LIST_ID_FIELD_NAME, API_ENTITY_LIST_ID_FIELD_NAME)

          .build();

  private static final ImmutableMap<String, String> RELEASE_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "_release_id")
          .put("name", "name")
          .put("releasedOn", "date")
          .put("donorCount", "donor_count")
          .put("mutationCount", "mutation_count")
          .put("sampleCount", "sample_count")
          .put("projectCount", "project_count")
          .put("specimenCount", "specimen_count")
          .put("ssmCount", "ssm_count")
          .put("primarySiteCount", "primary_site_count")
          .put("mutatedGeneCount", "mutated_gene_count")
          .put("releaseNumber", "number")
          .put("completeDonorCount", "complete_donor_count")
          .put("completeProjectCount", "complete_project_count")
          .build();

  private static final ImmutableMap<String, String> KEYWORD_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          // Common
          .put("id", "id")
          .put("type", "type")

          // Gene and project and pathway
          .put("name", "name")

          // Gene
          .put("symbol", "symbol")
          .put("ensemblTranscriptId", "ensemblTranscriptId")
          .put("ensemblTranslationId", "ensemblTranslationId")
          .put("synonyms", "synonyms")
          .put("uniprotkbSwissprot", "uniprotkbSwissprot")
          .put("omimGene", "omimGene")
          .put("entrezGene", "entrezGene")
          .put("hgnc", "hgnc")

          // Mutation
          .put("mutation", "mutation")
          .put("geneMutations", "geneMutations")
          .put("start", "start")

          // Project
          .put("tumourType", "tumourType")
          .put("tumourSubtype", "tumourSubtype")
          .put("primarySite", "primarySite")

          // Donor
          .put("specimenIds", "specimenIds")
          .put("submittedSpecimenIds", "submittedSpecimenIds")
          .put("sampleIds", "sampleIds")
          .put("submittedSampleIds", "submittedSampleIds")
          .put("projectId", "projectId")

          // GO Term
          .put("altIds", "altIds")

          // File Repo
          .put("file_name", "file_name")
          .put("donor_id", "donor_id")

          // Pathway
          // .put("url", "url")
          // .put("source", "source")
          .build();

  private static final ImmutableMap<String, String> PATHWAY_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "pathway_id")
          .put("name", "pathway_name")
          .put("summation", "summation")
          .put("source", "source")
          .put("species", "species")
          .put("uniprotId", "uniprotId")
          .put("url", "url")
          .put("evidenceCode", "evidence_code")
          .put("projects", "projects")
          .put("parentPathways", "parent_pathways")
          .put("linkOut", "link_out")
          .put("geneCount", "gene_count")
          .build();

  private static final ImmutableMap<String, String> GENESET_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "id")
          .put("name", "name")
          .put("type", "type")
          .put("source", "source")
          .put("description", "description")
          .put("geneCount", "_summary._gene_count")
          .put("projects", "projects")

          // Pathway
          .put("hierarchy", "pathway.hierarchy")
          .put("diagrammed", "pathway.diagrammed")

          // Gene Ontology
          .put("ontology", "go_term.ontology")
          .put("altIds", "go_term.alt_ids")
          .put("synonyms", "go_term.synonyms")
          .put("inferredTree", "go_term.inferred_tree")

          // Curated gene set
          // ???
          .build();

  /*
   * private static final ImmutableMap<String, String> GO_SET_FIELDS_MAPPING = new ImmutableMap.Builder<String,
   * String>() .put("ontology", "ontology") .put("altIds", "alt_ids") .put("synonyms", "synonyms") .put("inferredTree",
   * "inferred_tree") .build();
   * 
   * private static final ImmutableMap<String, String> PATHWAY_SET_FIELDS_MAPPING = new ImmutableMap.Builder<String,
   * String>() .put("hierarchy", "hierarchy") .build();
   */

  public static final EnumMap<Kind, ImmutableMap<String, String>> FIELDS_MAPPING =
      new EnumMap<Kind, ImmutableMap<String, String>>(Kind.class);
  static {
    FIELDS_MAPPING.put(Kind.PROJECT, PROJECTS_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.DONOR, DONORS_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.SPECIMEN, SPECIMEN_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.FAMILY, FAMILY_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.EXPOSURE, EXPOSURE_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.THERAPY, THERAPY_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.SAMPLE, SAMPLE_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.SEQ_DATA, RAWSEQDATA_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.GENE, GENES_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.MUTATION, MUTATIONS_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.TRANSCRIPT, TRANSCRIPT_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.CONSEQUENCE, CONSEQUENCE_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.DOMAIN, DOMAIN_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.EXON, EXON_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.EMB_OCCURRENCE, EMB_OCCURRENCE_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.OBSERVATION, OBSERVATION_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.OCCURRENCE, OCCURRENCE_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.RELEASE, RELEASE_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.KEYWORD, KEYWORD_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.PATHWAY, PATHWAY_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.GENE_SET, GENESET_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.REPOSITORY_FILE, REPOSITORY_FILE_FIELDS_MAPPING);
  }

  /**
   * Internal gene set types
   */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  public static enum GeneSetType {
    GENE_SET_TYPE_ALL("geneSetId"),
    GENE_SET_TYPE_GO("go_term"),
    GENE_SET_TYPE_PATHWAY("pathway"),
    GENE_SET_TYPE_CURATED("curated_set");

    private final String type;
  }

  /**
   * Mapping query ID fields to internal types
   */
  public static final Map<String, String> GENE_SET_QUERY_ID_FIELDS = ImmutableMap.<String, String> builder()
      .put(GeneSetType.GENE_SET_TYPE_ALL.getType(), "geneSetId")
      .put(GeneSetType.GENE_SET_TYPE_CURATED.getType(), "curatedSetId")
      .put(GeneSetType.GENE_SET_TYPE_PATHWAY.getType(), "pathwayId")
      .put(GeneSetType.GENE_SET_TYPE_GO.getType(), "goTermId").build();

  /**
   * Mapping query type fields to internal type
   */
  public static final Map<String, String> GENE_SET_QUERY_TYPE_FIELDS = ImmutableMap.<String, String> of(
      GeneSetType.GENE_SET_TYPE_CURATED.getType(), "hasCuratedSet",
      GeneSetType.GENE_SET_TYPE_PATHWAY.getType(), "hasPathway",
      GeneSetType.GENE_SET_TYPE_GO.getType(), "hasGoTerm");

  /**
   * Mapping of gene set types to actual ES fields
   */
  public static final Map<String, List<String>> GENE_SET_INDEX_FIELDS = ImmutableMap
      .<String, List<String>> builder()
      .put(GeneSetType.GENE_SET_TYPE_CURATED.getType(),
          ImmutableList.<String> of(GeneSetType.GENE_SET_TYPE_CURATED.getType()))
      .put(GeneSetType.GENE_SET_TYPE_PATHWAY.getType(),
          ImmutableList.<String> of(GeneSetType.GENE_SET_TYPE_PATHWAY.getType()))
      .put(GeneSetType.GENE_SET_TYPE_GO.getType(),
          ImmutableList.<String> of(
              String.format("%s.%s", GeneSetType.GENE_SET_TYPE_GO.getType(), "molecular_function"),
              String.format("%s.%s", GeneSetType.GENE_SET_TYPE_GO.getType(), "biological_process"),
              String.format("%s.%s", GeneSetType.GENE_SET_TYPE_GO.getType(), "cellular_component")))
      .put(GeneSetType.GENE_SET_TYPE_ALL.getType(),
          ImmutableList.<String> of(
              GeneSetType.GENE_SET_TYPE_CURATED.getType(),
              GeneSetType.GENE_SET_TYPE_PATHWAY.getType(),
              String.format("%s.%s", GeneSetType.GENE_SET_TYPE_GO.getType(), "molecular_function"),
              String.format("%s.%s", GeneSetType.GENE_SET_TYPE_GO.getType(), "biological_process"),
              String.format("%s.%s", GeneSetType.GENE_SET_TYPE_GO.getType(), "cellular_component")))
      .build();

  private String index;

  @Autowired
  public IndexModel(@Value("#{indexName}") String index) {
    super();
    this.index = index;
  }

  public String getIndex() {
    return this.index;
  }
}
