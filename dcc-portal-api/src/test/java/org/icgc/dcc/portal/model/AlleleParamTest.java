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
package org.icgc.dcc.portal.model;

import javax.ws.rs.WebApplicationException;

import org.junit.Test;

public class AlleleParamTest {

  @SuppressWarnings("unused")
  public void testValidAllele() {
    AlleleMutation mutation = new AlleleParam("AAA").get();
    mutation = new AlleleParam("T>TGGG").get();
    mutation = new AlleleParam("CCA>C").get();
    mutation = new AlleleParam("->ATT").get();
    mutation = new AlleleParam("  GG>-").get();
  }

  @SuppressWarnings("unused")
  @Test(expected = WebApplicationException.class)
  public void testInvalidLowercase() {
    AlleleMutation mutation = new AlleleParam("acccc").get();
    mutation = new AlleleParam("A>aaA").get();
  }

  @SuppressWarnings("unused")
  @Test(expected = WebApplicationException.class)
  public void testInvalidReferenceInIndels() {
    // Must be one reference for insertion
    AlleleMutation mutation = new AlleleParam("AA>AGGG").get();

    // Must be one "left" for deletion
    mutation = new AlleleParam("GAGC>GC").get();
  }

  @SuppressWarnings("unused")
  @Test(expected = WebApplicationException.class)
  public void testInvalidNoRerfernceFormat() {
    // no spaces inside
    AlleleMutation mutation = new AlleleParam("- >AGGG").get();

    // no random other character to indicate mutation
    mutation = new AlleleParam("A<AAA").get();

    // only one mutation
    mutation = new AlleleParam("A>C>T").get();
  }
}
