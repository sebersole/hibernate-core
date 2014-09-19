/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.namingstrategy;

import org.hibernate.cfg.naming.EntityNamingSource;
import org.hibernate.cfg.naming.ImplicitCollectionTableNameSource;
import org.hibernate.cfg.naming.ImplicitJoinTableNameSource;
import org.hibernate.cfg.naming.ImplicitNamingStrategyStandardImpl;
import org.hibernate.cfg.naming.ImplicitPrimaryTableNameSource;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests of the new logical/physical naming strategy split
 *
 * @author Steve Ebersole
 */
public class ImplicitNamingStrategyStandardImplTest extends BaseUnitTestCase {

	@Test
	public void testImplicitPrimaryTableNaming() {
		// no explicit entity name (of any kind)
		{
			final EntityNamingSourceImpl entityNamingSource = new EntityNamingSourceImpl( Workflow.class, null, null );
			String implicitName = ImplicitNamingStrategyStandardImpl.INSTANCE.determinePrimaryTableName(
					new ImplicitPrimaryTableNameSource() {
						@Override
						public EntityNamingSource getEntityNamingSource() {
							return entityNamingSource;
						}
					}
			);

			assertEquals( "Workflow", implicitName );
		}

		// explicit entity name
		{
			final EntityNamingSourceImpl entityNamingSource = new EntityNamingSourceImpl( Workflow.class, "Kwerkflow", null );
			String implicitName = ImplicitNamingStrategyStandardImpl.INSTANCE.determinePrimaryTableName(
					new ImplicitPrimaryTableNameSource() {
						@Override
						public EntityNamingSource getEntityNamingSource() {
							return entityNamingSource;
						}
					}
			);

			assertEquals( "Kwerkflow", implicitName );
		}

		// explicit jpa entity name
		{
			final EntityNamingSourceImpl entityNamingSource = new EntityNamingSourceImpl( Workflow.class, null, "Kwerkflow" );
			String implicitName = ImplicitNamingStrategyStandardImpl.INSTANCE.determinePrimaryTableName(
					new ImplicitPrimaryTableNameSource() {
						@Override
						public EntityNamingSource getEntityNamingSource() {
							return entityNamingSource;
						}
					}
			);

			assertEquals( "Kwerkflow", implicitName );
		}

		// explicit entity name and jpa entity name;
		// (I'm not sure if this can happen in practice, but should still be predictable).
		{
			final EntityNamingSourceImpl entityNamingSource = new EntityNamingSourceImpl( Workflow.class, "Kwerkflow", "Kwerkflow1" );
			String implicitName = ImplicitNamingStrategyStandardImpl.INSTANCE.determinePrimaryTableName(
					new ImplicitPrimaryTableNameSource() {
						@Override
						public EntityNamingSource getEntityNamingSource() {
							return entityNamingSource;
						}
					}
			);

			assertEquals( "Kwerkflow1", implicitName );
		}
	}

	@Test
	public void testImplicitJoinTableNaming() {
		// bare minimum for JPA
		{
			String implicitName = ImplicitNamingStrategyStandardImpl.INSTANCE.determineJoinTableName(
					new ImplicitJoinTableNameSource() {
						@Override
						public String getOwningPhysicalTableName() {
							return "WORKFLOW";
						}

						@Override
						public EntityNamingSource getOwningEntityNamingSource() {
							return null;
						}

						@Override
						public String getNonOwningPhysicalTableName() {
							return "STEP";
						}

						@Override
						public EntityNamingSource getNonOwningEntityNamingSource() {
							return null;
						}

						@Override
						public String getAssociationOwningAttributeName() {
							return null;
						}
					}
			);

			assertEquals( "WORKFLOW_STEP", implicitName );
		}

		// provide return values that should be ignored
		{
			String implicitName = ImplicitNamingStrategyStandardImpl.INSTANCE.determineJoinTableName(
					new ImplicitJoinTableNameSource() {
						@Override
						public String getOwningPhysicalTableName() {
							return "Category";
						}

						@Override
						public EntityNamingSource getOwningEntityNamingSource() {
							// should be ignored
							return new EntityNamingSourceImpl( Category.class );
						}

						@Override
						public String getNonOwningPhysicalTableName() {
							return "ITEMS";
						}

						@Override
						public EntityNamingSource getNonOwningEntityNamingSource() {
							// should be ignored
							return new EntityNamingSourceImpl( Item.class );
						}

						@Override
						public String getAssociationOwningAttributeName() {
							// should be ignored
							return "items";
						}
					}
			);

			assertEquals( "Category_ITEMS", implicitName );
		}
	}

	@Test
	public void testImplicitCollectionTableNaming() {
		// no explicit JPA entity name.
		{
			String implicitName = ImplicitNamingStrategyStandardImpl.INSTANCE.determineCollectionTableName(
					new ImplicitCollectionTableNameSource() {
						@Override
						public String getOwningPhysicalTableName() {
							return null;
						}

						@Override
						public EntityNamingSource getOwningEntityNamingSource() {
							return new EntityNamingSourceImpl( Workflow.class );
						}

						@Override
						public String getAssociationOwningAttributePath() {
							return "supportedLocales";
						}
					}
			);

			assertEquals( "Workflow_supportedLocales", implicitName );
		}

		// explicit entity name
		{
			String implicitName = ImplicitNamingStrategyStandardImpl.INSTANCE.determineCollectionTableName(
					new ImplicitCollectionTableNameSource() {
						@Override
						public String getOwningPhysicalTableName() {
							return null;
						}

						@Override
						public EntityNamingSource getOwningEntityNamingSource() {
							return new EntityNamingSourceImpl( Workflow.class, "Kwerkflow", null );
						}

						@Override
						public String getAssociationOwningAttributePath() {
							return "supportedLocales";
						}
					}
			);

			assertEquals( "Kwerkflow_supportedLocales", implicitName );
		}


		// explicit JPA entity name
		{
			String implicitName = ImplicitNamingStrategyStandardImpl.INSTANCE.determineCollectionTableName(
					new ImplicitCollectionTableNameSource() {
						@Override
						public String getOwningPhysicalTableName() {
							return null;
						}

						@Override
						public EntityNamingSource getOwningEntityNamingSource() {
							return new EntityNamingSourceImpl( Workflow.class, null, "Kwerkflow" );
						}

						@Override
						public String getAssociationOwningAttributePath() {
							return "supportedLocales";
						}
					}
			);

			assertEquals( "Kwerkflow_supportedLocales", implicitName );
		}

		// explicit entity name and JPA entity name
		{
			String implicitName = ImplicitNamingStrategyStandardImpl.INSTANCE.determineCollectionTableName(
					new ImplicitCollectionTableNameSource() {
						@Override
						public String getOwningPhysicalTableName() {
							return null;
						}

						@Override
						public EntityNamingSource getOwningEntityNamingSource() {
							return new EntityNamingSourceImpl( Workflow.class, "Kwerkflow", "Kwerkflow1" );
						}

						@Override
						public String getAssociationOwningAttributePath() {
							return "supportedLocales";
						}
					}
			);

			assertEquals( "Kwerkflow1_supportedLocales", implicitName );
		}

		// provide return values that should be ignored
		{
			String implicitName = ImplicitNamingStrategyStandardImpl.INSTANCE.determineCollectionTableName(
					new ImplicitCollectionTableNameSource() {
						@Override
						public String getOwningPhysicalTableName() {
							return "WORKFLOW";
						}

						@Override
						public EntityNamingSource getOwningEntityNamingSource() {
							return new EntityNamingSourceImpl( Workflow.class );
						}

						@Override
						public String getAssociationOwningAttributePath() {
							return "supportedLocales";
						}
					}
			);

			assertEquals( "Workflow_supportedLocales", implicitName  );
		}
	}
}
