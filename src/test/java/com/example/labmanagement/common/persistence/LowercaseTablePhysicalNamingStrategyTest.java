package com.example.labmanagement.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.boot.model.naming.Identifier;
import org.junit.jupiter.api.Test;

class LowercaseTablePhysicalNamingStrategyTest {

	private final LowercaseTablePhysicalNamingStrategy strategy = new LowercaseTablePhysicalNamingStrategy();

	@Test
	void lowercasesTableNames() {
		Identifier physicalName = strategy.toPhysicalTableName(Identifier.toIdentifier("BaoTri"), null);

		assertThat(physicalName.getText()).isEqualTo("baotri");
	}

	@Test
	void leavesColumnNamesUnchanged() {
		Identifier columnName = Identifier.toIdentifier("MaBaoTri");

		assertThat(strategy.toPhysicalColumnName(columnName, null)).isEqualTo(columnName);
	}
}
