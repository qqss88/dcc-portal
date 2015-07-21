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

import lombok.Value;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Pagination {

  @ApiModelProperty(value = "Actual Number of results returned", required = true)
  Integer count;
  @ApiModelProperty(value = "Total number of results matching query", required = true)
  Long total;
  @ApiModelProperty(value = "Number of results returned", required = true)
  Integer size;
  @ApiModelProperty(value = "Start index of results", required = true)
  Integer from;
  @ApiModelProperty(value = "Current page", required = true)
  Integer page;
  @ApiModelProperty(value = "Total number of pages", required = true)
  Long pages;
  @ApiModelProperty(value = "Column that the results are sorted on", required = true)
  String sort;
  @ApiModelProperty(value = "Order of the sorted column", required = true)
  String order;

  public static Pagination of(Integer count, Long total, Query query) {
    return new Pagination(count, total, query);
  }

  private Pagination(Integer count, Long total, Query query) {
    this.count = count;
    this.total = total;
    this.size = query.getSize();
    this.from = query.getFrom() + 1;
    this.sort = query.getSort();
    this.order = query.getOrder().toString().toLowerCase();
    this.page = size <= 1 ? from : (from / size) + 1;
    this.pages = size <= 1 ? total : (total + size - 1) / size;
  }
}
