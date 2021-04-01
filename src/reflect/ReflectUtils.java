/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import common.RecComponent;

/**
 * Utility methods for record serialization, using Java Core Reflection.
 */
public class ReflectUtils {
    private static final Method IS_RECORD;
    private static final Method GET_RECORD_COMPONENTS;
    private static final Method GET_NAME;
    private static final Method GET_TYPE;

    static {
        Method isRecord;
        Method getRecordComponents;
        Method getName;
        Method getType;

        try {
            // reflective machinery required to access the record components
            // without a static dependency on Java SE 14 APIs
            Class<?> c = Class.forName("java.lang.reflect.RecordComponent");
            isRecord = Class.class.getDeclaredMethod("isRecord");
            getRecordComponents = Class.class.getMethod("getRecordComponents");
            getName = c.getMethod("getName");
            getType = c.getMethod("getType");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // pre-Java-14
            isRecord = null;
            getRecordComponents = null;
            getName = null;
            getType = null;
        }

        IS_RECORD = isRecord;
        GET_RECORD_COMPONENTS = getRecordComponents;
        GET_NAME = getName;
        GET_TYPE = getType;
    }

    /** Returns true if, and only if, the given class is a record class. */
    static boolean isRecord(Class<?> type) {
        try {
            return (boolean) IS_RECORD.invoke(type);
        } catch (Throwable t) {
            throw new RuntimeException("Could not determine type (" + type + ")");
        }
    }

    /**
     * Returns an ordered array of the record components for the given record
     * class. The order is imposed by the given comparator. If the given
     * comparator is null, the order is that of the record components in the
     * record attribute of the class file.
     */
    static <T> RecComponent[] recordComponents(Class<T> type,
                                               Comparator<RecComponent> comparator) {
        try {
            Object[] rawComponents = (Object[]) GET_RECORD_COMPONENTS.invoke(type);
            RecComponent[] recordComponents = new RecComponent[rawComponents.length];
            for (int i = 0; i < rawComponents.length; i++) {
                final Object comp = rawComponents[i];
                recordComponents[i] = new RecComponent(
                        (String) GET_NAME.invoke(comp),
                        (Class<?>) GET_TYPE.invoke(comp), i);
            }
            if (comparator != null) Arrays.sort(recordComponents, comparator);
            return recordComponents;
        } catch (Throwable t) {
            throw new RuntimeException("Could not retrieve record components (" + type.getName() + ")");
        }
    }

    /** Retrieves the value of the record component for the given record object. */
    static Object componentValue(Object recordObject,
                                         RecComponent recordComponent) {
        try {
            Method get = recordObject.getClass().getDeclaredMethod(recordComponent.name());
            return get.invoke(recordObject);
        } catch (Throwable t) {
            throw new RuntimeException("Could not retrieve record components ("
                    + recordObject.getClass().getName() + ")");
        }
    }

    /**
     * Invokes the canonical constructor of a record class with the
     * given argument values.
     */
    static <T> T invokeCanonicalConstructor(Class<T> recordType,
                                                    RecComponent[] recordComponents,
                                                    Object[] args) {
        try {
            Class<?>[] paramTypes = Arrays.stream(recordComponents)
                    .map(RecComponent::type)
                    .toArray(Class<?>[]::new);
            Constructor<T> canonicalConstructor = recordType.getConstructor(paramTypes);
            return canonicalConstructor.newInstance(args);
        } catch (Throwable t) {
            throw new RuntimeException("Could not construct type (" + recordType.getName() + ")");
        }
    }
}
