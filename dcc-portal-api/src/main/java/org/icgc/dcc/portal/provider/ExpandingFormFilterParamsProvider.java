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

import static com.sun.jersey.server.impl.model.method.dispatch.FormDispatchProvider.FORM_PROPERTY;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import lombok.val;

import org.icgc.dcc.portal.model.FiltersParam;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.api.representation.Form;
import com.sun.jersey.core.header.MediaTypes;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

/**
 * {@code InjectableProvider} that services {@link FormParam} annotated {@link FiltersParam}s in resource methods.
 */
@Provider
@Component
public class ExpandingFormFilterParamsProvider extends AbstractExpandingFilterParamsProvider implements
    InjectableProvider<FormParam, Parameter> {

  @Override
  public Injectable<FiltersParam> getInjectable(ComponentContext context, FormParam meta, Parameter param) {
    return getInjectable(param);
  }

  @Override
  protected MultivaluedMap<String, String> resolveParameters(HttpContext context) {
    Form form = (Form) context.getProperties().get(FORM_PROPERTY);
    if (form == null) {
      form = getForm(context);
      context.getProperties().put(FORM_PROPERTY, form);
    }

    return form;
  }

  private static Form getForm(HttpContext context) {
    val request = context.getRequest();
    if (request.getMethod().equals("GET")) {
      throw new IllegalStateException("Form with HTTP method GET");
    }

    if (!MediaTypes.typeEquals(APPLICATION_FORM_URLENCODED_TYPE, request.getMediaType())) {
      throw new IllegalStateException("Form with HTTP content-type other than x-www-form-urlencoded");
    }

    return request.getFormParameters();
  }

}
