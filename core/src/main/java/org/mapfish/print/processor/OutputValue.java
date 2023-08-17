package org.mapfish.print.processor;

import java.lang.reflect.Field;

final class OutputValue extends InputValue {
  public final boolean canBeRenamed;

  OutputValue(final String name, final boolean canBeRenamed, final Field field) {
    super(name, field);
    this.canBeRenamed = canBeRenamed;
  }

  @Override
  public int hashCode() {
    // Would be nice to justify why faking being superclass
    return super.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    // Would be nice to justify why faking being superclass
    return super.equals(obj);
  }

  @Override
  public String toString() {
    // Would be nice to justify why faking being superclass
    return super.toString();
  }
}
