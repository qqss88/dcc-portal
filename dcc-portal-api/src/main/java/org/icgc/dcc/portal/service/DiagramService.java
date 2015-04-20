/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.service;

import static org.icgc.dcc.common.core.model.FieldNames.GENE_UNIPROT_IDS;
import static org.icgc.dcc.common.core.model.FieldNames.MUTATION_TRANSCRIPTS;
import static org.icgc.dcc.common.core.model.FieldNames.MUTATION_TRANSCRIPTS_GENE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.icgc.dcc.portal.model.DiagramProtein;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.repository.DiagramRepository;
import org.icgc.dcc.portal.repository.MutationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class DiagramService {

  private final DiagramRepository diagramRepo;
  private final MutationRepository mutationRepository;

  private ImmutableMap<String, String> INDEX_MODEL = IndexModel.FIELDS_MAPPING.get(Kind.DIAGRAM);

  public Map<String, DiagramProtein> mapProteinIds(@NonNull List<String> proteinUniprotIds, @NonNull String pathwayId) {
    val queries = new ArrayList<QueryBuilder>();
    for (val uniprotId : proteinUniprotIds) {
      queries.add(getQuery(uniprotId));
    }
    val response = mutationRepository.countSearches(queries);

    val uniprotToDbMap = ArrayListMultimap.create();
    val dbToUniprotMap = getProteinIdMap(pathwayId);

    dbToUniprotMap.forEach((dbId, uniprotsString) -> {
      String[] uniprots = uniprotsString.split(",");
      for (String uniprotId : uniprots) {
        uniprotToDbMap.put(parseUniprot(uniprotId), dbId);
      }
    });

    val map = Maps.<String, DiagramProtein> newHashMap();

    for (int i = 0; i < proteinUniprotIds.size(); i++) {
      val id = proteinUniprotIds.get(i);
      val value = response.getResponses()[i].getResponse().getHits().getTotalHits();

      map.put(id, getDiagramProtein(uniprotToDbMap.get(id), value));
    }

    return map;
  }

  private DiagramProtein getDiagramProtein(List<Object> map, Long value) {
    val protein = new DiagramProtein();
    protein.setDbIds(Joiner.on(",").join(map));
    protein.setValue(value);

    return protein;
  }

  private String parseUniprot(String uniprot) {
    return uniprot.substring(uniprot.indexOf(":") + 1);
  }

  private BoolQueryBuilder getQuery(String id) {
    return QueryBuilders.boolQuery().must(
        QueryBuilders.termQuery(MUTATION_TRANSCRIPTS + "." + MUTATION_TRANSCRIPTS_GENE + "."
            + GENE_UNIPROT_IDS, id));
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> getProteinIdMap(@NonNull String pathwayId) {
    return (Map<String, String>) getPathway(pathwayId).get(INDEX_MODEL.get("proteinMap"));
  }

  public String getPathwayDiagramString(@NonNull String pathwayId) {
    return unescape(getPathway(pathwayId).get(INDEX_MODEL.get("xml")).toString());
  }

  public String[] getShownPathwaySection(@NonNull String pathwayId) {
    return getPathway(pathwayId).get(INDEX_MODEL.get("highlights")).toString().split(",");
  }

  private Map<String, Object> getPathway(String id) {
    val query = Query.builder().build();
    return diagramRepo.findOne(id, query);
  }

  /**
   * Opposite of dcc.etl.db.importer.diagram.reader.DiagramXmlReader's escape
   */
  private String unescape(String xml) {
    for (String[] replacement : replacements) {
      xml.replace(replacement[1], replacement[0]);
    }
    return xml;
  }

  private String[][] replacements =
  {
      { "\b", "\\b" },
      { "\n", "\\n" },
      { "\t", "\\t" },
      { "\f", "\\f" },
      { "\r", "\\r" },
      { "\"", "\\\"" },
      { "\\", "\\\\" },
      { "/", "\\/" }
  };

}
