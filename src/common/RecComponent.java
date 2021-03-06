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

package common;

/**
 * A record component, which has a name, a type and an index.
 *
 * (If running on Java 14+, this should be a record class ;) )
 *
 * The latter is the index of the record components in the class file's
 * record attribute, required to invoke the record's canonical constructor .
 */
public class RecComponent {
        private final String name;
        private final Class<?> type;
        private final int index;

        public RecComponent(String name, Class<?> type, int index) {
            this.name = name;
            this.type = type;
            this.index = index;
        }

        public String name() { return name; }
        public Class<?> type() { return type; }
        public int index() { return index; }
}
