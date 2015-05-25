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

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.icgc.dcc.common.core.util.Joiners.COMMA;
import static org.icgc.dcc.common.core.util.Joiners.SLASH;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Resource;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.lang.time.DateFormatUtils;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.MapType;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class ExternalFileService {

  private static final String DATE_FORMAT_PATTERN = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern();
  private static final String GNOS_REPO = "GNOS";
  private static final String[] DOWNLOAD_INFO_QUERY_FIELDS = new String[] {
      FieldNames.REPO_TYPE,
      FieldNames.REPO_ID,
      FieldNames.DATA_PATH,
      FieldNames.FILE_NAME,
      FieldNames.FILE_SIZE,
      FieldNames.CHECK_SUM
  };
  private static final String DOWNLOAD_INFO_QUERY_SOURCE_FIELD =
      FieldNames.REPOSITORY + "." + FieldNames.REPO_SERVER + ".*";
  private static final String[] TSV_HEADERS = new String[] {
      "url",
      "file_name",
      "file_size",
      "md5_sum"
  };
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ObjectReader READER = MAPPER.reader();
  private static final MapType MAP_TYPE = MAPPER.getTypeFactory()
      .constructMapType(Map.class, String.class, String.class);

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

    for (val hit : hits) {
      val fieldMap = createResponseMap(hit, query, Kind.EXTERNAL_FILE);
      fieldMap.put("_id", hit.getId());
      list.add(new ExternalFile(fieldMap));
    }

    val externalFiles = new ExternalFiles(list.build());
    externalFiles.setTermFacets(externalFileRepository.convertAggregations2Facets(response.getAggregations()));
    // externalFiles.setFacets(response.getFacets());
    externalFiles.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return externalFiles;
  }

  @NonNull
  public void generateManifestArchive(OutputStream output, Date timestamp, Query query, List<String> repoList)
      throws JsonProcessingException, IOException {
    // FIXME - find the appropriate value or another way to set the proper size.
    val maxRecordCount = 1000000;
    query.setLimit(maxRecordCount);

    val esResponse = externalFileRepository.findDownloadInfo(query,
        DOWNLOAD_INFO_QUERY_FIELDS,
        FieldNames.REPO_TYPE,
        DOWNLOAD_INFO_QUERY_SOURCE_FIELD);
    val hits = newArrayList(esResponse.getHits().hits());
    val all = FluentIterable.from(hits)
        .transformAndConcat(hit -> expandByFlatteningRepoServers(hit));

    @Cleanup
    val tar = new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(output)));
    tar.setLongFileMode(LONGFILE_GNU);

    val repoCodeGroups = Multimaps.index(all, entry -> entry.get(FieldNames.REPO_CODE));
    val repoIncludes = newArrayList(filter(repoList, repoCode -> !isBlank(repoCode)));

    for (val repoCode : repoCodeGroups.keySet()) {
      if (shouldRepositoryBeExcluded(repoIncludes, repoCode)) {
        continue;
      }

      val entries = repoCodeGroups.get(repoCode);

      if (entries.isEmpty()) {
        continue;
      }

      // Entries with the same repoCode should & must have the same repoType.
      val repoType = entries.get(0).get(FieldNames.REPO_TYPE);

      generateTarEntry(tar, entries, repoCode, repoType, timestamp);
    }

  }

  @NonNull
  private static boolean shouldRepositoryBeExcluded(List<String> repoIncludes, String repoCode) {
    return !(repoIncludes.isEmpty() || repoIncludes.contains(repoCode));
  }

  @SneakyThrows
  private static Iterable<Map<String, String>> expandByFlatteningRepoServers(SearchHit hit) {
    val fields = Maps.transformValues(hit.getFields(), field -> field.getValues().get(0).toString());
    val serverArray = READER.readTree(hit.sourceAsString())
        .path(FieldNames.REPOSITORY)
        .path(FieldNames.REPO_SERVER);
    val servers = Lists.<Map<String, String>> newArrayList();

    // Converts the arrayNode to a typed iterable just so Guava's transform() can be used.
    for (val server : serverArray) {
      servers.add(MAPPER.<Map<String, String>> convertValue(server, MAP_TYPE));
    }

    return transform(servers,
        server -> ImmutableMap.<String, String> builder()
            // Merges these two maps - there shouldn't be collision on the keys.
            .putAll(fields)
            .putAll(server)
            .build());
  }

  @SneakyThrows
  @NonNull
  private static void generateTarEntry(TarArchiveOutputStream tar, List<Map<String, String>> allFileInfoOfOneRepo,
      String repoCode, String repoType, Date timestamp) {
    // FIXME - magic number
    val bufferSize = 1024 * 100;

    // A buffer to hold all the file content before writing it to the tar archive
    @Cleanup
    val buffer = new ByteArrayOutputStream(bufferSize);

    val downloadUrlGroups = Multimaps.index(allFileInfoOfOneRepo,
        entry -> buildDownloadUrl(
            entry.get(FieldNames.BASE_URL),
            entry.get(FieldNames.DATA_PATH),
            entry.get(FieldNames.REPO_ID)));

    if (isGnosRepo(repoType)) {
      generateXmlFile(buffer, downloadUrlGroups, timestamp);
    } else {
      generateTextFile(buffer, downloadUrlGroups);
    }

    addFileToTar(buildFileName(repoCode, repoType, timestamp), buffer, tar);
  }

  @SneakyThrows
  @NonNull
  private static void generateXmlFile(OutputStream buffer, ListMultimap<String, Map<String, String>> downloadUrlGroups,
      Date timestamp) {
    int rowCount = 0;
    // Perhaps make this static???
    val factory = XMLOutputFactory.newInstance();
    @Cleanup
    val writer = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(buffer));

    startXmlDocument(writer, timestamp);

    for (val url : downloadUrlGroups.keySet()) {
      val rowInfo = downloadUrlGroups.get(url);

      if (rowInfo.isEmpty()) {
        continue;
      }

      val repoId = rowInfo.get(0).get(FieldNames.REPO_ID);

      writeXmlEntry(writer, repoId, url, rowInfo, ++rowCount);
    }

    endXmlDocument(writer);
  }

  @SneakyThrows
  @NonNull
  private static void generateTextFile(OutputStream buffer, Multimap<String, Map<String, String>> downloadUrlGroups) {
    @Cleanup
    val tsv = new CsvListWriter(new OutputStreamWriter(buffer), TAB_PREFERENCE);
    tsv.writeHeader(TSV_HEADERS);

    for (val url : downloadUrlGroups.keySet()) {
      val fileInfo = downloadUrlGroups.get(url);
      val fileNames = transform(fileInfo, r -> r.get(FieldNames.FILE_NAME));
      val fileSizes = transform(fileInfo, r -> r.get(FieldNames.FILE_SIZE));
      val checksums = transform(fileInfo, r -> r.get(FieldNames.CHECK_SUM));
      val row = ImmutableList.of(
          url,
          COMMA.join(fileNames),
          COMMA.join(fileSizes),
          COMMA.join(checksums));

      tsv.write(row);
    }

    tsv.flush();
  }

  private final static class FieldNames {

    final static String REPOSITORY = "repository";
    final static String REPO_SERVER = "repo_server";
    final static String REPO_CODE = "repo_code";
    final static String BASE_URL = "repo_base_url";

    final static String REPO_TYPE = REPOSITORY + ".repo_type";
    final static String DATA_PATH = REPOSITORY + ".repo_data_path";
    final static String REPO_ID = REPOSITORY + ".repo_entity_id";

    final static String FILE_NAME = REPOSITORY + ".file_name";
    final static String FILE_SIZE = REPOSITORY + ".file_size";
    final static String CHECK_SUM = REPOSITORY + ".file_md5sum";

  }

  private final static class XmlTags {

    final static String ROOT = "ResultSet";
    final static String RECORD = "Result";
    final static String RECORD_ID = "analysis_id";
    final static String RECORD_URI = "analysis_data_uri";
    final static String FILES = "files";
    final static String FILE = "file";
    final static String FILE_NAME = "filename";
    final static String FILE_SIZE = "filesize";
    final static String CHECK_SUM = "checksum";

  }

  private static boolean isGnosRepo(String repoType) {
    return GNOS_REPO.equals(repoType);
  }

  private static String getFileExtensionOf(String repoType) {
    return isGnosRepo(repoType) ? ".xml" : ".txt";
  }

  @NonNull
  private static String buildFileName(String repoCode, String repoType, Date timestamp) {
    return "manifest." +
        repoCode + "." +
        timestamp.getTime() +
        getFileExtensionOf(repoType);
  }

  @NonNull
  private static void addFileToTar(String fileName, ByteArrayOutputStream content, TarArchiveOutputStream tar)
      throws IOException {
    val tarEntry = new TarArchiveEntry(fileName);

    tarEntry.setSize(content.size());
    tar.putArchiveEntry(tarEntry);

    content.writeTo(tar);
    tar.closeArchiveEntry();
  }

  @NonNull
  private static String buildDownloadUrl(String baseUrl, String dataPath, String id) {
    return SLASH.join(baseUrl, dataPath, id)
        .replace("///", "/");
  }

  @NonNull
  private static String formatToUtc(Date timestamp) {
    return DateFormatUtils.formatUTC(timestamp, DATE_FORMAT_PATTERN);
  }

  @NonNull
  private static void startXmlDocument(XMLStreamWriter writer, Date timestamp) throws XMLStreamException {
    writer.writeStartDocument("utf-8", "1.0");
    writer.writeStartElement(XmlTags.ROOT);
    writer.writeAttribute("date", formatToUtc(timestamp));
  }

  @NonNull
  private static void endXmlDocument(XMLStreamWriter writer) throws XMLStreamException {
    // Closes the root element - XmlTags.ROOT
    writer.writeEndElement();
    writer.writeEndDocument();
  }

  @NonNull
  private static void addDownloadUrlEntryToXml(XMLStreamWriter writer, String id, String downloadUrl, final int rowCount)
      throws XMLStreamException {
    writer.writeStartElement(XmlTags.RECORD);
    writer.writeAttribute("id", String.valueOf(rowCount));

    addXmlElement(writer, XmlTags.RECORD_ID, id);
    addXmlElement(writer, XmlTags.RECORD_URI, downloadUrl);

    writer.writeStartElement(XmlTags.FILES);
  }

  @NonNull
  private static void closeDownloadUrlElement(XMLStreamWriter writer) throws XMLStreamException {
    // Close off XmlTags.FILES ("files") element.
    writer.writeEndElement();
    // Close off XmlTags.RECORD ("Result") element.
    writer.writeEndElement();
  }

  @NonNull
  private static void addFileInfoEntriesToXml(XMLStreamWriter writer, Iterable<Map<String, String>> info)
      throws XMLStreamException {
    for (val fileInfo : info) {
      writer.writeStartElement(XmlTags.FILE);

      addXmlElement(writer, XmlTags.FILE_NAME, fileInfo.get(FieldNames.FILE_NAME));
      addXmlElement(writer, XmlTags.FILE_SIZE, fileInfo.get(FieldNames.FILE_SIZE));

      writer.writeStartElement(XmlTags.CHECK_SUM);
      writer.writeAttribute("type", "md5");
      writer.writeCharacters(fileInfo.get(FieldNames.CHECK_SUM));
      writer.writeEndElement();

      writer.writeEndElement();
    }
  }

  @NonNull
  private static void addXmlElement(XMLStreamWriter writer, String elementName, String elementValue)
      throws XMLStreamException {
    writer.writeStartElement(elementName);
    writer.writeCharacters(elementValue);
    writer.writeEndElement();
  }

  @NonNull
  private static void writeXmlEntry(XMLStreamWriter writer, String id, String downloadUrl,
      Iterable<Map<String, String>> fileInfo, final int rowCount) throws XMLStreamException {
    addDownloadUrlEntryToXml(writer, id, downloadUrl, rowCount);
    addFileInfoEntriesToXml(writer, fileInfo);
    closeDownloadUrlElement(writer);
  }

}
