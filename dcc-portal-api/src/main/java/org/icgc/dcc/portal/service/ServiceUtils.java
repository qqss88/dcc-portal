package org.icgc.dcc.portal.service;

import java.util.Iterator;
import java.util.LinkedHashMap;

import lombok.val;

import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.icgc.dcc.portal.model.Query;

import com.google.common.collect.Maps;

public class ServiceUtils {

  protected static final LinkedHashMap<String, Long> buildCounts(LinkedHashMap<String, Query> queries,
      MultiSearchResponse sr) {
    val counts = Maps.<String, Long> newLinkedHashMap();
    val ids = queries.keySet().iterator();
    for (val item : sr.getResponses()) {
      SearchResponse r = item.getResponse();
      counts.put(ids.next(), r.getHits().getTotalHits());
    }

    return counts;
  }

  protected static final LinkedHashMap<String, LinkedHashMap<String, Long>> buildNestedCounts(
      LinkedHashMap<String, LinkedHashMap<String, Query>> queries,
      MultiSearchResponse sr) {
    val counts = Maps.<String, LinkedHashMap<String, Long>> newLinkedHashMap();

    val idSet = queries.keySet();
    val firstId = idSet.iterator().next();
    val subIdSet = queries.get(firstId).keySet();

    val ids = idSet.iterator();
    Iterator<String> subIds = subIdSet.iterator();
    LinkedHashMap<String, Long> subCounts = Maps.<String, Long> newLinkedHashMap();

    for (val item : sr.getResponses()) {
      SearchResponse r = item.getResponse();
      if (!subIds.hasNext()) {
        counts.put(ids.next(), subCounts);
        subIds = subIdSet.iterator();
        subCounts = Maps.<String, Long> newLinkedHashMap();
      }
      subCounts.put(subIds.next(), r.getHits().getTotalHits());
    }
    // catch last set of subCounts
    counts.put(ids.next(), subCounts);

    return counts;
  }
}
