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

package org.icgc.dcc.portal.repository;

import static com.github.tlrx.elasticsearch.test.EsSetup.createIndex;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.join;
import static org.icgc.dcc.portal.util.JsonUtils.parseFilters;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.junit.After;
import org.junit.Before;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tlrx.elasticsearch.test.EsSetup;
import com.github.tlrx.elasticsearch.test.provider.JSONProvider;
import com.github.tlrx.elasticsearch.test.request.CreateIndex;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

public abstract class BaseElasticSearchTest {

  /**
   * Test configuration.
   */
  protected static final String INDEX_NAME = "test_index";
  protected static final String REPO_INDEX_NAME = "icgc-repository";
  protected static final String SETTINGS_FILE_NAME = "index.settings.json";
  protected static final String JSON_DIR = "mappings";
  protected static final String FIXTURES_DIR = "src/test/resources/fixtures";
  protected static final URL SETTINGS_FILE = getMappingFileUrl(SETTINGS_FILE_NAME);

  @SneakyThrows
  private static URL getMappingFileUrl(String fileName) {
    return Resources.getResource(JSON_DIR + "/" + fileName);
  }

  protected static final IndexModel INDEX = new IndexModel(INDEX_NAME, REPO_INDEX_NAME);

  /**
   * Test data.
   */
  protected static final String MISSING_ID = "@@@@@@@@@@@";

  /**
   * ES facade.
   */
  protected EsSetup es;

  @Before
  public void before() {
    val settings = ImmutableSettings.settingsBuilder().put("script.groovy.sandbox.enabled", true).build();
    es = new EsSetup(settings);
  }

  @After
  public void after() {
    es.terminate();
  }

  protected String joinFilters(String... filters) {
    return "{" + join(filters, ",") + "}";
  }

  @SneakyThrows
  public void bulkInsert(JSONProvider provider) {
    byte[] content = provider.toJson().getBytes("UTF-8");
    checkState(!es.client()
        .prepareBulk()
        .add(content, 0, content.length, true)
        .setRefresh(true)
        .execute()
        .actionGet()
        .hasFailures());
  }

  @SneakyThrows
  public void bulkInsert() {
    bulkInsert(bulkFile(getClass()));
  }

  /**
   * Creates the index, settings and {@code TypeName} mapping
   * 
   * @param typeName - the index type to create
   * @return
   */
  protected static CreateIndex createIndexMapping(Type typeName) {
    return createIndexMappings(typeName);
  }

  protected static CreateIndex createIndexMappings(Type... typeNames) {
    CreateIndex request = createIndex(INDEX_NAME)
        .withSettings(settingsSource(SETTINGS_FILE));

    for (Type typeName : typeNames) {
      request = request.withMapping(typeName.getId(), mappingSource(typeName));
    }

    return request;
  }

  protected static FileJSONProvider jsonFile(File file) {
    return new FileJSONProvider(file);
  }

  protected static BulkJSONProvider bulkFile(File file) {
    return new BulkJSONProvider(file);
  }

  protected static BulkJSONProvider bulkFile(String fileName) {
    return new BulkJSONProvider(new File(FIXTURES_DIR, fileName));
  }

  protected static BulkJSONProvider bulkFile(Class<?> testClass) {
    return new BulkJSONProvider(new File(FIXTURES_DIR, testClass.getSimpleName() + ".json"));
  }

  protected static ObjectNode filters(String jsonish) {
    return (ObjectNode) parseFilters(jsonish);
  }

  @SneakyThrows
  protected static String settingsSource() {
    return settingsSource(SETTINGS_FILE);
  }

  @SneakyThrows
  private static String settingsSource(URL settingsFile) {
    // Override production values that would introduce test timing delays / issues
    return objectNode(settingsFile)
        .put("index.number_of_shards", 1)
        .put("index.number_of_replicas", 0)
        .toString();
  }

  private static String mappingSource(Type typeName) {
    return mappingSource(mappingFile(typeName));
  }

  @SneakyThrows
  private static String mappingSource(URL mappingFile) {
    return json(mappingFile);
  }

  private static URL mappingFile(Type typeName) {
    String mappingFileName = typeName.getId() + ".mapping.json";
    return getMappingFileUrl(mappingFileName);
  }

  private static String json(URL url) throws IOException, JsonProcessingException {
    return objectNode(url).toString();
  }

  private static ObjectNode objectNode(URL url) throws IOException, JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return (ObjectNode) mapper.readTree(url);
  }

  /**
   * {@link JSONProvider} implementation that can read files from the local file system.
   */
  @RequiredArgsConstructor
  private static class FileJSONProvider implements JSONProvider {

    @NonNull
    private final File file;

    @Override
    @SneakyThrows
    public String toJson() {
      return Files.toString(file, UTF_8);
    }

  }

  /**
   * {@link JSONProvider} implementation that can read formatted concatenated Elasticsearch bulk load files as specified
   * in http://www.elasticsearch.org/guide/reference/api/bulk/.
   */
  @RequiredArgsConstructor
  protected static class BulkJSONProvider implements JSONProvider {

    @NonNull
    private final File file;

    @Override
    @SneakyThrows
    public String toJson() {
      // Normalize to non-pretty printed in memory representation
      ObjectReader reader = new ObjectMapper().reader(JsonNode.class);
      MappingIterator<JsonNode> iterator = reader.readValues(file);

      StringBuilder builder = new StringBuilder();
      while (iterator.hasNext()) {
        // Write non-pretty printed
        JsonNode jsonNode = iterator.nextValue();
        builder.append(jsonNode);
        builder.append("\n");
      }

      return builder.toString();
    }
  }

  protected Object cast(Object object) {
    return object;
  }

}
