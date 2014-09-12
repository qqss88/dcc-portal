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
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Pathway")
public class Pathway {

  @ApiModelProperty(value = "Pathway ID", required = true)
  String id;
  @ApiModelProperty(value = "Pathway Name", required = true)
  String name;
  @ApiModelProperty(value = "Uniport ID", required = true)
  String uniprotId;
  @ApiModelProperty(value = "Pathway Source", required = true)
  String source;
  @ApiModelProperty(value = "Species", required = true)
  String species;
  @ApiModelProperty(value = "URL", required = true)
  String url;
  @ApiModelProperty(value = "Evidence Code", required = true)
  String evidenceCode;
  @ApiModelProperty(value = "Summation Description", required = true)
  String summation;
  @ApiModelProperty(value = "Projects that have a Donor affected by the Gene")
  List<Project> projects;
  @ApiModelProperty(value = "List of pathway hierarchy containing this pathway")
  List<List<Map<String, String>>> parentPathways;
  @ApiModelProperty(value = "List of pathway hierarchy urls")
  List<String> linkOut;
  @ApiModelProperty(value = "Count of genes affected")
  Long geneCount;

  @SuppressWarnings("unchecked")
  @JsonCreator
  public Pathway(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(Kind.PATHWAY);
    id = (String) fieldMap.get(fields.get("id"));
    name = (String) fieldMap.get(fields.get("name"));
    source = (String) fieldMap.get(fields.get("source"));
    uniprotId = (String) fieldMap.get(fields.get("uniprotId"));
    species = (String) fieldMap.get(fields.get("species"));
    url = (String) fieldMap.get(fields.get("url"));
    evidenceCode = (String) fieldMap.get(fields.get("evidenceCode"));
    summation = (String) fieldMap.get(fields.get("summation"));
    projects = buildProjects(fieldMap);
    parentPathways = (List<List<Map<String, String>>>) fieldMap.get(fields.get("parentPathways"));
    linkOut = (List<String>) fieldMap.get(fields.get("linkOut"));

    geneCount = getLong(fieldMap.get(fields.get("geneCount")));
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

  private Long getLong(Object field) {
    if (field instanceof Long) return (Long) field;
    if (field instanceof Integer) return (long) (Integer) field;
    else
      return null;
  }
}
