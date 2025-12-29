package org.mapfish.print.attribute;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.util.AssertionFailedException;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.mapfish.print.wrapper.json.PJsonObject;

public class ReflectiveAttributeTest {
  @Test
  public void testPJsonObjIllegal() {
    assertThrows(
        AssertionFailedException.class,
        () -> new TestReflectiveAtt(PJsonObjParamIllegal.class).init());
  }

  @Test
  public void testPJsonArrayIllegal() {
    assertThrows(
        AssertionFailedException.class,
        () -> new TestReflectiveAtt(PJsonArrayParamIllegal.class).init());
  }

  @Test
  public void testJsonObjIllegal() {
    assertThrows(
        AssertionFailedException.class,
        () -> new TestReflectiveAtt(JsonObjParamIllegal.class).init());
  }

  @Test
  public void testJsonArrayIllegal() {
    assertThrows(
        AssertionFailedException.class,
        () -> new TestReflectiveAtt(JsonArrayParamIllegal.class).init());
  }

  @Test
  public void testEmpty() {
    assertThrows(AssertionFailedException.class, () -> new TestReflectiveAtt(Empty.class).init());
  }

  private static class TestReflectiveAtt extends ReflectiveAttribute<Object> {

    private final Class<?> type;

    private TestReflectiveAtt(Class<?> type) {
      this.type = type;
    }

    @Override
    public Class<?> getValueType() {
      return type;
    }

    @Override
    public Object createValue(Template template) {
      return null;
    }

    @Override
    public void validate(List<Throwable> validationErrors, final Configuration configuration) {}
  }

  static class PJsonObjParamIllegal {
    public PJsonObject p;
  }

  static class JsonObjParamIllegal {
    public JSONObject p;
  }

  static class PJsonArrayParamIllegal {
    public PJsonArray p;
  }

  static class JsonArrayParamIllegal {
    public JSONArray p;
  }

  static class Empty {}
}
