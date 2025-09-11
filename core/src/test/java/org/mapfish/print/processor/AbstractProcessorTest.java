package org.mapfish.print.processor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.Nullable;
import org.junit.Test;
import org.mapfish.print.config.Configuration;

public class AbstractProcessorTest {

  /**
   * This test checks that all the inputMapper mappings have an associated property in the params
   * object.
   */
  @Test
  public void testExtraInputMapperMapping() {
    final TestProcessor testProcessor = new TestProcessor();
    testProcessor.getInputMapperBiMap().put("pqr", "prop");

    Configuration configuration = new Configuration();
    List<Throwable> errors = new ArrayList<>();
    testProcessor.validate(errors, configuration);

    assertTrue(errors.isEmpty());
    testProcessor.getInputMapperBiMap().put("ml", "proc");
    testProcessor.validate(errors, configuration);
    assertFalse(errors.isEmpty());
  }

  /**
   * This test checks that all the outputMapper mappings have an associated property in the output
   * object.
   */
  @Test
  public void testExtraOutputMapperMapping() {
    final TestProcessor testProcessor = new TestProcessor();
    testProcessor.getOutputMapperBiMap().put("prop", "oq");

    Configuration configuration = new Configuration();
    List<Throwable> errors = new ArrayList<>();
    testProcessor.validate(errors, configuration);

    assertTrue(errors.isEmpty());
    testProcessor.getInputMapperBiMap().put("proc", "mk");
    testProcessor.validate(errors, configuration);
    assertFalse(errors.isEmpty());
  }

  class TestIn {
    public String prop;
  }

  class TestOut {
    public String prop;
  }

  class TestProcessor extends AbstractProcessor<TestIn, TestOut> {

    protected TestProcessor() {
      super(TestOut.class);
    }

    @Override
    public TestIn createInputParameter() {
      return new TestIn();
    }

    @Nullable
    @Override
    public TestOut execute(TestIn values, ExecutionContext context) {
      return new TestOut();
    }

    @Override
    protected void extraValidation(
        List<Throwable> validationErrors, final Configuration configuration) {
      // no checks
    }
  }
}
