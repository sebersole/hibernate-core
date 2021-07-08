/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.mapping;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.IndexColumn;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey( value = "HHH-1268")
@Jpa(
		annotatedClasses = {
				UnidirectionalOneToManyIndexColumnTest.Parent.class,
				UnidirectionalOneToManyIndexColumnTest.Child.class
		},
		integrationSettings = {
				@Setting(name = AvailableSettings.HBM2DDL_AUTO, value = "create-drop"),
				@Setting(name = AvailableSettings.IMPLICIT_NAMING_STRATEGY, value = "org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl")
		}
)
public class UnidirectionalOneToManyIndexColumnTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	public void testRemovingAChild(EntityManagerFactoryScope scope) {
		int parentId = scope.fromTransaction(
				entityManager -> {
					Parent parent = new Parent();
					parent.getChildren().add( new Child() );
					parent.getChildren().add( new Child() );
					parent.getChildren().add( new Child() );
					entityManager.persist( parent );
					return parent.getId();
				}
		);

		scope.inTransaction(
				entityManager -> {
					Parent parent = entityManager.find( Parent.class, parentId );
					List<Child> children = parent.getChildren();
					assertThat( children.size(), is( 3 ) );
					children.remove( 0 );
					entityManager.persist( parent );
				}
		);

		scope.inEntityManager(
				entityManager -> {
					Parent parent = entityManager.find( Parent.class, parentId );
					List<Child> children = parent.getChildren();
					assertThat( children.size(), is( 2 ) );
				}
		);
	}


	@Entity(name = "Parent")
	@Table(name = "PARENT")
	public static class Parent {

		@Id
		@GeneratedValue
		private int id;

		@OneToMany(targetEntity = Child.class, cascade = CascadeType.ALL)
		@IndexColumn(name = "position")
		private List<Child> children = new ArrayList<>();

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}
	}

	@Entity( name = "Child" )
	@Table(name = "CHILD")
	public static class Child {
		@Id
		@GeneratedValue
		private int id;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}
	}
}
