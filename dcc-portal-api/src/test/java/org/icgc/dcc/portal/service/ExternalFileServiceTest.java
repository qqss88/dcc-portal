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

import static org.assertj.core.api.Assertions.assertThat;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import lombok.Cleanup;
import lombok.val;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.icgc.dcc.portal.model.ExternalFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.supercsv.io.CsvListReader;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

/**
 * Test suite for ExternalFileService
 */
public class ExternalFileServiceTest {

  private static final String MANIFEST_FILE_DIRECTORY = "./";
  private static final String MANIFEST_FILE_NAME_SUFFIX = ".tsv";

  private List<ExternalFile> testFiles;
  private Multimap<String, SimpleImmutableEntry<String, List<String>>> expectedFileContents;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() {
    val info1 = ImmutableMap.<String, Object> builder()
        .put("repository.repo_entity_id", "id1")
        .put("repository.repo_server.repo_name", Arrays.asList("repo1", "repo2"))
        .put("repository.file_name", "file1")
        .put("repository.file_size", 101000)
        .put("md5", "md5-1")
        .put("url", "url-1")
        .build();

    val info2 = ImmutableMap.<String, Object> builder()
        .put("repository.repo_entity_id", "id2")
        .put("repository.repo_server.repo_name", Arrays.asList("repo2"))
        .put("repository.file_name", "file2")
        .put("repository.file_size", 102000)
        .put("md5", "md5-2")
        .put("url", "url-2")
        .build();

    val info3 = ImmutableMap.<String, Object> builder()
        .put("repository.repo_entity_id", "id3")
        .put("repository.repo_server.repo_name", Arrays.asList("repo2", "repo3"))
        .put("repository.file_name", "file3")
        .put("repository.file_size", 103000)
        .put("md5", "md5-3")
        .put("url", "url-3")
        .build();

    testFiles = Lists.transform(Arrays.asList(info1, info2, info3),
        map -> new ExternalFile(map));

    val allRows = FluentIterable.from(testFiles)
        .transformAndConcat(file -> ExternalFileService.expandRows(file));
    expectedFileContents = Multimaps.index(allRows, row -> row.getKey());
  }

  @Test
  public void test() throws IOException {
    val file = temp.newFile();

    // Generate the test archive file by streaming the manifest content into a file.
    try (val outputStream = new FileOutputStream(file)) {
      ExternalFileService.generateManifestArchive(testFiles, outputStream);
    }

    @Cleanup
    val inputStream = new FileInputStream(file);
    @Cleanup
    val tar = new TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(inputStream)));

    val fileNameSet = Sets.newHashSet(FluentIterable.from(testFiles)
        .transformAndConcat(fileInfo -> fileInfo.getRepositoryNames()));
    val expectedFileNames = Sets.newHashSet(Iterables.transform(fileNameSet,
        fileName -> MANIFEST_FILE_DIRECTORY + fileName + MANIFEST_FILE_NAME_SUFFIX));

    val testFileNames = Sets.<String> newHashSet();
    ArchiveEntry entry;

    // Read the generated archive and compare the file content directly.
    while ((entry = tar.getNextEntry()) != null) {

      if (entry.isDirectory()) {
        continue;
      }

      val fileName = entry.getName();
      testFileNames.add(fileName);

      val tempFile = temp.newFile();
      writeTempFile(tempFile, tar);

      @Cleanup
      val reader = new CsvListReader(new FileReader(tempFile), TAB_PREFERENCE);

      val testFileContent = Lists.<List<String>> newArrayList();
      List<String> line;

      while ((line = reader.read()) != null) {
        testFileContent.add(line);
      }

      val fileKey =
          fileName.substring(MANIFEST_FILE_DIRECTORY.length(), fileName.length() - MANIFEST_FILE_NAME_SUFFIX.length());
      val expectedFileContent =
          Lists.newArrayList(Iterables.transform(expectedFileContents.get(fileKey), map -> map.getValue()));

      assertThat(testFileContent).isEqualTo(expectedFileContent);
    }

    assertThat(testFileNames).isEqualTo(expectedFileNames);
  }

  private void writeTempFile(File tempFile, TarArchiveInputStream source) throws IOException {
    val bufferSize = 1024;
    byte[] buffer = new byte[bufferSize];

    @Cleanup
    val target = new BufferedOutputStream(new FileOutputStream(tempFile), bufferSize);

    int count = 0;

    while ((count = source.read(buffer, 0, bufferSize)) != -1) {
      target.write(buffer, 0, count);
    }

  }

}
