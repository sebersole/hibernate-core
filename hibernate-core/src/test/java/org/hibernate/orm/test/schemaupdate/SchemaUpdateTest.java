/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.naming.Identifier;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.SkipLog;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@RunWith(Parameterized.class)
public class SchemaUpdateTest extends BaseSchemaTest {

	private boolean skipTest;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				LowercaseTableNameEntity.class
				, TestEntity.class
				, UppercaseTableNameEntity.class
				, MixedCaseTableNameEntity.class
				, Match.class
				, InheritanceRootEntity.class
				, InheritanceChildEntity.class
				, InheritanceSecondChildEntity.class
		};
	}

	@Override
	protected boolean createTempOutputFile() {
		return true;
	}

	@Override
	public void setUp() throws IOException {
		super.setUp();
		if ( SQLServerDialect.class.isAssignableFrom( Dialect.getDialect().getClass() ) ) {
			// SQLServerDialect stores case-insensitive quoted identifiers in mixed case,
			// so the checks at the end of this method won't work.
			skipTest = true;
			return;
		}

		// Databases that use case-insensitive quoted identifiers need to be skipped.
		// The following checks will work for checking those dialects that store case-insensitive
		// quoted identifiers as upper-case or lower-case. It does not work for dialects that
		// store case-insensitive identifiers in mixed case (like SQL Server).
		final IdentifierHelper identifierHelper = getStandardServiceRegistry().getService( JdbcEnvironment.class )
				.getIdentifierHelper();
		final String lowerCaseName = identifierHelper.toMetaDataObjectName( Identifier.toIdentifier(
				"testentity",
				true
		) );
		final String upperCaseName = identifierHelper.toMetaDataObjectName( Identifier.toIdentifier(
				"TESTENTITY",
				true
		) );
		final String mixedCaseName = identifierHelper.toMetaDataObjectName( Identifier.toIdentifier(
				"TESTentity",
				true
		) );
		if ( lowerCaseName.equals( upperCaseName ) ||
				lowerCaseName.equals( mixedCaseName ) ||
				upperCaseName.equals( mixedCaseName ) ) {
			StandardServiceRegistryBuilder.destroy( getStandardServiceRegistry() );
			skipTest = true;
		}
	}

	@Override
	public void tearDown() {
		if ( skipTest ) {
			return;
		}
		super.tearDown();
	}

	@Test
	public void testSchemaUpdateAndValidation() throws Exception {
		if ( skipTest ) {
			SkipLog.reportSkip( "skipping test because quoted names are not case-sensitive." );
			return;
		}

		createSchemaUpdate().setHaltOnError( true )
				.execute( EnumSet.of( TargetType.DATABASE ) );

		createSchemaValidator().validate();

		createSchemaUpdate().setHaltOnError( true )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ) );

		final String fileContent = getOutputFileContent();
		assertThat( "The update output file should be empty", fileContent, is( "" ) );
	}

	@Entity(name = "TestEntity")
	@Table(name = "`testentity`")
	public static class LowercaseTableNameEntity {
		@Id
		long id;
		String field1;

		@ManyToMany(mappedBy = "entities")
		Set<TestEntity> entity1s;
	}

	@Entity(name = "TestEntity1")
	public static class TestEntity {
		@Id
		@Column(name = "`Id`")
		long id;
		String field1;

		@ManyToMany
		Set<LowercaseTableNameEntity> entities;

		@OneToMany
		@JoinColumn
		private Set<UppercaseTableNameEntity> entitie2s;

		@ManyToOne
		private LowercaseTableNameEntity entity;
	}

	@Entity(name = "TestEntity2")
	@Table(name = "`TESTENTITY`")
	public static class UppercaseTableNameEntity {
		@Id
		long id;
		String field1;

		@ManyToOne
		TestEntity testEntity;

		@ManyToOne
		@JoinColumn(foreignKey = @ForeignKey(name = "FK_mixedCase"))
		MixedCaseTableNameEntity mixedCaseTableNameEntity;
	}

	@Entity(name = "TestEntity3")
	@Table(name = "`TESTentity`", indexes = {
			@Index(name = "index1", columnList = "`FieLd1`"),
			@Index(name = "Index2", columnList = "`FIELD_2`")
	})
	public static class MixedCaseTableNameEntity {
		@Id
		long id;
		@Column(name = "`FieLd1`")
		String field1;
		@Column(name = "`FIELD_2`")
		String field2;
		@Column(name = "`field_3`")
		String field3;
		String field4;

		@OneToMany
		@JoinColumn
		private Set<Match> matches = new HashSet<>();
	}

	@Entity(name = "Match")
	public static class Match {
		@Id
		long id;
		String match;

		@ElementCollection
		@CollectionTable
		private Map<Integer, Integer> timeline = new TreeMap<>();
	}

	@Entity(name = "InheritanceRootEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class InheritanceRootEntity {
		@Id
		protected Long id;
	}

	@Entity(name = "InheritanceChildEntity")
	@PrimaryKeyJoinColumn(name = "ID", foreignKey = @ForeignKey(name = "FK_ROOT"))
	public static class InheritanceChildEntity extends InheritanceRootEntity {
	}

	@Entity(name = "InheritanceSecondChildEntity")
	@PrimaryKeyJoinColumn(name = "ID")
	public static class InheritanceSecondChildEntity extends InheritanceRootEntity {
		@ManyToOne
		@JoinColumn
		public Match match;
	}
}
