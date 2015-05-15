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

import static com.google.common.collect.Iterables.transform;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU;
import static org.icgc.dcc.common.core.util.FormatUtils.formatBytes;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Resource;
import javax.ws.rs.core.StreamingOutput;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.icgc.dcc.portal.model.ExternalFile;
import org.icgc.dcc.portal.model.ExternalFiles;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Pagination;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.repository.ExternalFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.supercsv.io.CsvListWriter;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimaps;

@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class ExternalFileService {

  // This constant is only used as a "magic number" when allocating initial size for ByteArrayOutputStream.
  private static final int ESTIMATED_ROW_BYTE_COUNT = 256;
  private static final String MANIFEST_FILE_NAME_SUFFIX = ".tsv";

  private final ExternalFileRepository externalFileRepository;

  @Resource
  private Map<String, String> repoIndexMetadata;

  public Map<String, String> getIndexMetadata() {
    return repoIndexMetadata;
  }

  public StreamingOutput exportTableData(Query query) {
    return externalFileRepository.exportData(query);
  }

  public ExternalFiles findAll(Query query) {
    SearchResponse response = externalFileRepository.findAll(query);
    SearchHits hits = response.getHits();

    val list = ImmutableList.<ExternalFile> builder();

    for (SearchHit hit : hits) {
      val fieldMap = createResponseMap(hit, query, Kind.EXTERNAL_FILE);
      list.add(new ExternalFile(fieldMap));
    }

    val externalFiles = new ExternalFiles(list.build());
    externalFiles.setTermFacets(externalFileRepository.convertAggregations2Facets(response.getAggregations()));
    // externalFiles.setFacets(response.getFacets());
    externalFiles.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return externalFiles;
  }

  @NonNull
  private static void writeRowsToStream(Iterable<SimpleImmutableEntry<String, List<String>>> rows,
      OutputStream outputStream)
      throws IOException {
    @Cleanup
    val writer = new CsvListWriter(new OutputStreamWriter(outputStream), TAB_PREFERENCE);

    for (val row : rows) {
      writer.write(row.getValue());
    }

    writer.flush();
  }

  @NonNull
  private static void addFileToTar(String fileKey, ByteArrayOutputStream content, TarArchiveOutputStream tar)
      throws IOException {
    val fileName = "./" + fileKey + MANIFEST_FILE_NAME_SUFFIX;
    val tarEntry = new TarArchiveEntry(fileName);

    tarEntry.setSize(content.size());
    tar.putArchiveEntry(tarEntry);

    content.writeTo(tar);
    tar.closeArchiveEntry();
  }

  /**
   * This method takes an ExternalFile and expands it (on the Repository field) into a collection of key-value pairs
   * with the value being a list of columns required in the Manifest file. This collection will then be joined (via
   * flatMap) and used in a group-by operation.
   */
  @NonNull
  // Right now it's set to package-private for unit test.
  static Iterable<SimpleImmutableEntry<String, List<String>>> expandRows(ExternalFile file) {
    return transform(file.getRepositoryNames(),
        rowKey -> new SimpleImmutableEntry<>(rowKey, buildManifestColumns(file)));
  }

  @NonNull
  private static List<String> buildManifestColumns(ExternalFile fileInfo) {
    // Right now this is simple. Once the spec has been finalized, we'd probably need to consider another column
    // format for GNOS types.
    return ImmutableList.of(
        fileInfo.getFileName(),
        fileInfo.getMd5(),
        formatBytes(fileInfo.getFileSize()),
        fileInfo.getUrl()
        );
  }

  @NonNull
  // Right now it's set to package-private for unit test.
  static void generateManifestArchive(Iterable<ExternalFile> files, OutputStream outputStream)
      throws IOException {
    // Guava's transformAndConcat() is like flatMap.
    val allRows = FluentIterable.from(files)
        .transformAndConcat(file -> expandRows(file));
    // Here we are doing a group-by on the Repository name as the key, which is used later to represent a file in the
    // tarball.
    val groupedRows = Multimaps.index(allRows, row -> row.getKey());

    @Cleanup
    val tar = new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(outputStream)));

    tar.setLongFileMode(LONGFILE_GNU);

    val fileKeys = groupedRows.keySet().asList();

    for (val fileKey : fileKeys) {
      val rows = groupedRows.get(fileKey);

      @Cleanup
      val content = new ByteArrayOutputStream(ESTIMATED_ROW_BYTE_COUNT * rows.size());

      writeRowsToStream(rows, content);
      addFileToTar(fileKey, content, tar);
    }

  }

}
