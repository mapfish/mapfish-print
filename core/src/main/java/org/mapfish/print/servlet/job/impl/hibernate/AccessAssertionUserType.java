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
import org.mapfish.print.ApplicationContextProvider;
import org.mapfish.print.config.access.AccessAssertion;
import org.mapfish.print.config.access.AccessAssertionPersister;

/** Hibernate user type for access assertion. */
public class AccessAssertionUserType implements UserType<AccessAssertion> {

  private static final int SQL_TYPE = Types.LONGVARCHAR;

  @Override
  public final AccessAssertion assemble(final Serializable cached, final Object owner) {
    return deepCopy((AccessAssertion) cached);
  }

  @Override
  public final AccessAssertion deepCopy(final AccessAssertion value) {
    if (value == null) {
      return null;
    }
    return value.copy();
  }

  @Override
  public final Serializable disassemble(final AccessAssertion value) {
    return (Serializable) deepCopy(value);
  }

  @Override
  public final boolean equals(final AccessAssertion x, final AccessAssertion y) {
    if (x == null) {
      return y == null;
    } else {
      return x.equals(y);
    }
  }

  @Override
  public final int hashCode(final AccessAssertion x) {
    return x.hashCode();
  }

  @Override
  public final boolean isMutable() {
    return false;
  }

  @Override
  public final AccessAssertion nullSafeGet(
      final ResultSet rs,
      final int position,
      final SharedSessionContractImplementor session,
      final Object owner)
      throws SQLException {
    String value = rs.getString(position);
    if (value != null) {
      try {
        AccessAssertionPersister persister =
            ApplicationContextProvider.getApplicationContext()
                .getBean(AccessAssertionPersister.class);
        return persister.unmarshal(new JSONObject(value));
      } catch (JSONException e) {
        throw new RuntimeException(e);
      }
    } else {
      return null;
    }
  }

  @Override
  public final void nullSafeSet(
      final PreparedStatement st,
      final AccessAssertion value,
      final int index,
      final SharedSessionContractImplementor session)
      throws SQLException {
    if (value == null) {
      st.setNull(index, SQL_TYPE);
    } else {
      AccessAssertionPersister persister =
          ApplicationContextProvider.getApplicationContext()
              .getBean(AccessAssertionPersister.class);
      st.setString(index, persister.marshal(value).toString());
    }
  }

  @Override
  public final AccessAssertion replace(
      final AccessAssertion original, final AccessAssertion target, final Object owner) {
    return deepCopy(original);
  }

  @Override
  public final Class<AccessAssertion> returnedClass() {
    return AccessAssertion.class;
  }

  @Override
  public final int getSqlType() {
    return SQL_TYPE;
  }
}
