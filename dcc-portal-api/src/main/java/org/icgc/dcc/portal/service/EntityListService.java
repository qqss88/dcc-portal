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

import static com.google.common.base.Preconditions.checkState;

import java.util.UUID;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.analysis.UnionAnalyzer;
import org.icgc.dcc.portal.model.BaseEntityList;
import org.icgc.dcc.portal.model.DerivedEntityListDefinition;
import org.icgc.dcc.portal.model.EntityList;
import org.icgc.dcc.portal.model.EntityListDefinition;
import org.icgc.dcc.portal.repository.EntityListRepository;
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

  public EntityList getEntityList(@NonNull final UUID listId) {

    val list = repository.find(listId);

    if (null == list) {

      log.error("No list is found for id: '{}'.", listId);

    } else {

      log.debug("Got enity list: '{}'.", list);
    }
    return list;
  }

  public EntityList createEntityList(
      @NonNull final EntityListDefinition listDefinition) {

    val newList = createNewListFrom(listDefinition);

    analyzer.createList(newList.getId(), listDefinition);

    return newList;
  }

  public EntityList deriveEntityList(
      @NonNull final DerivedEntityListDefinition listDefinition) {

    val newList = createNewListFrom(listDefinition);

    analyzer.combineLists(newList.getId(), listDefinition);

    return newList;
  }

  private EntityList createNewListFrom(final BaseEntityList listDefinition) {

    val newList = EntityList.createFromDefinition(listDefinition);

    val insertCount = repository.save(newList);
    checkState(insertCount == 1, "Could not save list - Insert count: %s", insertCount);

    return newList;
  }
}
