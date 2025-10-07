package org.mapfish.print.wrapper;

/** PArray that is consists of several PArray objects merged together. */
public final class PJoinedArray implements PArray {
  private final PArray[] arrays;
  private final int combinedSize;

  /**
   * Constructor.
   *
   * @param arrays the arrays that constitute the joined array.
   */
  public PJoinedArray(final PArray[] arrays) {
    this.arrays = arrays;
    int size = 0;
    for (PArray array : arrays) {
      size += array.size();
    }

    this.combinedSize = size;
  }

  @Override
  public int size() {
    return this.combinedSize;
  }

  @Override
  public PObject getObject(final int i) {
    int index = i;
    for (PArray array : this.arrays) {
      if (index < array.size()) {
        return array.getObject(index);
      } else {
        index -= array.size();
      }
    }
    return null;
  }

  @Override
  public PArray getArray(final int i) {
    int index = i;
    for (PArray array : this.arrays) {
      if (index < array.size()) {
        return array.getArray(index);
      } else {
        index -= array.size();
      }
    }
    return null;
  }

  @Override
  public int getInt(final int i) {
    Object o = getObjectAt(i);
    return (Integer) o;
  }

  @Override
  public long getLong(final int i) {
    Object o = getObjectAt(i);
    return (Long) o;
  }

  @Override
  public float getFloat(final int i) {
    Object o = getObjectAt(i);
    return (Float) o;
  }

  @Override
  public double getDouble(final int i) {
    Object o = getObjectAt(i);
    return (Double) o;
  }

  @Override
  public String getString(final int i) {
    return getObjectAt(i).toString();
  }

  @Override
  public boolean getBool(final int i) {
    Object o = getObjectAt(i);
    return (Boolean) o;
  }

  /**
   * Method avoiding uncontrolled {@link NullPointerException}.
   *
   * @param index in the array
   * @return the object at this index.
   * @throws IndexOutOfBoundsException if the object returned is null.
   */
  private Object getObjectAt(final int index) throws IndexOutOfBoundsException {
    Object o = get(index);
    if (o == null) {
      throw new IndexOutOfBoundsException("No value found at index:" + index);
    }
    return o;
  }

  @Override
  public String getPath(final String key) {
    StringBuilder builder = new StringBuilder();
    for (PArray array : this.arrays) {
      if (builder.isEmpty()) {
        builder.append(" + ");
      }
      builder.append(array.getPath(key));
    }
    return "Merged: " + builder;
  }

  @Override
  public String getCurrentPath() {
    StringBuilder builder = new StringBuilder();
    for (PArray array : this.arrays) {
      if (builder.isEmpty()) {
        builder.append(" + ");
      }
      builder.append(array.getCurrentPath());
    }
    return "Merged: " + builder;
  }

  @Override
  public Object get(final int i) {
    int index = i;
    for (PArray array : this.arrays) {
      if (index < array.size()) {
        return array.get(index);
      } else {
        index -= array.size();
      }
    }
    return null;
  }
}
