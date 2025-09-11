package org.mapfish.print.processor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import jakarta.annotation.Nullable;
import org.junit.Test;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.HasDefaultValue;

public class ProcessorGraphNodeTest {
  final String iMappingName = "integer";
  final String bMappingName = "bool";
  Integer intVal = 1;
  String sVal = "sValue";
  List<String> lsVal = Arrays.asList("one", "two");
  double[] daVal = new double[] {1.2, 2.3};

  @Test
  public void testPopulateInputParameter() {

    Values values = new Values();
    values.put(iMappingName, intVal);
    values.put(bMappingName, true);
    values.put("s", sVal);
    values.put("ls", lsVal);
    values.put("da", daVal);

    TestProcessor processor = new TestProcessor();
    processor.getInputMapperBiMap().put(iMappingName, "i");
    processor.getInputMapperBiMap().put(bMappingName, "b");
    DataTransferObject param = ProcessorUtils.populateInputParameter(processor, values);

    assertEquals(sVal, param.s);
    assertEquals(intVal.intValue(), param.i);
    assertTrue(param.b);
    assertEquals(new DataTransferObject().defaultI, param.defaultI);
    assertEquals(lsVal, param.ls);
    assertArrayEquals(daVal, param.da, 0.00001);
  }

  @Test(expected = RuntimeException.class)
  public void testNullableProperty() {

    Values values = new Values();
    values.put(iMappingName, intVal);
    values.put(bMappingName, true);
    values.put("s", sVal);
    values.put("ls", lsVal);
    // NO da value is specified so an exception should be thrown.

    TestProcessor processor = new TestProcessor();
    processor.getInputMapperBiMap().put(iMappingName, "i");
    processor.getInputMapperBiMap().put(bMappingName, "b");
    ProcessorUtils.populateInputParameter(processor, values);
  }

  @Test
  public void testWriteProcessorOutputToValues() {
    Values values = new Values();

    final DataTransferObject dto = new DataTransferObject();
    dto.b = true;
    dto.da = daVal;
    dto.defaultI = 32;
    dto.ls = lsVal;

    TestProcessor processor = new TestProcessor();
    processor.getOutputMapperBiMap().put("i", iMappingName);
    processor.getOutputMapperBiMap().put("b", bMappingName);

    ProcessorUtils.writeProcessorOutputToValues(dto, processor, values);

    assertEquals(dto.defaultI, values.getInteger("defaultI").intValue());
    assertEquals(dto.i, values.getInteger(iMappingName).intValue());
    assertEquals(dto.b, values.getBoolean(bMappingName));
    assertNull(values.getBoolean("s"));
    assertEquals(lsVal, values.getObject("ls", Object.class));
    assertArrayEquals(daVal, (double[]) values.getObject("da", Object.class), 0.00001);
  }

  @Test
  public void testWritePrefixedOutputToValues() {
    Values values = new Values();

    final DataTransferObject dto = new DataTransferObject();
    dto.b = true;
    dto.da = daVal;
    dto.defaultI = 32;
    dto.ls = lsVal;

    TestProcessor processor = new TestProcessor();
    processor.getOutputMapperBiMap().put("i", iMappingName);
    processor.getOutputMapperBiMap().put("b", bMappingName);

    processor.setOutputPrefix("   prefix   ");
    ProcessorUtils.writeProcessorOutputToValues(dto, processor, values);

    assertEquals(dto.defaultI, values.getInteger("prefixDefaultI").intValue());
    assertEquals(dto.i, values.getInteger(iMappingName).intValue());
    assertEquals(dto.b, values.getBoolean(bMappingName));
    assertNull(values.getBoolean("prefixS"));
    assertEquals(lsVal, values.getObject("prefixLs", Object.class));
    assertArrayEquals(daVal, (double[]) values.getObject("prefixDa", Object.class), 0.00001);
  }

  static class DataTransferObject {
    @HasDefaultValue public int defaultI = 3;
    public int i;
    public boolean b;
    public String s;
    public List<String> ls;
    public double[] da;
  }

  static class TestProcessor extends AbstractProcessor<DataTransferObject, DataTransferObject> {

    /** Constructor. */
    protected TestProcessor() {
      super(DataTransferObject.class);
    }

    @Override
    public DataTransferObject createInputParameter() {
      return new DataTransferObject();
    }

    @Nullable
    @Override
    public DataTransferObject execute(DataTransferObject values, ExecutionContext context) {
      return null;
    }

    @Override
    protected void extraValidation(
        List<Throwable> validationErrors, final Configuration configuration) {
      // no checks
    }
  }
}
