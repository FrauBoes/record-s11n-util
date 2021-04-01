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

package invoke;

import common.RecComponent;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import static java.lang.invoke.MethodType.methodType;

/**
 * Utility methods for record serialization, using MethodHandles.
 */
public class InvokeUtils {
    private static final MethodHandle MH_IS_RECORD;
    private static final MethodHandle MH_GET_RECORD_COMPONENTS;
    private static final MethodHandle MH_GET_NAME;
    private static final MethodHandle MH_GET_TYPE;
    private static final MethodHandles.Lookup LOOKUP;

    static {
        MethodHandle MH_isRecord;
        MethodHandle MH_getRecordComponents;
        MethodHandle MH_getName;
        MethodHandle MH_getType;
        LOOKUP = MethodHandles.lookup();

        try {
            // reflective machinery required to access the record components
            // without a static dependency on Java SE 14 APIs
            Class<?> c = Class.forName("java.lang.reflect.RecordComponent");
            MH_isRecord = LOOKUP.findVirtual(Class.class, "isRecord", methodType(boolean.class));
            MH_getRecordComponents = LOOKUP.findVirtual(Class.class, "getRecordComponents",
                    methodType(Array.newInstance(c, 0).getClass()))
                    .asType(methodType(Object[].class, Class.class));
            MH_getName = LOOKUP.findVirtual(c, "getName", methodType(String.class))
                    .asType(methodType(String.class, Object.class));
            MH_getType = LOOKUP.findVirtual(c, "getType", methodType(Class.class))
                    .asType(methodType(Class.class, Object.class));
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // pre-Java-14
            MH_isRecord = null;
            MH_getRecordComponents = null;
            MH_getName = null;
            MH_getType = null;
        } catch (IllegalAccessException unexpected) {
            throw new AssertionError(unexpected);
        }

        MH_IS_RECORD = MH_isRecord;
        MH_GET_RECORD_COMPONENTS = MH_getRecordComponents;
        MH_GET_NAME = MH_getName;
        MH_GET_TYPE = MH_getType;
    }

    /** Returns true if, and only if, the given class is a record class. */
    static boolean isRecord(Class<?> type) {
        try {
            return (boolean) MH_IS_RECORD.invokeExact(type);
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
            Object[] rawComponents = (Object[]) MH_GET_RECORD_COMPONENTS.invokeExact(type);
            RecComponent[] recordComponents = new RecComponent[rawComponents.length];
            for (int i = 0; i < rawComponents.length; i++) {
                final Object comp = rawComponents[i];
                recordComponents[i] = new RecComponent(
                        (String) MH_GET_NAME.invokeExact(comp),
                        (Class<?>) MH_GET_TYPE.invokeExact(comp), i);
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
            MethodHandle MH_get = LOOKUP.findVirtual(recordObject.getClass(),
                    recordComponent.name(),
                    methodType(recordComponent.type()));
            return (Object) MH_get.invoke(recordObject);
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
            MethodHandle MH_canonicalConstructor =
                    LOOKUP.findConstructor(recordType, methodType(void.class, paramTypes))
                            .asType(methodType(Object.class, paramTypes));
            return (T)MH_canonicalConstructor.invokeWithArguments(args);
        } catch (Throwable t) {
            throw new RuntimeException("Could not construct type (" + recordType.getName() + ")");
        }
    }
}
