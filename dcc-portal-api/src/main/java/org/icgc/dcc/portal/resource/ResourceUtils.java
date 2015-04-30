package org.icgc.dcc.portal.resource;

import static java.lang.String.format;
import static org.icgc.dcc.common.core.util.Joiners.COMMA;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.validation.Validation;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.Query.QueryBuilder;
import org.icgc.dcc.portal.service.BadRequestException;
import org.icgc.dcc.portal.util.JsonUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

public class ResourceUtils {

  static final Set<String> ORDER_VALUES = ImmutableSet.of("asc", "desc");

  static final String DEFAULT_FILTERS = "{}";
  static final String DEFAULT_SIZE = "10";
  static final String DEFAULT_FROM = "1";
  static final String DEFAULT_ORDER = "desc";
  static final String DEFAULT_PROJECT_SORT = "totalDonorCount";
  static final String DEFAULT_DONOR_SORT = "ssmAffectedGenes";
  public static final String DEFAULT_GENE_MUTATION_SORT = "affectedDonorCountFiltered";
  static final String DEFAULT_OCCURRENCE_SORT = "donorId";

  static final String COUNT_TEMPLATE = "Request for a count of {} with filters '{}'";
  static final String FIND_ALL_TEMPLATE =
      "Request for '{}' {} from index '{}', sorted by '{}' in '{}' order with filters '{}'";
  static final String FIND_ONE_TEMPLATE = "Request for '{}'";
  static final String NESTED_FIND_TEMPLATE = "Request {} for '{}'";
  static final String NESTED_COUNT_TEMPLATE = "Request {} count for '{}'";
  static final String NESTED_NESTED_COUNT_TEMPLATE = "Request count of '{}' in '{}' affected by '{}'";

  static final String RETURNS_LIST = "Returns a list of ";
  static final String RETURNS_COUNT = "Returns a count of ";
  static final String GROUPED_BY = " grouped by ";
  static final String DONOR = "Donor";
  static final String MUTATION = "Mutation";
  static final String TOTAL = "Total";
  static final String GENE = "Gene";
  static final String PROJECT = "Project";
  static final String OCCURRENCE = "Occurrence";
  static final String S = "(s)";
  static final String AFFECTED_BY_THE = " affected by the ";
  static final String FOR_THE = " for the ";
  static final String FIND_BY_ID_ERROR =
      "If it does not exist with the specified Identifiable an error will be returned";
  static final String FIND_BY_ID = "Find by Identifiable";
  static final String NOT_FOUND = " not found";
  static final String MULTIPLE_IDS = ". Multiple IDs can be separated by a comma";

  static final String API_DONOR_VALUE = "Donor ID";
  static final String API_DONOR_PARAM = "donorId";
  static final String API_MUTATION_VALUE = "Mutation ID";
  static final String API_MUTATION_PARAM = "mutationId";
  static final String API_GENE_VALUE = "Gene ID";
  static final String API_GENE_PARAM = "geneId";
  static final String API_GENE_SET_PARAM = "geneSetId";
  static final String API_GENE_SET_VALUE = "Gene Set ID";
  static final String API_PROJECT_VALUE = "Project ID";
  static final String API_PROJECT_PARAM = "projectId";
  static final String API_OCCURRENCE_VALUE = "Occurrence ID";
  static final String API_OCCURRENCE_PARAM = "occurrenceId";
  static final String API_ORDER_ALLOW = "asc,desc";
  static final String API_ORDER_PARAM = "order";
  static final String API_ORDER_VALUE = "Order to sort the column";
  static final String API_SORT_FIELD = "sort";
  static final String API_SORT_VALUE = "Column to sort results on";
  static final String API_SIZE_ALLOW = "range[1,100]";
  static final String API_SIZE_PARAM = "size";
  static final String API_SIZE_VALUE = "Number of results returned";
  static final String API_FROM_PARAM = "from";
  static final String API_FROM_VALUE = "Start index of results";
  static final String API_INCLUDE_PARAM = "include";
  static final String API_INCLUDE_VALUE = "Include addition data in the response";
  static final String API_FIELD_PARAM = "field";
  static final String API_FIELD_VALUE = "Select fields returned";
  static final String API_FILTER_PARAM = "filters";
  static final String API_FILTER_VALUE = "Filter the search results";
  static final String API_SCORE_FILTERS_PARAM = "scoreFilters";
  static final String API_SCORE_FILTER_VALUE = "Used to filter scoring differently from results";
  static final String API_ANALYSIS_VALUE = "Analysis";
  static final String API_ANALYSIS_PARAM = "analysis";
  static final String API_ANALYSIS_ID_VALUE = "Analysis ID";
  static final String API_ANALYSIS_ID_PARAM = "analysisId";
  static final String API_PARAMS_VALUE = "EnrichmentParams";
  static final String API_PARAMS_PARAM = "params";

  static final String API_ENTITY_LIST_ID_VALUE = "Entity Set ID";
  static final String API_ENTITY_LIST_ID_PARAM = "entitySetId";

  static final String API_ENTITY_LIST_DEFINITION_VALUE = "Entity Set Definition";
  static final String API_ENTITY_LIST_DEFINITION_PARAM = "entityListDefinition";
  static final String API_SET_ANALYSIS_DEFINITION_VALUE = "Set Analysis Definition";

  static LinkedHashMap<String, Query> generateQueries(ObjectNode filters, String filterTemplate, List<String> ids) {
    val queries = Maps.<String, Query> newLinkedHashMap();

    for (String id : ids) {
      val filter = mergeFilters(filters, filterTemplate, id);
      queries.put(id, query().filters(filter).build());
    }
    return queries;
  }

  static LinkedHashMap<String, Query> generateQueries(ObjectNode filters, String filterTemplate, List<String> ids,
      String anchorId) {
    val queries = Maps.<String, Query> newLinkedHashMap();

    for (String id : ids) {
      val filter = mergeFilters(filters, filterTemplate, id, anchorId);
      queries.put(id, query().filters(filter).build());
    }
    return queries;
  }

  static LinkedHashMap<String, LinkedHashMap<String, Query>> generateQueries(ObjectNode filters, String filterTemplate,
      List<String> ids,
      List<String> anchorIds) {
    val queries = Maps.<String, LinkedHashMap<String, Query>> newLinkedHashMap();

    for (String anchorId : anchorIds) {
      queries.put(anchorId, generateQueries(filters, filterTemplate, ids, anchorId));
    }
    return queries;
  }

  static ObjectNode mergeFilters(ObjectNode filters, String template, Object... objects) {
    return JsonUtils.merge(filters, (new FiltersParam(String.format(template, objects)).get()));
  }

  static QueryBuilder query() {
    return Query.builder();
  }

  /**
   * @see http://stackoverflow.com/questions/23704616/how-to-validate-a-single-parameter-in-dropwizard
   */
  static void validate(@NonNull Object object) {
    val errorMessages = new ArrayList<String>();
    val validator = Validation.buildDefaultValidatorFactory().getValidator();

    val violations = validator.validate(object);
    if (!violations.isEmpty()) {
      for (val violation : violations) {
        errorMessages.add("'" + violation.getPropertyPath() + "' " + violation.getMessage());
      }

      throw new BadRequestException(COMMA.join(errorMessages));
    }

  }

  /**
   * @param condition True to throw a bad request exception
   */
  static void checkRequest(boolean condition, String message, Object... args) {
    if (condition) {
      throw new BadRequestException(format(message, args));
    }
  }

}
