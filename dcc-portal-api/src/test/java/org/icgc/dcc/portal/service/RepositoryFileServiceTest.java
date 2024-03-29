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
package org.icgc.dcc.portal.service;

import static com.google.common.io.Files.getFileExtension;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.util.Joiners.DOT;
import static org.icgc.dcc.common.core.util.Splitters.WHITESPACE;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.XMLInputFactory;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.icgc.dcc.portal.config.PortalProperties;
import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.repository.BaseElasticSearchTest;
import org.icgc.dcc.portal.repository.RepositoryFileRepository;
import org.icgc.dcc.portal.repository.TermsLookupRepository;
import org.icgc.dcc.portal.test.TestIndex;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.supercsv.io.CsvListReader;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

/**
 * Test suite for RepositoryFileService
 */
public class RepositoryFileServiceTest extends BaseElasticSearchTest {

  private static final SMInputFactory XML_READER_FACTORY = new SMInputFactory(XMLInputFactory.newInstance());
  private static final List<String> EMPTY_STRING_LIST = Collections.emptyList();
  private static final ObjectNode EMPTY_FILTER = new FiltersParam("{}").get();

  private static final String XML_FILE_EXTENSION = "xml";
  private static final String TXT_FILE_EXTENSION = "txt";
  private static final String GNOS_REPO = "GNOS";

  private static final String TERMS_LOOKUP = "terms-lookup";

  /*
   * We only have two documents in the test index. See this file for details:
   * /dcc-portal/dcc-portal-api/src/test/resources/fixtures/RepositoryFileServiceTest.json The following expected values
   * come from the file. Update the values here if the fixture file is changed.
   */
  private final static class ExpectedValues {

    final static String REPO_CODE_FOR_XML = "pcawg-heidelberg";
    final static String REPO_TYPE_FOR_XML = "GNOS";
    final static List<String> CONTENT_FOR_XML = ImmutableList.of(
        "c08b8fec-78a3-4f95-b472-953a5d0b46ef",
        "https://gtrepo-dkfz.annailabs.com/cghub/data/analysis/download/c08b8fec-78a3-4f95-b472-953a5d0b46ef",
        "ffe2ed5dedcf27e52280c16db5eb2d04.bam",
        "94359085765",
        "ffe2ed5dedcf27e52280c16db5eb2d04");
    final static String REPO_CODE_FOR_TSV = "tcga";
    final static String REPO_TYPE_FOR_TSV = "Web Archive";
    final static List<String> CONTENT_FOR_TSV = ImmutableList.of(
        "https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/"
            // + "cesc/bcr/nationwidechildrens.org/bio/clin/nationwidechildrens.org_CESC.bio.Level_1.114.68.0/"
            + "nationwidechildrens.org_clinical.TCGA-ZX-AA5X.xml",
        "nationwidechildrens.org_clinical.TCGA-ZX-AA5X.xml",
        "62464",
        "c2be918abd8d63c21cee69987a61fc23",
        "PCAWG");

  }

  private RepositoryFileService service;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() {
    this.testIndex = TestIndex.REPOSITORY;

    // This creates and populates the test index with fixture data.
    es.execute(createIndexMapping(Type.REPOSITORY_FILE_CENTRIC)
        .withData(bulkFile(getClass())));
    service =
        new RepositoryFileService(new RepositoryFileRepository(es.client(), testIndex.getName(), new IndexService()),
            new TermsLookupRepository(es.client(), TERMS_LOOKUP, testIndex.getName(), new PortalProperties()));
  }

  @Test
  public void test() throws IOException {
    val testArchive = temp.newFile();
    val timestamp = new Date();
    val repoInclusionList = EMPTY_STRING_LIST;
    val query = Query.builder()
        .filters(EMPTY_FILTER)
        .build();

    // Creates the test tar.gz archive.
    try (val output = new FileOutputStream(testArchive)) {
      service.generateManifestArchive(output, timestamp, query, repoInclusionList);
    }

    @Cleanup
    val inputStream = new FileInputStream(testArchive);
    @Cleanup
    val tar = new TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(inputStream)));

    ArchiveEntry tarEntry;
    while ((tarEntry = tar.getNextEntry()) != null) {
      if (tarEntry.isDirectory()) {
        continue;
      }

      val fileName = tarEntry.getName();
      val isXml = isXmlFile(fileName);

      validateFileName(fileName, isXml, timestamp);

      // Extracts a file from the archive
      val tempFile = temp.newFile();
      try (val output = new FileOutputStream(tempFile)) {
        IOUtils.copy(tar, output);
      }

      if (isXml) {
        validateXmlFile(tempFile);
      } else {
        validateTsvFile(tempFile);
      }

    }

  }

  private static boolean isXmlFile(String fileName) {
    return XML_FILE_EXTENSION.equalsIgnoreCase(getFileExtension(fileName));
  }

  private static boolean isGnosRepo(String repoType) {
    return GNOS_REPO.equalsIgnoreCase(repoType);
  }

  private static String getFileExtensionOf(String repoType) {
    return isGnosRepo(repoType) ? XML_FILE_EXTENSION : TXT_FILE_EXTENSION;
  }

  @NonNull
  private static String buildFileName(String repoCode, String repoType, Date timestamp) {
    return DOT.join(Arrays.asList(
        "manifest",
        repoCode,
        timestamp.getTime(),
        getFileExtensionOf(repoType)));
  }

  private static void validateFileName(String testFileName, boolean isXml, Date timestamp) {
    val expectedRepoCode = isXml ? ExpectedValues.REPO_CODE_FOR_XML : ExpectedValues.REPO_CODE_FOR_TSV;
    val expectedRepoType = isXml ? ExpectedValues.REPO_TYPE_FOR_XML : ExpectedValues.REPO_TYPE_FOR_TSV;
    val expectedFileName = buildFileName(expectedRepoCode, expectedRepoType, timestamp);

    assertThat(testFileName).isEqualTo(expectedFileName);
  }

  @SneakyThrows
  private static void validateXmlFile(File file) {
    val xmlDom = XML_READER_FACTORY.rootElementCursor(file);
    xmlDom.advance();

    val resultCursor = xmlDom.childElementCursor("Result");
    resultCursor.advance();

    val testRecordId = resultCursor.getAttrValue("id");
    assertThat(testRecordId).isEqualTo("1");

    val content = resultCursor.collectDescendantText();
    val testContent = FluentIterable.from(WHITESPACE.splitToList(content))
        .transform(value -> value.trim())
        .filter(value -> !isBlank(value))
        .toList();
    assertThat(testContent).isEqualTo(ExpectedValues.CONTENT_FOR_XML);

    xmlDom.getStreamReader().closeCompletely();
  }

  private static void validateTsvFile(File file) {
    val testContent = readLinesFromTsvFile(file);
    // The test fixture only contains one record/document for the non-GNOS type, therefore we only assert the first one.
    assertThat(testContent.get(0)).isEqualTo(ExpectedValues.CONTENT_FOR_TSV);
  }

  @SneakyThrows
  private static List<List<String>> readLinesFromTsvFile(File file) {
    @Cleanup
    val reader = new CsvListReader(new FileReader(file), TAB_PREFERENCE);
    val skipHeader = true;
    reader.getHeader(skipHeader);

    val result = Lists.<List<String>> newArrayList();
    List<String> line;

    while ((line = reader.read()) != null) {
      result.add(line);
    }

    return result;
  }

}
