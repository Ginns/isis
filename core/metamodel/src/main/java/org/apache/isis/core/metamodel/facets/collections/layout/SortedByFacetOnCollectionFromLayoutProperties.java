/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.core.metamodel.facets.collections.layout;

import java.util.Comparator;
import java.util.Properties;
import org.apache.isis.core.commons.lang.ClassUtil;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facets.collections.sortedby.SortedByFacet;
import org.apache.isis.core.metamodel.facets.collections.sortedby.SortedByFacetAbstract;

public class SortedByFacetOnCollectionFromLayoutProperties extends SortedByFacetAbstract {

    public static SortedByFacet create(Properties properties, FacetHolder holder) {
        final Class sortedBy = sortedBy(properties);
        return sortedBy != null? new SortedByFacetOnCollectionFromLayoutProperties(sortedBy, holder): null;
    }

    private SortedByFacetOnCollectionFromLayoutProperties(Class<? extends Comparator<?>> sortedBy, FacetHolder holder) {
        super(sortedBy, holder);
    }

    private static Class<?> sortedBy(Properties properties) {
        if(properties == null) {
            return null;
        }
        String sortedBy = properties.getProperty("sortedBy");
        if (sortedBy == null) {
            return null;
        }
        return ClassUtil.forName(sortedBy);
    }

}