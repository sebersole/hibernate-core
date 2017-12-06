package org.hibernate.test.schemaupdate.idgenerator;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TableGeneratorTest {

	private StandardServiceRegistry ssr;
	private File output;
	private MetadataImplementor metadata;
	private static final int INITIAL_VALUE = 5;
	private static final int EXPECTED_DB_INSERTED_VALUE = INITIAL_VALUE + 1;

	@Before
	public void setUp() throws Exception {
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.build();

		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();

		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( TestEntity.class )
				.buildMetadata();
		metadata.validate();
	}

	@Test
	public void testTableGeneratorIsGenerated() throws Exception {
		new SchemaExport()
				.setOutputFile( output.getAbsolutePath() )
				.create( EnumSet.of( TargetType.SCRIPT, TargetType.DATABASE ), metadata );

		final List<String> commands = Files.readAllLines( output.toPath() );

		final String expectedTestEntityTableCreationCommand = "create table test_entity \\(id .*, primary key \\(id\\)\\)";
		assertTrue(
				"The command '" + expectedTestEntityTableCreationCommand + "' has not been correctly generated",
				isCommandGenerated( commands, expectedTestEntityTableCreationCommand )
		);

		final String expectedIdTableGeneratorCreationCommand = "create table ID_TABLE_GENERATOR \\(PK .*, VALUE .*, primary key \\(PK\\)\\)";

		assertTrue(
				"The command '" + expectedIdTableGeneratorCreationCommand + "' has not been correctly generated",

				isCommandGenerated(
						commands,
						expectedIdTableGeneratorCreationCommand
				)
		);

		final String expectedInsertIntoTableGeneratorCommand = "INSERT INTO ID_TABLE_GENERATOR\\(PK, VALUE\\) values \\('TEST_ENTITY_ID', " + EXPECTED_DB_INSERTED_VALUE + "\\)\\)";

		assertTrue(
				"The command '" + expectedInsertIntoTableGeneratorCommand + "' has not been correctly generated",
				isCommandGenerated(
						commands,
						expectedInsertIntoTableGeneratorCommand
				)
		);
	}

	@After
	public void tearDown() {
		try {
			new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	@TableGenerator(name = "tableGenerator",
			table = "ID_TABLE_GENERATOR",
			pkColumnName = "PK",
			pkColumnValue = "TEST_ENTITY_ID",
			valueColumnName = "VALUE",
			allocationSize = 3,
			initialValue = INITIAL_VALUE)
	public static class TestEntity {
		Long id;

		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "tableGenerator")
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	private boolean isCommandGenerated(List<String> commands, String expectedCommnad) {
		final Pattern pattern = Pattern.compile( expectedCommnad.toLowerCase() );
		for ( String command : commands ) {
			Matcher matcher = pattern.matcher( command.toLowerCase() );
			if ( matcher.matches() ) {
				return true;
			}
		}
		return false;
	}

}
