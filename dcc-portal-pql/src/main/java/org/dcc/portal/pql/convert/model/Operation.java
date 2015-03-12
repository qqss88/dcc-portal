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
package org.dcc.portal.pql.convert.model;

import static java.lang.String.format;

import java.util.Collection;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.common.core.model.Identifiable;

import com.google.common.collect.ImmutableList;

public enum Operation implements Identifiable {

  IS,
  NOT;

  private static final Collection<String> OPERATION_IDS = initOperationIds();

  @Override
  public String getId() {
    return name().toLowerCase();
  }

  public static Operation byId(@NonNull String id) {
    for (val name : values()) {
      if (id.equals(name.getId())) {
        return name;
      }
    }

    throw new IllegalArgumentException(format("Could not find operation %s", id));
  }

  public static Collection<String> operations() {
    return OPERATION_IDS;
  }

  private static Collection<String> initOperationIds() {
    val result = new ImmutableList.Builder<String>();
    for (val name : values()) {
      result.add(name.getId());
    }

    return result.build();
  }

}