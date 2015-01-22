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

import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.BaseEntityList;
import org.icgc.dcc.portal.model.DerivedEntityListDefinition;
import org.icgc.dcc.portal.model.EntityList;
import org.icgc.dcc.portal.model.EntityListDefinition;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.repository.DonorRepository;
import org.icgc.dcc.portal.repository.EntityListRepository;
import org.icgc.dcc.portal.repository.GeneRepository;
import org.icgc.dcc.portal.repository.MutationRepository;
import org.icgc.dcc.portal.repository.Repository;
import org.icgc.dcc.portal.repository.UnionAnalyzer;
import org.icgc.dcc.portal.service.TermsLookupService.TermLookupType;
import org.icgc.dcc.portal.util.SearchResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * TODO
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class EntityListService {

  @NonNull
  private final EntityListRepository repository;

  @NonNull
  private final UnionAnalyzer analyzer;

  @NonNull
  private final TermsLookupService termLookupService;

  @NonNull
  private final GeneRepository geneRepository;
  @NonNull
  private final MutationRepository mutationRepository;
  @NonNull
  private final DonorRepository donorRepository;

  private List<String> executeQuery(final EntityListDefinition definition) {

    val maxGeneCount = 1000;

    log.info("List def is: " + definition);

    val limitedGeneQuery = Query.builder()
        // .fields(idField())
        .filters(definition.getFilters())
        .sort(definition.getSortBy())
        .order(definition.getSortOrder().getName())

        // This is non standard in terms of size of result set, but its just ids
        .size(maxGeneCount)
        .limit(maxGeneCount)
        .build();

    Repository repo = geneRepository;
    switch (definition.getType()) {
    case DONOR: {
      repo = donorRepository;
      break;
    }
    case MUTATION: {
      repo = mutationRepository;
      break;
    }
    }

    return SearchResponses.getHitIds(repo.findAllCentric(limitedGeneQuery));
  }

  public EntityList getEntityList(@NonNull final UUID listId) {

    val list = repository.find(listId);

    if (null == list) {

      log.info("No list is found for id: '{}'.", listId);

    } else {

      log.info("Got enity list: '{}'.", list);
    }
    return list;
  }

  // TODO: temp
  private static TermLookupType getLookupTypeFrom(final BaseEntityList.Type type) {

    TermLookupType result = TermLookupType.GENE_IDS;

    switch (type) {

    case DONOR: {
      result = TermLookupType.DONOR_IDS;
      break;
    }
    case MUTATION: {
      result = TermLookupType.MUTATION_IDS;
      break;
    }
    }
    return result;
  }

  public EntityList createEntityList(
      @NonNull final EntityListDefinition listDefinition) {

    val newList = EntityList.createFromDefinition(listDefinition);
    repository.save(newList);

    val listIds = executeQuery(listDefinition);
    termLookupService.createTermsLookup(getLookupTypeFrom(listDefinition.getType()), newList.getId(), listIds);

    repository.update(newList.finished(listIds.size()));

    return newList;
  }

  public EntityList deriveEntityList(
      @NonNull final DerivedEntityListDefinition listDefinition) {

    val newList = EntityList.createFromDefinition(listDefinition);
    repository.save(newList);

    val definitions = listDefinition.getUnion();
    val entityType = listDefinition.getType();

    val count = analyzer.unionAll(definitions, entityType,
        // TODO: temp
        newList.getId(), termLookupService);

    repository.update(newList.finished(count));

    return newList;
  }
}
