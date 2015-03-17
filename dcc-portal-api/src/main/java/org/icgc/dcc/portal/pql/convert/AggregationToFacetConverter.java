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
package org.icgc.dcc.portal.pql.convert;

import static java.lang.String.format;

import java.util.Map;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.global.Global;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.icgc.dcc.portal.model.TermFacet;
import org.icgc.dcc.portal.model.TermFacet.Term;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@Slf4j
public class AggregationToFacetConverter {

  public Map<String, TermFacet> convert(Aggregations aggregations) {
    val result = new ImmutableMap.Builder<String, TermFacet>();
    for (val aggregation : aggregations) {
      val termFacet = createTermFacet(findTermsAggregation(aggregation));
      log.debug("Created TermFacet - {}", termFacet);
      result.put(aggregation.getName(), termFacet);
    }

    return result.build();
  }

  private static TermFacet createTermFacet(Terms termsAgg) {
    val terms = new ImmutableList.Builder<Term>();
    long total = 0;
    for (val bucket : termsAgg.getBuckets()) {
      val docsCount = bucket.getDocCount();
      terms.add(new Term(bucket.getKey(), docsCount));
      total += docsCount;
    }

    return TermFacet.of(total, terms.build());
  }

  private static Terms findTermsAggregation(Aggregation aggregation) {
    if (aggregation instanceof Terms) {
      return (Terms) aggregation;
    }

    if (aggregation instanceof Global) {
      Global agg = (Global) aggregation;

      return findTermsAggregation(getSubaggregation(agg.getAggregations()));
    } else if (aggregation instanceof Filter) {
      Filter agg = (Filter) aggregation;

      return findTermsAggregation(getSubaggregation(agg.getAggregations()));
    } else if (aggregation instanceof Nested) {
      Nested agg = (Nested) aggregation;

      return findTermsAggregation(getSubaggregation(agg.getAggregations()));
    }

    throw new IllegalArgumentException(format("Failed to find TermsAggretaion in %s", aggregation));
  }

  private static Aggregation getSubaggregation(Aggregations aggregations) {
    return aggregations.asList().get(0);
  }

}
