package com.example.labmanagement.common.persistence;

import java.util.Locale;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public final class LowercaseTablePhysicalNamingStrategy extends PhysicalNamingStrategyStandardImpl {

	@Override
	public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		if (logicalName == null) {
			return null;
		}
		return Identifier.toIdentifier(logicalName.getText().toLowerCase(Locale.ROOT), logicalName.isQuoted());
	}
}
