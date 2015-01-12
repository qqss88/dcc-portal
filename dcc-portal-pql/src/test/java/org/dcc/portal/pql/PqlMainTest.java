package org.dcc.portal.pql;

import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;

import org.junit.Test;

public class PqlMainTest {

  QueryProcessor processor = new QueryProcessor();

  @Test
  public void equalTest() {
    val query = "eq(x, 1)";
    val result = processor.process(query);

    assertThat(result).isNotNull();
  }

  @Test
  public void selectTest() {
    val query = "select(transcript, occurrence)&and(eq(id,10),ne(id,20))";
    val result = processor.process(query);

    assertThat(result.getFields()).containsExactly("transcript", "occurrence");
  }

  @Test
  public void filterTest() {
    val query = "and(eq(id,10),ne(id,20))";
    val result = processor.process(query);

    assertThat(result).isNotNull();
  }

}
