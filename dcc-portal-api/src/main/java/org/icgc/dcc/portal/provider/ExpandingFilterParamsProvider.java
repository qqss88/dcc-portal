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
package org.icgc.dcc.portal.provider;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Strings.nullToEmpty;
import static com.sun.jersey.core.spi.component.ComponentScope.PerRequest;
import static org.elasticsearch.common.collect.Iterables.addAll;
import static org.elasticsearch.common.collect.Iterables.isEmpty;

import java.util.Set;

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import lombok.val;

import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.service.BadRequestException;
import org.icgc.dcc.portal.service.GeneListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

/**
 * {@code InjectableProvider} that services {@link @QueryParam} annotated {@link FiltersParam}s in resource methods.
 */
@Provider
@Component
public class ExpandingFilterParamsProvider implements InjectableProvider<QueryParam, Parameter> {

  /**
   * Field constants.
   */
  private static final String DEFAULT_FILTERS = "{}";
  private static final String FILTERS_QUERY_PARAM_NAME = "filters";
  private static final String GENE_FILTER_FIELD_NAME = "gene";
  private static final String GENE_LIST_ID_FIELD_NAME = "uploadedGeneList";
  private static final String GENE_ID_FILTER_FIELD_NAME = "id";
  private static final String IS_FIELD_NAME = "is";

  /**
   * Parser constants.
   */
  private static final Splitter GENE_ID_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  /**
   * Dependencies.
   */
  @Context
  private HttpContext context;
  @Autowired
  private GeneListService geneListService;

  @Override
  public ComponentScope getScope() {
    return PerRequest;
  }

  @Override
  public Injectable<FiltersParam> getInjectable(ComponentContext context, QueryParam meta, Parameter param) {
    val expandable = FiltersParam.class == param.getParameterClass();
    if (!expandable) {
      return null;
    }

    return new Injectable<FiltersParam>() {

      @Override
      public FiltersParam getValue() {
        val expandedFilterParams = expandFilterParams();

        return expandedFilterParams;
      }

    };
  }

  private FiltersParam expandFilterParams() {
    val filterParams = parseFilterParams(context);
    val filters = filterParams.get();

    if (!hasGeneListIds(filters)) {
      // No expansion needed
      return filterParams;
    }

    expandGeneIds(filters);

    return filterParams;
  }

  private void expandGeneIds(ObjectNode filters) {
    val geneListGeneIds = resolveGeneListGeneIds(filters);
    val geneFilter = getGeneFilter(filters);

    for (val geneListGeneId : geneListGeneIds) {
      geneFilter.with(GENE_ID_FILTER_FIELD_NAME).withArray(IS_FIELD_NAME).add(geneListGeneId);
    }

    geneFilter.remove(GENE_LIST_ID_FIELD_NAME);
  }

  private Iterable<String> resolveGeneListGeneIds(ObjectNode filters) {
    val geneListIds = getGeneListIds(filters);

    val geneListGeneIds = Sets.<String> newLinkedHashSet();
    for (val geneListId : geneListIds) {
      // TODO: Make geneListService return a List<String>:
      val geneIds = parseGeneIds(geneListService.get(geneListId));
      if (isEmpty(geneIds)) {
        throw new BadRequestException("Cannot find gene list id " + geneListId);
      }

      addAll(geneListGeneIds, geneIds);
    }

    return geneListGeneIds;
  }

  private Iterable<String> parseGeneIds(String text) {
    return ImmutableList.copyOf(GENE_ID_SPLITTER.split(nullToEmpty(text)));
  }

  private static FiltersParam parseFilterParams(HttpContext context) {
    val params = context.getUriInfo().getQueryParameters();
    val value = getParam(params, FILTERS_QUERY_PARAM_NAME, DEFAULT_FILTERS);

    return new FiltersParam(value);
  }

  private static boolean hasGeneListIds(ObjectNode filters) {
    return !getGeneFilter(filters).path(GENE_LIST_ID_FIELD_NAME).path(IS_FIELD_NAME).isMissingNode();
  }

  private static Set<Long> getGeneListIds(ObjectNode filters) {
    val geneListIds = Sets.<Long> newLinkedHashSet();
    for (val geneIdNode : getGeneFilter(filters).path(GENE_LIST_ID_FIELD_NAME).withArray(IS_FIELD_NAME)) {
      val geneListId = geneIdNode.longValue();

      geneListIds.add(geneListId);
    }

    return geneListIds;
  }

  private static ObjectNode getGeneFilter(ObjectNode filters) {
    return filters.with(GENE_FILTER_FIELD_NAME);
  }

  private static String getParam(MultivaluedMap<String, String> params, String name, String defaultValue) {
    return firstNonNull(params.getFirst(name), defaultValue);
  }

}
