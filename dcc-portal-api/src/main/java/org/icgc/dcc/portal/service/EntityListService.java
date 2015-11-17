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

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;
import static lombok.AccessLevel.PRIVATE;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.icgc.dcc.portal.analysis.UnionAnalyzer;
import org.icgc.dcc.portal.config.PortalProperties;
import org.icgc.dcc.portal.model.BaseEntitySet;
import org.icgc.dcc.portal.model.BaseEntitySetDefinition;
import org.icgc.dcc.portal.model.DerivedEntitySetDefinition;
import org.icgc.dcc.portal.model.EntitySet;
import org.icgc.dcc.portal.model.EntitySet.SubType;
import org.icgc.dcc.portal.model.EntitySetDefinition;
import org.icgc.dcc.portal.repository.EntityListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.supercsv.io.CsvListWriter;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * A service to facilitate entity set operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class EntityListService {

  @NonNull
  private final EntityListRepository repository;
  @NonNull
  private final UnionAnalyzer analyzer;
  @NonNull
  private final PortalProperties properties;

  @Getter(lazy = true, value = PRIVATE)
  private final int currentDataVersion = resolveDataVersion();

  @SneakyThrows
  private static String toFilterParamForGeneSymbols(@NonNull final String symbolList) {
    // Build the ObjectNode to represent this filterParam: { "gene": "symbol": {"is": ["s1", "s2", ...]}
    val nodeFactory = new JsonNodeFactory(false);
    val root = nodeFactory.objectNode();
    val gene = nodeFactory.objectNode();
    root.put("gene", gene);
    val symbol = nodeFactory.objectNode();
    gene.put("symbol", symbol);
    val isNode = nodeFactory.arrayNode();
    symbol.put("is", isNode);

    final String[] symbols = symbolList.split(",");
    for (val s : symbols) {
      isNode.add(s);
    }

    val result = root.toString();
    return URLEncoder.encode(result, UTF_8.name());
  }

  public EntitySet getEntityList(@NonNull final UUID entitySetId) {
    val list = repository.find(entitySetId);

    if (null == list) {
      log.error("No list is found for id: '{}'.", entitySetId);
    } else {
      log.debug("Got enity list: '{}'.", list);
    }

    return list;
  }

  public EntitySet createEntityList(@NonNull final EntitySetDefinition entitySetDefinition, boolean async) {
    val newEntitySet = createAndSaveNewListFrom(entitySetDefinition);
    if (async) {
      analyzer.materializeListAsync(newEntitySet.getId(), entitySetDefinition);
    } else {
      analyzer.materializeList(newEntitySet.getId(), entitySetDefinition);
    }
    return newEntitySet;
  }

  public EntitySet createExternalEntityList(@NonNull final EntitySetDefinition entitySetDefinition) {
    val newEntitySet = createAndSaveNewListFrom(entitySetDefinition);
    analyzer.materializeRepositoryList(newEntitySet.getId(), entitySetDefinition);
    return newEntitySet;
  }

  public EntitySet createFileEntitySet(@NonNull final EntitySetDefinition entitySetDefinition) {
    val newEntitySet = createAndSaveNewListFrom(entitySetDefinition);
    analyzer.materializeFileSet(newEntitySet.getId(), entitySetDefinition);
    return newEntitySet;
  }

  public EntitySet computeEntityList(@NonNull final DerivedEntitySetDefinition entitySetDefinition, boolean async) {
    val newEntitySet = createAndSaveNewListFrom(entitySetDefinition);
    if (async) {
      analyzer.combineListsAsync(newEntitySet.getId(), entitySetDefinition);
    } else {
      analyzer.combineLists(newEntitySet.getId(), entitySetDefinition);
    }
    return newEntitySet;
  }

  private int resolveDataVersion() {
    return properties.getRelease().getDataVersion();
  }

  private EntitySet createAndSaveNewListFrom(final BaseEntitySet entitySetDefinition, final SubType subtype) {
    val dataVersion = getCurrentDataVersion();
    val newEntitySet = EntitySet.createFromDefinition(entitySetDefinition, dataVersion);

    if (null != subtype) {
      newEntitySet.setSubtype(subtype);
    }

    val insertCount = repository.save(newEntitySet, dataVersion);
    checkState(insertCount == 1, "Could not save list - Insert count: %s", insertCount);

    return newEntitySet;
  }

  private EntitySet createAndSaveNewListFrom(final BaseEntitySetDefinition entitySetDefinition) {
    val subtype = entitySetDefinition.isTransient() ? SubType.TRANSIENT : null;
    return createAndSaveNewListFrom(entitySetDefinition, subtype);
  }

  // Helpers to facilitate exportListItems() only. They should not be used in anywhere else.
  private List<List<String>> convertToListOfList(@NonNull final List<String> list) {
    val result = new ArrayList<List<String>>(list.size());

    for (val v : list) {
      result.add(Arrays.asList(v));
    }

    return result;
  }

  private List<List<String>> convertToListOfListForGene(@NonNull final Map<String, String> map) {
    val result = new ArrayList<List<String>>(map.size());
    val entrySet = map.entrySet();

    for (val v : entrySet) {
      result.add(Arrays.asList(v.getKey(), v.getValue()));
    }

    return result;
  }

  public void exportListItems(@NonNull EntitySet entitySet, @NonNull OutputStream outputStream) throws IOException {
    val isGeneType = BaseEntitySet.Type.GENE == entitySet.getType();
    // I need this 'convolution' to achieve the correct type inference to satisfy CsvListWriter.write (List<?>)
    // overload.
    val content =
        isGeneType ? convertToListOfListForGene(
            analyzer.retrieveGeneIdsAndSymbolsByListId(entitySet.getId())) : convertToListOfList(
                analyzer.retriveListItems(entitySet));

    @Cleanup
    val writer = new CsvListWriter(new OutputStreamWriter(outputStream), TAB_PREFERENCE);

    for (val v : content) {
      writer.write(v);
    }

    writer.flush();
  }
}
