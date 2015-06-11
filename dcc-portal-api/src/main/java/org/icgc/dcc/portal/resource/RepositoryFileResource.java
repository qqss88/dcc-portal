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
package org.icgc.dcc.portal.resource;

import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.sun.jersey.core.header.ContentDisposition.type;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.icgc.dcc.common.core.util.Splitters.COMMA;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FIELD_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FIELD_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FILE_IDS_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FILE_IDS_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FILE_REPOS_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FILE_REPOS_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FILTER_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FILTER_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FROM_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FROM_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_INCLUDE_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_INCLUDE_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ORDER_ALLOW;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ORDER_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ORDER_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SIZE_ALLOW;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SIZE_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SIZE_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SORT_FIELD;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SORT_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_FILTERS;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_FROM;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_ORDER;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_SIZE;
import static org.icgc.dcc.portal.resource.ResourceUtils.checkRequest;
import static org.icgc.dcc.portal.resource.ResourceUtils.query;
import static org.icgc.dcc.portal.util.MediaTypes.GZIP;
import static org.icgc.dcc.portal.util.MediaTypes.TEXT_TSV;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.RepositoryFile;
import org.icgc.dcc.portal.model.RepositoryFiles;
import org.icgc.dcc.portal.service.RepositoryFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.dropwizard.jersey.params.IntParam;
import com.yammer.metrics.annotation.Timed;

@Component
@Slf4j
@Path("/v1/repository/files")
@Produces(APPLICATION_JSON)
@Api(value = "/repository/files", description = "Resources relating to external files")
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class RepositoryFileResource {

  private final static String TYPE_ATTACHMENT = "attachment";

  private final RepositoryFileService repositoryFileService;

  @Path("/{fileId}")
  @GET
  @Timed
  @ApiOperation(value = "Find by fileId", response = RepositoryFile.class)
  public RepositoryFile find(
      @ApiParam(value = "File Id", required = true) @PathParam("fileId") String id
      ) {
    return repositoryFileService.findOne(id);
  }

  @GET
  @Timed
  @ApiOperation(value = "Returns a list of RepositoryFiles", response = RepositoryFile.class)
  public RepositoryFiles findAll(
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue("fileName") String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order) {

    ObjectNode filters = filtersParam.get();

    return repositoryFileService.findAll(query().fields(fields).filters(filters)
        .from(from.get())
        .size(size.get())
        .sort(sort)
        .order(order)
        .includes(include)
        .build());
  }

  @GET
  @Timed
  @Path("/donors/count")
  public Long getUniqueDonorCount(
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    return repositoryFileService.getDonorCount(toQuery(filtersParam));
  }

  @GET
  @Timed
  @Path("/summary")
  public Map<String, Long> getSummary(
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    return repositoryFileService.getSummary(query().filters(filtersParam.get()).build());
  }

  @GET
  @Timed
  @Path("/export")
  @Produces(TEXT_TSV)
  public Response exportFiles(
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {

    val stream = repositoryFileService.exportTableData(toQuery(filtersParam));
    // Make this similar to client-side export naming format
    val fileName = String.format("repository_%s.tsv", (new SimpleDateFormat("yyyy_MM_dd").format(new Date())));

    return Response.ok(stream).header(CONTENT_DISPOSITION,
        type(TYPE_ATTACHMENT).fileName(fileName).creationDate(new Date()).build()).build();
  }

  @GET
  @Path("/metadata")
  @Timed
  public Map<String, String> getIndexMetaData() {
    return repositoryFileService.getIndexMetadata();
  }

  @GET
  @Path("/manifest")
  @Produces(GZIP)
  @Timed
  public Response generateManifestArchiveByFilters(
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FILE_REPOS_VALUE) @QueryParam(API_FILE_REPOS_PARAM) @DefaultValue("") String repoList) {
    log.info("filtersParam is: '{}' AND repoList is: '{}'.", filtersParam, repoList);

    val timestamp = new Date();
    final StreamingOutput outputGenerator = (outputStream) ->
        repositoryFileService.generateManifestArchive(outputStream,
            timestamp,
            toQuery(filtersParam),
            COMMA.splitToList(repoList));
    val attechmentType = type(TYPE_ATTACHMENT)
        .fileName(buildManifestFileName(timestamp))
        .creationDate(timestamp)
        .modificationDate(timestamp)
        .build();

    return Response
        .ok(outputGenerator)
        .header(CONTENT_DISPOSITION, attechmentType)
        .build();
  }

  @POST
  @Path("/manifest")
  @Consumes(APPLICATION_FORM_URLENCODED)
  @Timed
  public Response generateManifestArchiveByIdList(
      @ApiParam(value = API_FILE_IDS_VALUE) @FormParam(API_FILE_IDS_PARAM) List<String> fileIds,
      @ApiParam(value = API_FILE_REPOS_VALUE) @FormParam(API_FILE_REPOS_PARAM) String repoList) {
    checkRequest(null == fileIds,
        "Form field, '%s', is missing in the POST payload.", API_FILE_IDS_PARAM);
    checkRequest(fileIds.isEmpty(),
        "Form field, '%s', must contain a list of repository file IDs.", API_FILE_IDS_PARAM);

    val filter = buildFileIdListFilterParam(fileIds);
    return generateManifestArchiveByFilters(new FiltersParam(filter),
        null == repoList ? "" : repoList);
  }

  @NonNull
  private static Query toQuery(FiltersParam filters) {
    return query()
        .filters(filters.get())
        .build();
  }

  @NonNull
  private static String buildManifestFileName(Date timestamp) {
    return "manifest." + timestamp.getTime() + ".tar.gz";
  }

  private static String buildFileIdListFilterParam(@NonNull List<String> fileIds) {
    val nodeFactory = new JsonNodeFactory(false);
    val root = nodeFactory.objectNode();

    // Build an ObjectNode to represent this filter: {"file": "id": {"is": ["id1", "id2", ...]}
    val file = nodeFactory.objectNode();
    root.put("file", file);

    val id = nodeFactory.objectNode();
    file.put("id", id);

    val idArray = nodeFactory.arrayNode();
    fileIds.stream().forEach(fileId -> idArray.add(fileId));
    id.put("is", idArray);

    return root.toString();
  }

}
