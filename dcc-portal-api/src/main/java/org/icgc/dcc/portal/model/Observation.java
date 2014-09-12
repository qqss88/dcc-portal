/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.model;

import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;

import java.util.Map;

import lombok.Value;
import lombok.val;

import org.icgc.dcc.portal.model.IndexModel.Kind;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordnik.swagger.annotations.ApiModel;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Observation")
public class Observation {

  String alignmentAlgorithm;
  String analyzedSampleId;
  String baseCallingAlgorithm;
  String chromosomeStrand;
  String controlGenotype;
  String donorId;
  String experimentalProtocol;
  String expressedAllele;
  String isAnnotated;
  String note;
  String otherAnalysisAlgorithm;
  String platform;
  Double probability;
  String projectId;
  Double qualityScore;
  String rawDataAccession;
  String rawDataRepository;
  Double readCount;
  String referenceGenomeAllele;
  String refsnpAllele;
  String refsnpStrand;
  Double seqCoverage;
  String sequencingStrategy;
  String tumourGenotype;
  String validationPlatform;
  String validationStatus;
  String variationCallingAlgorithm;
  String verificationStatus;
  String xrefEnsemblVarId;

  String matchedICGCSampleId;
  String icgcSampleId;
  String icgcSpecimenId;
  String submittedSampleId;
  String submittedMatchedSampleId;

  @JsonCreator
  public Observation(Map<String, Object> fieldMap) {

    val fields = FIELDS_MAPPING.get(Kind.OBSERVATION);

    matchedICGCSampleId = (String) fieldMap.get(fields.get("matchedICGCSampleId"));
    icgcSampleId = (String) fieldMap.get(fields.get("icgcSampleId"));
    icgcSpecimenId = (String) fieldMap.get(fields.get("icgcSpecimenId"));
    submittedSampleId = (String) fieldMap.get(fields.get("submittedSampleId"));
    submittedMatchedSampleId = (String) fieldMap.get(fields.get("submittedMatchedSampleId"));

    alignmentAlgorithm = (String) fieldMap.get(fields.get("alignmentAlgorithm"));
    analyzedSampleId = (String) fieldMap.get(fields.get("analyzedSampleId"));
    baseCallingAlgorithm = (String) fieldMap.get(fields.get("baseCallingAlgorithm"));
    chromosomeStrand = (String) fieldMap.get(fields.get("chromosomeStrand"));
    controlGenotype = (String) fieldMap.get(fields.get("controlGenotype"));
    donorId = (String) fieldMap.get(fields.get("donorId"));
    experimentalProtocol = (String) fieldMap.get(fields.get("experimentalProtocol"));
    expressedAllele = (String) fieldMap.get(fields.get("expressedAllele"));
    isAnnotated = (String) fieldMap.get(fields.get("isAnnotated"));

    note = (String) fieldMap.get(fields.get("note"));
    otherAnalysisAlgorithm = (String) fieldMap.get(fields.get("otherAnalysisAlgorithm"));
    platform = (String) fieldMap.get(fields.get("platform"));
    probability = (Double) fieldMap.get(fields.get("probability"));
    projectId = (String) fieldMap.get(fields.get("projectId"));
    qualityScore = (Double) fieldMap.get(fields.get("qualityScore"));
    rawDataAccession = (String) fieldMap.get(fields.get("rawDataAccession"));
    rawDataRepository = (String) fieldMap.get(fields.get("rawDataRepository"));
    readCount = (Double) fieldMap.get(fields.get("readCount"));
    referenceGenomeAllele = (String) fieldMap.get(fields.get("referenceGenomeAllele"));
    refsnpAllele = (String) fieldMap.get(fields.get("refsnpAllele"));
    refsnpStrand = (String) fieldMap.get(fields.get("refsnpStrand"));
    seqCoverage = (Double) fieldMap.get(fields.get("seqCoverage"));
    sequencingStrategy = (String) fieldMap.get(fields.get("sequencingStrategy"));
    tumourGenotype = (String) fieldMap.get(fields.get("tumourGenotype"));
    validationPlatform = (String) fieldMap.get(fields.get("validationPlatform"));
    validationStatus = (String) fieldMap.get(fields.get("validationStatus"));
    variationCallingAlgorithm = (String) fieldMap.get(fields.get("variation_callingAlgorithm"));
    verificationStatus = (String) fieldMap.get(fields.get("verificationStatus"));
    xrefEnsemblVarId = (String) fieldMap.get(fields.get("xrefEnsemblVarId"));
  }
}
