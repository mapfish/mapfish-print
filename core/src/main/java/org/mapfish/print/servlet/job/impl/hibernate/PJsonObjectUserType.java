package org.mapfish.print.servlet.job.impl.hibernate;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.wrapper.json.PJsonObject;

/** Hibernate User Type for PJson object. */
public class PJsonObjectUserType implements UserType<PJsonObject> {

  private static final int SQL_TYPE = Types.LONGVARCHAR;

  private static final String CONTEXT_NAME = "spec";

  @Override
  public final PJsonObject assemble(final Serializable cached, final Object owner) {
    return deepCopy((PJsonObject) cached);
  }

  @Override
  public final PJsonObject deepCopy(final PJsonObject value) {
    if (value == null) {
      return null;
    } else {
      try {
        return new PJsonObject(new JSONObject(value.getInternalObj().toString()), CONTEXT_NAME);
      } catch (JSONException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public final Serializable disassemble(final PJsonObject value) {
    return (Serializable) deepCopy(value);
  }

  @Override
  public final boolean equals(final PJsonObject x, final PJsonObject y) {
    if (x == null) {
      return y == null;
    } else {
      return x.equals(y);
    }
  }

  @Override
  public final int hashCode(final PJsonObject x) {
    return x.hashCode();
  }

  @Override
  public final boolean isMutable() {
    return false;
  }

  @Override
  public final PJsonObject nullSafeGet(
      final ResultSet rs,
      final int position,
      final SharedSessionContractImplementor session,
      final Object owner)
      throws SQLException {
    String value = rs.getString(position);
    if (value != null) {
      try {
        return new PJsonObject(new JSONObject(value), CONTEXT_NAME);
      } catch (JSONException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  @Override
  public final void nullSafeSet(
      final PreparedStatement st,
      final PJsonObject value,
      final int index,
      final SharedSessionContractImplementor session)
      throws SQLException {
    if (value == null) {
      st.setNull(index, SQL_TYPE);
    } else {
      st.setString(index, value.getInternalObj().toString());
    }
  }

  @Override
  public final PJsonObject replace(
      final PJsonObject original, final PJsonObject target, final Object owner) {
    return deepCopy(original);
  }

  @Override
  public final Class<PJsonObject> returnedClass() {
    return PJsonObject.class;
  }

  @Override
  public final int getSqlType() {
    return SQL_TYPE;
  }
}
