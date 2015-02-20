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
package org.icgc.dcc.portal.filter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.icgc.dcc.portal.config.PortalProperties.DownloadProperties;
import org.icgc.dcc.portal.resource.DownloadResource;
import org.icgc.dcc.portal.service.NotAvailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/**
 * Filter for globally disabling access to {@link DownloadResource} resources if {@link DownloadProperties#isEnabled()}
 * is {@code false}.
 */
@Component
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class DownloadFilter implements ContainerRequestFilter {

  @NonNull
  private final DownloadProperties download;

  @Override
  public ContainerRequest filter(ContainerRequest request) {
    if (isDownloadDisabled() && isDownloadURL(request)) {
      throw new NotAvailableException("Download service unavailable. Please try again later");
    }

    return request;
  }

  private boolean isDownloadDisabled() {
    return !download.isEnabled();
  }

  private boolean isDownloadURL(ContainerRequest request) {
    return request.getPath().contains("/v1/download");
  }

}