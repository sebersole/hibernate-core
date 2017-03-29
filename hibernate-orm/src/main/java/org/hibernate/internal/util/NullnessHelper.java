/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

import java.util.function.Supplier;

/**
 * @author Steve Ebersole
 */
public class NullnessHelper {
	private NullnessHelper() {
	}

	public static <T> T nullif(T test, T fallback) {
		return test == null ? fallback : test;
	}

	public static <T> T nullif(T test, Supplier<T> fallbackSupplier) {
		return test != null ? test : fallbackSupplier.get();
	}
}
