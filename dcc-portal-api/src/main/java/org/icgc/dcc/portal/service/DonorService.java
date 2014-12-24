package org.icgc.dcc.portal.service;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.sort;
import static org.icgc.dcc.portal.service.ServiceUtils.buildCounts;
import static org.icgc.dcc.portal.service.ServiceUtils.buildNestedCounts;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.icgc.dcc.portal.model.Donor;
import org.icgc.dcc.portal.model.Donors;
import org.icgc.dcc.portal.model.Pagination;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.repository.DonorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.supercsv.io.CsvMapWriter;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

@Service
@RequiredArgsConstructor(onConstructor = @_({ @Autowired }))
public class DonorService {

  private final DonorRepository donorRepository;

  private Donors buildDonors(SearchResponse response, Query query) {
    val hits = response.getHits();

    boolean includeScore = !query.hasFields() || query.getFields().contains("ssmAffectedGenes");

    val list = ImmutableList.<Donor> builder();

    for (val hit : hits) {
      val fieldMap = Maps.<String, Object> newHashMap();
      for (val field : hit.getFields().entrySet()) {
        fieldMap.put(field.getKey(), field.getValue().getValues());
      }
      if (includeScore) fieldMap.put("_score", hit.getScore());
      list.add(new Donor(fieldMap));
    }

    Donors donors = new Donors(list.build());
    donors.setFacets(response.getFacets());
    donors.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return donors;
  }

  public Donors findAllCentric(Query query) {
    return buildDonors(donorRepository.findAllCentric(query), query);
  }

  public Donors findAll(Query query) {
    return buildDonors(donorRepository.findAll(query), query);
  }

  public long count(Query query) {
    return donorRepository.count(query);
  }

  public LinkedHashMap<String, Long> counts(LinkedHashMap<String, Query> queries) {
    MultiSearchResponse sr = donorRepository.counts(queries);

    return buildCounts(queries, sr);
  }

  public LinkedHashMap<String, LinkedHashMap<String, Long>> nestedCounts(
      LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    MultiSearchResponse sr = donorRepository.nestedCounts(queries);

    return buildNestedCounts(queries, sr);
  }

  public Donor findOne(String donorId, Query query) {
    return new Donor(donorRepository.findOne(donorId, query));
  }

  public Set<String> findIds(Query query) {
    return donorRepository.findIds(query);
  }

  public List<Map<String, Object>> getSamples(List<Donor> donors) {
    val records = Lists.<Map<String, Object>> newArrayList();
    for (val donor : donors) {
      val dRecord = Maps.<String, Object> newHashMap();
      dRecord.put("icgc_donor_id", donor.getId());
      dRecord.put("submitted_donor_id", donor.getSubmittedDonorId());
      dRecord.put("project_code", donor.getProjectId());
      if (donor.getSpecimen() != null) {
        for (val specimen : donor.getSpecimen()) {
          val spRecord = Maps.<String, Object> newHashMap();
          spRecord.putAll(dRecord);
          spRecord.put("icgc_specimen_id", specimen.getId());
          spRecord.put("submitted_specimen_id", specimen.getSubmittedId());
          spRecord.put("specimen_type", specimen.getType());
          spRecord.put("specimen_type_other", specimen.getTypeOther());
          if (specimen.getSamples() != null) {
            for (val sample : specimen.getSamples()) {
              val saRecord = Maps.<String, Object> newHashMap();
              saRecord.putAll(spRecord);
              saRecord.put("icgc_sample_id", sample.getId());
              saRecord.put("submitted_sample_id", sample.getAnalyzedId());
              saRecord.put("analyzed_sample_interval", sample.getAnalyzedInterval());
              saRecord.put("study", sample.getStudy());
              if (sample.getAvailableRawSequenceData() != null) {
                for (val external : sample.getAvailableRawSequenceData()) {
                  val eRecord = Maps.<String, Object> newHashMap();
                  eRecord.putAll(saRecord);
                  // eRecord.put("raw_sequence_repository", external.getRepository());
                  eRecord.put("repository", external.getRepository());
                  eRecord.put("library_strategy", external.getLibraryStrategy());
                  eRecord.put("analysis_data_uri", external.getDataUri());
                  eRecord.put("raw_data_accession", external.getRawDataAccession());
                  records.add(eRecord);
                }
              } else
                records.add(saRecord);
            }
          } else
            records.add(spRecord);
        }
      } else
        records.add(dRecord);
    }
    return sortSamples(records);
  }

  public String sampleFilename(String projectId) {
    return format("sample.%s.%s.tsv", projectId, currentTimeMillis());
  }

  public StreamingOutput asSampleStream(final List<Map<String, Object>> samples) {
    return new StreamingOutput() {

      @Override
      public void write(OutputStream os) throws IOException, WebApplicationException {

        @Cleanup
        val writer =
            new CsvMapWriter(new BufferedWriter(new OutputStreamWriter(os)), TAB_PREFERENCE);

        final String[] headers = {
            "icgc_sample_id",
            "submitted_sample_id",
            "icgc_specimen_id",
            "submitted_specimen_id",
            "icgc_donor_id",
            "submitted_donor_id",
            "project_code",

            "specimen_type",
            "specimen_type_other",
            "analyzed_sample_interval",
            "repository",
            "library_strategy",
            "raw_data_accession",
            "study"
        };

        // Write TSV
        writer.writeHeader(headers);
        for (val sample : samples) {
          writer.write(sample, headers);
        }

        writer.flush();
      }

    };
  }

  private List<Map<String, Object>> sortSamples(List<Map<String, Object>> records) {
    // Multi-key sort
    sort(records, new Comparator<Map<String, Object>>() {

      @Override
      public int compare(Map<String, Object> r1, Map<String, Object> r2) {
        Ordering<Comparable<?>> ordering = Ordering.natural();

        return ComparisonChain
            .start()
            // Column 1
            .compare((String) r1.get("icgc_donor_id"), (String) r2.get("icgc_donor_id"), ordering)
            // Column 2
            .compare((String) r1.get("icgc_specimen_id"), (String) r2.get("icgc_specimen_id"), ordering)
            // Column 3
            .compare((String) r1.get("icgc_sample_id"), (String) r2.get("icgc_sample_id"), ordering)
            .result();
      }
    });

    return records;
  }
}
