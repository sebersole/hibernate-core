/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cfg;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.Persistence;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.boot.ClassLoaderServiceTestingImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-13227")
public class CfgFilePropertyTest extends BaseUnitTestCase {

	@Test
	public void test() throws InterruptedException {

		final AtomicReference<Exception> exceptionHolder = new AtomicReference<>();

		Thread thread = new Thread( () -> {
			try {
				final Map props = new HashMap();
				props.put( AvailableSettings.CFG_FILE, "/org/hibernate/orm/test/boot/cfgXml/hibernate.cfg.xml" );
				props.put( AvailableSettings.CLASSLOADERS, Collections.singletonList( new TestClassLoader() ) );

				Persistence.createEntityManagerFactory( "ExcludeUnlistedClassesTest1", props );
			}
			catch (Exception e) {
				exceptionHolder.set( e );
			}
		} );

		thread.start();
		thread.join();

		assertNull( exceptionHolder.get() );
	}

	private static class TestClassLoader extends ClassLoader {
		@Override
		protected Enumeration<URL> findResources(String name) throws IOException {
			return name.equals( "META-INF/persistence.xml" ) ?
					Collections.enumeration(
							Collections.singletonList(
									ClassLoaderServiceTestingImpl.INSTANCE.locateResource(
											"org/hibernate/jpa/test/persistenceunit/META-INF/persistence.xml" )
							)
					) :
					Collections.emptyEnumeration();
		}
	}
}
