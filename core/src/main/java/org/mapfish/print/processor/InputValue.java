package org.mapfish.print.processor;

import java.lang.reflect.Field;
import java.util.Objects;

class InputValue {
  public final String name;
  public final String internalName;
  public final Class<?> type;
  public final Field field;

  InputValue(final String name, final Field field) {
    this.name = name;
    this.internalName = field.getName();
    this.type = field.getType();
    this.field = field;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.name);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final InputValue that = (InputValue) obj;
    return Objects.equals(name, that.name);
  }

  @Override
  public String toString() {
    return "InputValue{" + "name='" + this.name + "', " + "type=" + this.type.getSimpleName() + '}';
  }
}
