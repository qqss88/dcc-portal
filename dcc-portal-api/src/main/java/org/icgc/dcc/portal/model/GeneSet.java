/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "GeneSet")
public class GeneSet {

  // Common fields
  @ApiModelProperty(value = "ID", required = true)
  String id;

  @ApiModelProperty(value = "Name", required = true)
  String name;

  @ApiModelProperty(value = "Source", required = true)
  String source;

  @ApiModelProperty(value = "Type", required = true)
  String type;

  @ApiModelProperty(value = "Description", required = true)
  String description;

  @ApiModelProperty(value = "Count of genes affected")
  Long geneCount;

  @ApiModelProperty(value = "Projects affected")
  List<Project> projects;

  // Reactome pathway - FIXME: model project-donor for pathway
  List<List<Map<String, String>>> hierarchy;

  // Gene Ontology
  String ontology;
  List<String> altIds;
  List<String> synonyms;
  List<Map<String, String>> inferredTree;

  // @SuppressWarnings("unchecked")
  @SuppressWarnings("unchecked")
  @JsonCreator
  public GeneSet(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(Kind.GENE_SET);
    id = (String) fieldMap.get(fields.get("id"));
    name = (String) fieldMap.get(fields.get("name"));
    source = (String) fieldMap.get(fields.get("source"));
    type = (String) fieldMap.get(fields.get("type"));
    description = (String) fieldMap.get(fields.get("description"));
    geneCount = getLong(fieldMap.get(fields.get("geneCount")));

    projects = buildProjects(fieldMap);

    // Reactome pathway specific fields
    // hierarchy = (List<List<Map<String, String>>>) fieldMap.get(fields.get("hierarchy"));
    hierarchy = buildPathwayHierarchy((List<List<Map<String, Object>>>) fieldMap.get(fields.get("hierarchy")));

    // Gene ontology specific fields
    ontology = (String) fieldMap.get(fields.get("ontology"));
    altIds = (List<String>) fieldMap.get(fields.get("altIds"));
    synonyms = (List<String>) fieldMap.get(fields.get("synonyms"));
    inferredTree = buildInferredTree((List<Map<String, Object>>) fieldMap.get(fields.get("inferredTree")));
  }

  @SuppressWarnings("unused")
  private List<List<Map<String, String>>> buildPathwayHierarchy(List<List<Map<String, Object>>> field) {
    val result = Lists.<List<Map<String, String>>> newArrayList();

    if (field == null) {
      return result;
    }

    for (val parentPath : field) {
      val path = Lists.<Map<String, String>> newArrayList();
      for (val pathwayNode : parentPath) {
        val node = Maps.<String, String> newHashMap();
        node.put("id", (String) pathwayNode.get("id"));
        node.put("name", (String) pathwayNode.get("name"));
        node.put("diagrammed", String.valueOf(pathwayNode.get("diagrammed")));
        path.add(node);
      }
      result.add(path);
    }
    return result;
  }

  @SuppressWarnings("unused")
  private List<Map<String, String>> buildInferredTree(List<Map<String, Object>> field) {
    val result = Lists.<Map<String, String>> newArrayList();

    if (field == null) {
      return result;
    }

    for (val obj : field) {
      val treeNode = Maps.<String, String> newHashMap();
      treeNode.put("id", (String) obj.get("id"));
      treeNode.put("name", (String) obj.get("name"));
      treeNode.put("relation", (String) obj.get("relation"));
      treeNode.put("level", String.valueOf(obj.get("level")));
      result.add(treeNode);
    }
    return result;
  }

  private Long getLong(Object field) {
    if (field instanceof Long) return (Long) field;
    if (field instanceof Integer) return (long) (Integer) field;
    else
      return null;
  }

  @SuppressWarnings("unchecked")
  private List<Project> buildProjects(Map<String, Object> fieldMap) {
    val ps = (List<Map<String, Object>>) fieldMap.get("projects");
    val projects = Lists.<Project> newArrayList();

    if (ps != null) {
      for (val p : ps) {
        p.put("_summary._total_donor_count", ((Map<String, Object>) p.get("_summary")).get("_total_donor_count"));
        p.put("_summary._affected_donor_count",
            ((Map<String, Object>) p.get("_summary")).get("_affected_donor_count"));
        p.put("_summary._available_data_type", ((Map<String, Object>) p.get("_summary")).get("_available_data_type"));
        p.put("_summary._ssm_tested_donor_count",
            ((Map<String, Object>) p.get("_summary")).get("_ssm_tested_donor_count"));
        projects.add(new Project(p));
      }
      return projects;
    } else {
      return null;
    }
  }
}
