/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.relational.spi.AuxiliaryDatabaseObject;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.metamodel.model.relational.spi.Exportable;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.Index;
import org.hibernate.metamodel.model.relational.spi.Namespace;
import org.hibernate.metamodel.model.relational.spi.Sequence;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.metamodel.model.relational.spi.UniqueKey;
import org.hibernate.naming.Identifier;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.AvailableSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSchemaMigrator implements SchemaMigrator {
	private static final Logger log = Logger.getLogger( IndividuallySchemaMigratorImpl.class );

	protected final HibernateSchemaManagementTool tool;
	protected final SchemaFilter schemaFilter;
	protected final DatabaseModel databaseModel;
	protected final JdbcServices jdbcServices;

	public AbstractSchemaMigrator(
			HibernateSchemaManagementTool tool,
			DatabaseModel databaseModel,
			SchemaFilter schemaFilter) {
		this.tool = tool;
		this.databaseModel = databaseModel;
		if ( schemaFilter == null ) {
			this.schemaFilter = DefaultSchemaFilter.INSTANCE;
		}
		else {
			this.schemaFilter = schemaFilter;
		}
		this.jdbcServices = tool.getServiceRegistry().getService( JdbcServices.class );
	}

	private UniqueConstraintSchemaUpdateStrategy uniqueConstraintStrategy;

	/**
	 * For testing...
	 */
	public void setUniqueConstraintStrategy(UniqueConstraintSchemaUpdateStrategy uniqueConstraintStrategy) {
		this.uniqueConstraintStrategy = uniqueConstraintStrategy;
	}

	@Override
	public void doMigration(ExecutionOptions options, TargetDescriptor targetDescriptor) {
		if ( !targetDescriptor.getTargetTypes().isEmpty() ) {
			final JdbcContext jdbcContext = tool.resolveJdbcContext( options.getConfigurationValues() );
			final DdlTransactionIsolator ddlTransactionIsolator = tool.getDdlTransactionIsolator( jdbcContext );
			try {
				final DatabaseInformation databaseInformation = Helper.buildDatabaseInformation(
						tool.getServiceRegistry(),
						ddlTransactionIsolator,
						databaseModel.getDefaultNamespace()
				);

				final GenerationTarget[] targets = tool.buildGenerationTargets(
						targetDescriptor,
						ddlTransactionIsolator,
						options.getConfigurationValues()
				);

				try {
					for ( GenerationTarget target : targets ) {
						target.prepare();
					}

					try {
						performMigration( databaseInformation, options, jdbcContext.getDialect(), targets );
					}
					finally {
						for ( GenerationTarget target : targets ) {
							try {
								target.release();
							}
							catch (Exception e) {
								log.debugf( "Problem releasing GenerationTarget [%s] : %s", target, e.getMessage() );
							}
						}
					}
				}
				finally {
					try {
						databaseInformation.cleanup();
					}
					catch (Exception e) {
						log.debug( "Problem releasing DatabaseInformation : " + e.getMessage() );
					}
				}
			}
			finally {
				ddlTransactionIsolator.release();
			}
		}
	}

	protected abstract NameSpaceTablesInformation performTablesMigration(
			DatabaseInformation existingDatabase,
			ExecutionOptions options,
			Dialect dialect,
			Formatter formatter,
			Set<String> exportIdentifiers,
			boolean tryToCreateCatalogs,
			boolean tryToCreateSchemas,
			Set<Identifier> exportedCatalogs,
			Namespace namespace,
			GenerationTarget[] targets);

	private void performMigration(
			DatabaseInformation existingDatabase,
			ExecutionOptions options,
			Dialect dialect,
			GenerationTarget... targets) {
		final boolean format = Helper.interpretFormattingEnabled( options.getConfigurationValues() );
		final Formatter formatter = format ? FormatStyle.DDL.getFormatter() : FormatStyle.NONE.getFormatter();

		final Set<String> exportIdentifiers = new HashSet<>( 50 );

		// Drop all AuxiliaryDatabaseObjects
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : databaseModel.getAuxiliaryDatabaseObjects() ) {
				applySqlStrings(
						true,
						dialect.getAuxiliaryDatabaseObjectExporter()
								.getSqlDropStrings( auxiliaryDatabaseObject, jdbcServices ),
						formatter,
						options,
						targets
				);
		}

		// Create beforeQuery-table AuxiliaryDatabaseObjects
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : databaseModel.getAuxiliaryDatabaseObjects() ) {
			if ( !auxiliaryDatabaseObject.isBeforeTablesOnCreation() ) {
				applySqlStrings(
						true,
						auxiliaryDatabaseObject.getSqlCreateStrings(),
						formatter,
						options,
						targets
				);
			}
		}

		boolean tryToCreateCatalogs = false;
		boolean tryToCreateSchemas = false;
		if ( options.shouldManageNamespaces() ) {
			if ( dialect.canCreateSchema() ) {
				tryToCreateSchemas = true;
			}
			if ( dialect.canCreateCatalog() ) {
				tryToCreateCatalogs = true;
			}
		}
		final Map<Namespace, NameSpaceTablesInformation> tablesInformation = new HashMap<>();
		Set<Identifier> exportedCatalogs = new HashSet<>();
		for ( Namespace namespace : databaseModel.getNamespaces() ) {
			final NameSpaceTablesInformation nameSpaceTablesInformation = performTablesMigration(
					existingDatabase,
					options,
					dialect,
					formatter,
					exportIdentifiers,
					tryToCreateCatalogs,
					tryToCreateSchemas,
					exportedCatalogs,
					namespace,
					targets
			);
			tablesInformation.put( namespace, nameSpaceTablesInformation );
			if ( schemaFilter.includeNamespace( namespace ) ) {
				for ( Sequence sequence : namespace.getSequences() ) {
					checkExportIdentifier( sequence, exportIdentifiers );
					final SequenceInformation sequenceInformation = existingDatabase.getSequenceInformation( sequence.getQualifiedName() );
					if ( sequenceInformation == null ) {
						applySqlStrings(
								false,
								dialect.getSequenceExporter().getSqlCreateStrings(
										sequence,
										jdbcServices
								),
								formatter,
								options,
								targets
						);
					}
				}
			}
		}

		//NOTE : Foreign keys must be created *afterQuery* all tables of all namespaces for cross namespace fks. see HHH-10420
		for ( Namespace namespace : databaseModel.getNamespaces() ) {
			if ( schemaFilter.includeNamespace( namespace ) ) {
				final NameSpaceTablesInformation nameSpaceTablesInformation = tablesInformation.get( namespace );
				for ( Table table : namespace.getTables() ) {
					if ( table.isExportable() ) {
						final ExportableTable exportableTable = (ExportableTable) table;
						if ( schemaFilter.includeTable( table ) ) {
							final TableInformation tableInformation = nameSpaceTablesInformation
									.getTableInformation( exportableTable );
							if ( tableInformation == null || tableInformation.isPhysicalTable() ) {
								applyForeignKeys(
										exportableTable,
										tableInformation,
										dialect,
										formatter,
										options,
										targets
								);
							}
						}
					}
				}
			}
		}

		// Create afterQuery-table AuxiliaryDatabaseObjects
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : databaseModel.getAuxiliaryDatabaseObjects() ) {
			if ( auxiliaryDatabaseObject.isBeforeTablesOnCreation() ) {
				applySqlStrings(
						true,
						auxiliaryDatabaseObject.getSqlCreateStrings(),
						formatter,
						options,
						targets
				);
			}
		}
	}

	protected void createTable(
			ExportableTable table,
			Dialect dialect,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		applySqlStrings(
				false,
				dialect.getTableExporter().getSqlCreateStrings( table, jdbcServices ),
				formatter,
				options,
				targets
		);
	}

	protected void migrateTable(
			ExportableTable table,
			TableInformation tableInformation,
			Dialect dialect,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		applySqlStrings(
				false,
				dialect.getTableAlter().getSqlAlterStrings( table, tableInformation, jdbcServices ),
				formatter,
				options,
				targets
		);
	}

	protected void applyIndexes(
			ExportableTable table,
			TableInformation tableInformation,
			Dialect dialect,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		final Exporter<Index> exporter = dialect.getIndexExporter();

		for ( Index index : table.getIndexes() ) {
			if ( index.getName() == null ) {
				IndexInformation existingIndex = null;
				if ( tableInformation != null ) {
					existingIndex = findMatchingIndex( index, tableInformation );
				}
				if ( existingIndex == null ) {
					applySqlStrings(
							false,
							exporter.getSqlCreateStrings( index, jdbcServices ),
							formatter,
							options,
							targets
					);
				}
			}
		}
	}

	private IndexInformation findMatchingIndex(Index index, TableInformation tableInformation) {
		return tableInformation.getIndex( index.getName() );
	}

	protected void applyUniqueKeys(
			ExportableTable table,
			TableInformation tableInfo,
			Dialect dialect,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( uniqueConstraintStrategy == null ) {
			uniqueConstraintStrategy = determineUniqueConstraintSchemaUpdateStrategy();
		}

		if ( uniqueConstraintStrategy != UniqueConstraintSchemaUpdateStrategy.SKIP ) {
			final Exporter<UniqueKey> exporter = dialect.getUniqueKeyExporter();

			for(UniqueKey uniqueKey : table.getUniqueKeys()){
				// Skip if index already exists. Most of the time, this
				// won't work since most Dialects use Constraints. However,
				// keep it for the few that do use Indexes.
				IndexInformation indexInfo = null;
				if ( tableInfo != null && uniqueKey.getName() != null ) {
					indexInfo = tableInfo.getIndex( uniqueKey.getName() );
				}
				if ( indexInfo == null ) {
					if ( uniqueConstraintStrategy == UniqueConstraintSchemaUpdateStrategy.DROP_RECREATE_QUIETLY ) {
						applySqlStrings(
								true,
								exporter.getSqlDropStrings( uniqueKey, jdbcServices ),
								formatter,
								options,
								targets
						);
					}

					applySqlStrings(
							true,
							exporter.getSqlCreateStrings( uniqueKey, jdbcServices ),
							formatter,
							options,
							targets
					);
				}
			}
		}
	}

	private UniqueConstraintSchemaUpdateStrategy determineUniqueConstraintSchemaUpdateStrategy() {
		final ConfigurationService cfgService = tool.getServiceRegistry().getService( ConfigurationService.class );

		return UniqueConstraintSchemaUpdateStrategy.interpret(
				cfgService.getSetting( UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, StandardConverters.STRING )
		);
	}

	protected void applyForeignKeys(
			ExportableTable table,
			TableInformation tableInformation,
			Dialect dialect,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( dialect.hasAlterTable() ) {
			final Exporter<ForeignKey> exporter = dialect.getForeignKeyExporter();

			for ( ForeignKey foreignKey : table.getForeignKeys() ) {
				if ( foreignKey.isExportationEnabled() ) {
					ForeignKeyInformation existingForeignKey = null;
					if ( tableInformation != null ) {
						existingForeignKey = findMatchingForeignKey(
								foreignKey,
								tableInformation
						);
					}
					if ( existingForeignKey == null ) {
						// todo : shouldn't we just drop+recreate if FK exists?
						//		this follows the existing code from legacy SchemaUpdate which just skipped

						// in old SchemaUpdate code, this was the trigger to "create"
						applySqlStrings(
								false,
								exporter.getSqlCreateStrings( foreignKey, jdbcServices ),
								formatter,
								options,
								targets
						);
					}
				}
			}
		}
	}

	private ForeignKeyInformation findMatchingForeignKey(ForeignKey foreignKey, TableInformation tableInformation) {
		if ( foreignKey.getName() == null ) {
			return null;
		}
		return tableInformation.getForeignKey( Identifier.toIdentifier( foreignKey.getName() ) );
	}

	protected void checkExportIdentifier(Exportable exportable, Set<String> exportIdentifiers) {
		final String exportIdentifier = exportable.getExportIdentifier();
		if ( exportIdentifiers.contains( exportIdentifier ) ) {
			throw new SchemaManagementException(
					String.format(
							"Export identifier [%s] encountered more than once",
							exportIdentifier
					)
			);
		}
		exportIdentifiers.add( exportIdentifier );
	}

	protected void createSchemaAndCatalog(
			DatabaseInformation existingDatabase,
			ExecutionOptions options,
			Dialect dialect,
			Formatter formatter,
			boolean tryToCreateCatalogs,
			boolean tryToCreateSchemas,
			Set<Identifier> exportedCatalogs,
			Namespace namespace,
			GenerationTarget[] targets) {
		if ( tryToCreateCatalogs || tryToCreateSchemas ) {
			if ( tryToCreateCatalogs ) {
				final Identifier catalogLogicalName = namespace.getCatalogName();
				final Identifier catalogPhysicalName = namespace.getCatalogName();

				if ( catalogPhysicalName != null && !exportedCatalogs.contains( catalogLogicalName )
						&& !existingDatabase.catalogExists( catalogLogicalName ) ) {
					applySqlStrings(
							false,
							dialect.getCreateCatalogCommand( catalogPhysicalName.render( dialect ) ),
							formatter,
							options,
							targets
					);
					exportedCatalogs.add( catalogLogicalName );
				}
			}

			if ( tryToCreateSchemas
					&& namespace.getSchemaName() != null
					&& !existingDatabase.schemaExists( namespace ) ) {
				applySqlStrings(
						false,
						dialect.getCreateSchemaCommand( namespace.getSchemaName().render( dialect ) ),
						formatter,
						options,
						targets
				);
			}
		}
	}

	protected static void applySqlStrings(
			boolean quiet,
			String[] sqlStrings,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( sqlStrings != null ) {
			for ( String sqlString : sqlStrings ) {
				applySqlString( quiet, sqlString, formatter, options, targets );
			}
		}
	}

	private static void applySqlString(
			boolean quiet,
			String sqlString,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( !StringHelper.isEmpty( sqlString ) ) {
			for ( GenerationTarget target : targets ) {
				try {
					target.accept( formatter.format( sqlString ) );
				}
				catch (CommandAcceptanceException e) {
					if ( !quiet ) {
						options.getExceptionHandler().handleException( e );
					}
					// otherwise ignore the exception
				}
			}
		}
	}
}
