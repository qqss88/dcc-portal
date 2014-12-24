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
package org.icgc.dcc.portal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.portal.util.ElasticsearchUtils.flattenFieldsMap;

import java.util.List;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.common.collect.ImmutableList;
import org.junit.Test;

import com.google.common.collect.Maps;

@Slf4j
public class ElasticsearchUtils {

  // @formatter:off
  /* 
   *  {
   *    "L1-stringField" : "L1-stringValue",
   *    "L1-arrayField" : ["L1-ArrayValue-1", "L2-ArrayValue-1"],
   *    "L1-objectField" : {
   *      "L2-stringField" : "L2-stringValue",
   *      "L2-ArrayField" : ["L2-ArrayValue-1", "L2-ArrayValue-2"],
   *      "L2-objectField" : {
   *        "L3-stringField" : "L3-stringValue",
   *        "L3-ArrayField" : ["L3-ArrayValue-1", "L3-ArrayValue-2"]
   *      }
   *    }
   *  }
   */
  // @formatter:off
  @SuppressWarnings("unchecked")
  @Test
  public void flattenFieldsMapTest() {
    val nestedMapLevel3 = Maps.<String, Object> newHashMap();
    nestedMapLevel3.put("L3-stringField", "L3-stringValue");
    nestedMapLevel3.put("L3-ArrayField", ImmutableList.of("L3-ArrayValue-1", "L3-ArrayValue-2"));
    
    val nestedMapLevel2 = Maps.<String, Object> newHashMap();
    nestedMapLevel2.put("L2-stringField", "L2-stringValue");
    nestedMapLevel2.put("L2-ArrayField", ImmutableList.of("L2-ArrayValue-1", "L2-ArrayValue-2"));
    nestedMapLevel2.put("L2-objectField", nestedMapLevel3);
    
    val testMap = Maps.<String, Object> newHashMap();
    testMap.put("L1-stringField", "L1-stringValue");
    testMap.put("L1-objectField", nestedMapLevel2);
    testMap.put("L1-ArrayField", ImmutableList.of("L1-ArrayValue-1", "L1-ArrayValue-2"));
    log.debug("Test map: {}", testMap);
    
    val resultMap = flattenFieldsMap(testMap);
    log.debug("Result map: {}", resultMap);
    assertThat(resultMap.get("L1-stringField")).isEqualTo("L1-stringValue");
    
    // Testing 2 level nested objects
    assertThat(resultMap.get("L1-objectField.L2-stringField")).isEqualTo("L2-stringValue");
    assertThat(resultMap.get("L1-objectField.L2-ArrayField")).isInstanceOf(List.class);
    assertThat((List<Object>) resultMap.get("L1-objectField.L2-ArrayField")).containsExactly("L2-ArrayValue-1", "L2-ArrayValue-2");
    
    // Testing 3 level nested objects
    assertThat(resultMap.get("L1-objectField.L2-objectField.L3-stringField")).isEqualTo("L3-stringValue");
    assertThat(resultMap.get("L1-objectField.L2-objectField.L3-ArrayField")).isInstanceOf(List.class);
    assertThat((List<Object>) resultMap.get("L1-objectField.L2-objectField.L3-ArrayField")).containsExactly("L3-ArrayValue-1", "L3-ArrayValue-2");
  }

}
