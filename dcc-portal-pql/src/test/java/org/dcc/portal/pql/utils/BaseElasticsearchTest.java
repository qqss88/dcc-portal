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

package org.dcc.portal.pql.utils;

import static com.github.tlrx.elasticsearch.test.EsSetup.createIndex;

import java.io.File;
import java.io.IOException;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.utils.EsAstTransformator;
import org.dcc.portal.pql.es.utils.ParseTrees;
import org.dcc.portal.pql.qe.PqlParseListener;
import org.dcc.portal.pql.qe.QueryContext;
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

public class BaseElasticsearchTest {

  /**
   * Test configuration.
   */
  protected static final String INDEX_NAME = "dcc-release-etl-cli";
  protected static final String SETTINGS_FILE_NAME = "index.settings.json";
  protected static final String JSON_DIR = "src/test/resources/org/icgc/dcc/etl/indexer";
  protected static final String FIXTURES_DIR = "src/test/resources/fixtures";
  protected static final File SETTINGS_FILE = new File(JSON_DIR, SETTINGS_FILE_NAME);
  protected static final IndexModel INDEX = new IndexModel(INDEX_NAME);

  /**
   * Test data.
   */
  protected static final String MISSING_ID = "@@@@@@@@@@@";

  /**
   * ES facade.
   */
  protected EsSetup es;

  /**
   * Parser's setup
   */
  protected PqlParseListener listener;
  protected QueryContext queryContext;
  protected EsAstTransformator esAstTransformator = new EsAstTransformator();

  @Before
  public void before() {
    val settings = ImmutableSettings.settingsBuilder().put("script.groovy.sandbox.enabled", true).build();
    es = new EsSetup(settings);
  }

  @After
  public void after() {
    es.terminate();
  }

  protected static CreateIndex createIndexMappings(Type... typeNames) {
    CreateIndex request = createIndex(INDEX_NAME)
        .withSettings(settingsSource(SETTINGS_FILE));

    for (Type typeName : typeNames) {
      request = request.withMapping(typeName.getId(), mappingSource(typeName));
    }

    return request;
  }

  protected static BulkJSONProvider bulkFile(Class<?> testClass) {
    return new BulkJSONProvider(new File(FIXTURES_DIR, testClass.getSimpleName() + ".txt"));
  }

  @SneakyThrows
  private static String settingsSource(File settingsFile) {
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
  private static String mappingSource(File mappingFile) {
    return json(mappingFile);
  }

  private static File mappingFile(Type typeName) {
    String mappingFileName = typeName.getId() + ".mapping.json";
    return new File(JSON_DIR, mappingFileName);
  }

  private static String json(File file) throws IOException, JsonProcessingException {
    return objectNode(file).toString();
  }

  private static ObjectNode objectNode(File file) throws IOException, JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return (ObjectNode) mapper.readTree(file);
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

  protected ExpressionNode createTree(String query) {
    val parser = ParseTrees.getParser(query);
    parser.addParseListener(listener);
    parser.statement();

    return listener.getEsAst();
  }
}
