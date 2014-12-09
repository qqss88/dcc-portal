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
package org.icgc.dcc.portal.enrichment;

import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.portal.model.Query.idField;
import static org.icgc.dcc.portal.util.Filters.geneSetFilter;
import static org.icgc.dcc.portal.util.Filters.inputGeneListFilter;

import java.util.UUID;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.portal.model.AndQuery;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.Universe;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Enrichment analysis {@link Query} utilities.
 */
@NoArgsConstructor(access = PRIVATE)
public class EnrichmentQueries {

  public static AndQuery overlapQuery(@NonNull Query query, @NonNull Universe universe, @NonNull UUID inputGeneListId) {
    // Components
    val queryFilter = query.getFilters();
    val universeFilter = universe.getFilter();
    val analysisFilter = inputGeneListFilter(inputGeneListId);

    // Overlap
    val filters = ImmutableList.of(queryFilter, universeFilter, analysisFilter);

    // EnrichmentSearchResponses?
    val includes = Lists.<String> newArrayList();

    val overlapQuery = new AndQuery(filters);
    overlapQuery.setFields(idField()).setIncludes(includes);

    return overlapQuery;
  }

  public static AndQuery geneSetOverlapQuery(@NonNull Query query, @NonNull Universe universe,
      @NonNull UUID inputGeneListId, @NonNull String geneSetId) {
    val overlapQuery = overlapQuery(query, universe, inputGeneListId);

    // TODO: Do not mutate, create a Query copy contructor instead
    val filters =
        ImmutableList.<ObjectNode> builder().addAll(overlapQuery.getAndFilters()).add(geneSetFilter(geneSetId)).build();

    overlapQuery.setAndFilters(filters);

    return overlapQuery;
  }

}