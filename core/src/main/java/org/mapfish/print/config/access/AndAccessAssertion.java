package org.mapfish.print.config.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.annotation.Nonnull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.config.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

/** An access assertion that throws fails if any of the encapsulated assertions fail. */
public final class AndAccessAssertion implements AccessAssertion {
  private static final String JSON_ARRAY = "data";
  private List<AccessAssertion> predicates;

  @Autowired private AccessAssertionPersister persister;

  /**
   * Set all the Predicates/AccessAssertion that have to all pass in order for this assertion to
   * pass.
   *
   * <p>An exception is thrown if this method is called more than once.
   *
   * @param predicates the Predicates/AccessAssertion
   */
  public void setPredicates(@Nonnull final AccessAssertion... predicates) {
    if (this.predicates != null) {
      throw new AssertionError("Predicates can only be set a single time");
    }
    if (predicates.length < 1) {
      throw new IllegalArgumentException("There must be at least 1 predicate");
    }
    this.predicates = Arrays.asList(predicates);
  }

  @Override
  public void assertAccess(final String resourceDescription, final Object protectedResource) {
    for (AccessAssertion predicate : this.predicates) {
      predicate.assertAccess(resourceDescription, protectedResource);
    }
  }

  @Override
  public JSONObject marshal() {
    try {
      JSONObject marshalData = new JSONObject();
      JSONArray array = new JSONArray();
      marshalData.put(JSON_ARRAY, array);

      if (this.predicates != null) {
        for (AccessAssertion predicate : this.predicates) {
          final JSONObject predicateMarshalData = this.persister.marshal(predicate);
          array.put(predicateMarshalData);
        }
      }
      return marshalData;
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void unmarshal(final JSONObject encodedAssertion) {
    try {
      this.predicates = new ArrayList<>();

      JSONArray marshalData = encodedAssertion.getJSONArray(JSON_ARRAY);
      for (int i = 0; i < marshalData.length(); i++) {
        JSONObject predicateData = marshalData.getJSONObject(i);
        final AccessAssertion predicate = this.persister.unmarshal(predicateData);
        this.predicates.add(predicate);
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
    for (AccessAssertion predicate : this.predicates) {
      predicate.validate(validationErrors, configuration);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof AndAccessAssertion) {
      return ((AndAccessAssertion) o).predicates.equals(this.predicates);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.predicates.hashCode();
  }

  @Override
  public AccessAssertion copy() {
    AndAccessAssertion assertion = new AndAccessAssertion();
    assertion.predicates = new ArrayList<>(this.predicates);
    assertion.persister = this.persister;
    return assertion;
  }
}
