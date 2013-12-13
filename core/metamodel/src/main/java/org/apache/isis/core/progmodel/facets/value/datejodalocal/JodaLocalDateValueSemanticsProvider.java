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

package org.apache.isis.core.progmodel.facets.value.datejodalocal;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import org.apache.isis.applib.adapters.EncoderDecoder;
import org.apache.isis.applib.adapters.EncodingException;
import org.apache.isis.applib.adapters.Parser;
import org.apache.isis.applib.profiles.Localization;
import org.apache.isis.core.commons.config.ConfigurationConstants;
import org.apache.isis.core.commons.config.IsisConfiguration;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facets.object.parseable.TextEntryParseException;
import org.apache.isis.core.progmodel.facets.object.value.ValueSemanticsProviderAndFacetAbstract;
import org.apache.isis.core.progmodel.facets.object.value.ValueSemanticsProviderContext;

public class JodaLocalDateValueSemanticsProvider extends ValueSemanticsProviderAndFacetAbstract<LocalDate> implements JodaLocalDateValueFacet {

    
    /**
     * Introduced to allow BDD tests to provide a different format string "mid-flight".
     * 
     * <p>
     * REVIEW: This seems only to have any effect if 'propertyType' is set to 'date'.
     * 
     * @see #setTitlePatternOverride(String)
     * @deprecated - because 'propertyType' parameter is never used
     */
    @Deprecated
    public static void setFormat(final String propertyType, final String pattern) {
        setTitlePatternOverride(pattern);
    }
    /**
     * A replacement for {@link #setFormat(String, String)}.
     */
    public static void setTitlePatternOverride(final String pattern) {
        OVERRIDE_TITLE_PATTERN.set(pattern);
    }
    
    /**
     * Key to indicate how LocalDate should be parsed/rendered.
     * 
     * <p>
     * eg:
     * <pre>
     * isis.value.format.date=iso
     * </pre>
     * 
     * <p>
     * A pre-determined list of values is available, specifically 'iso_encoding', 'iso' and 'medium' (see 
     * {@link #NAMED_TITLE_FORMATTERS}).  Alternatively,  can also specify a mask, eg <tt>dd-MMM-yyyy</tt>.
     * 
     * @see #NAMED_TITLE_FORMATTERS  
     */
    public final static String CFG_FORMAT_KEY = ConfigurationConstants.ROOT + "value.format.date";
    
    
    /**
     * Keys represent the values which can be configured, and which are used for the rendering of dates.
     * 
     */
    private static Map<String, DateTimeFormatter> NAMED_TITLE_FORMATTERS = Maps.newHashMap();
    static {
        NAMED_TITLE_FORMATTERS.put("iso_encoding", DateTimeFormat.forPattern("yyyyMMdd"));
        NAMED_TITLE_FORMATTERS.put("iso", DateTimeFormat.forPattern("yyyy-MM-dd"));
        NAMED_TITLE_FORMATTERS.put("long", DateTimeFormat.forStyle("L-"));
        NAMED_TITLE_FORMATTERS.put("medium", DateTimeFormat.forStyle("M-"));
        NAMED_TITLE_FORMATTERS.put("short", DateTimeFormat.forStyle("S-"));
    }
    
    private final static ThreadLocal<String> OVERRIDE_TITLE_PATTERN = new ThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return null;
        }
    };

    
    private final static List<DateTimeFormatter> PARSE_FORMATTERS = Lists.newArrayList();
    static {
        PARSE_FORMATTERS.add(DateTimeFormat.forStyle("L-"));
        PARSE_FORMATTERS.add(DateTimeFormat.forStyle("M-"));
        PARSE_FORMATTERS.add(DateTimeFormat.forStyle("S-"));
        PARSE_FORMATTERS.add(DateTimeFormat.forPattern("yyyy-MM-dd"));
        PARSE_FORMATTERS.add(DateTimeFormat.forPattern("yyyyMMdd"));
    }
    


    public static Class<? extends Facet> type() {
        return JodaLocalDateValueFacet.class;
    }


    // no default
    private static final LocalDate DEFAULT_VALUE = null;


    private final DateTimeFormatter encodingFormatter = DateTimeFormat.forPattern("yyyyMMdd");
    
    private DateTimeFormatter titleStringFormatter;
    private String titleStringFormatNameOrPattern;

    
    // //////////////////////////////////////
    // constructor
    // //////////////////////////////////////

    /**
     * Required because implementation of {@link Parser} and
     * {@link EncoderDecoder}.
     */
    public JodaLocalDateValueSemanticsProvider() {
        this(null, null, null);
    }

    /**
     * Uses {@link #type()} as the facet type.
     */
    public JodaLocalDateValueSemanticsProvider(
            final FacetHolder holder, final IsisConfiguration configuration, final ValueSemanticsProviderContext context) {
        super(type(), holder, LocalDate.class, 12, Immutability.IMMUTABLE, EqualByContent.HONOURED, DEFAULT_VALUE, configuration, context);

        String configuredNameOrPattern = getConfiguration().getString(CFG_FORMAT_KEY, "medium").toLowerCase().trim();
        updateTitleStringFormatter(configuredNameOrPattern);
    }


    private void updateTitleStringFormatter(String titleStringFormatNameOrPattern) {
        titleStringFormatter = NAMED_TITLE_FORMATTERS.get(titleStringFormatNameOrPattern);
        if (titleStringFormatter == null) {
            titleStringFormatter = DateTimeFormat.forPattern(titleStringFormatNameOrPattern);
        }
        this.titleStringFormatNameOrPattern = titleStringFormatNameOrPattern; 
    }
    

    // //////////////////////////////////////////////////////////////////
    // Parsing
    // //////////////////////////////////////////////////////////////////

    @Override
    protected LocalDate doParse(final Object context, final String entry, final Localization localization) {

        updateTitleStringFormatterIfOverridden();
        
        LocalDate contextDate = (LocalDate) context;

        final String dateString = entry.trim().toUpperCase();
        if (dateString.startsWith("+") && contextDate != null) {
            return JodaLocalDateUtil.relativeDate(contextDate, dateString, true);
        } else if (dateString.startsWith("-")  && contextDate != null) {
            return JodaLocalDateUtil.relativeDate(contextDate, dateString, false);
        } else {
            return parseDate(dateString, contextDate, localization);
        }
    }

    private void updateTitleStringFormatterIfOverridden() {
        final String overridePattern = OVERRIDE_TITLE_PATTERN.get();
        if (overridePattern == null || 
            titleStringFormatNameOrPattern.equals(overridePattern)) {
            return;
        } 
        
        // (re)create format
        updateTitleStringFormatter(overridePattern);
    }

    private LocalDate parseDate(final String dateStr, final Object original, final Localization localization) {
        return JodaLocalDateUtil.parseDate(dateStr, localization, PARSE_FORMATTERS);
    }
    

    // ///////////////////////////////////////////////////////////////////////////
    // TitleProvider
    // ///////////////////////////////////////////////////////////////////////////

    @Override
    public String titleString(final Object value, final Localization localization) {
        if (value == null) {
            return null;
        }
        final LocalDate date = (LocalDate) value;
        DateTimeFormatter f = titleStringFormatter;
        if (localization != null) {
            f = titleStringFormatter.withLocale(localization.getLocale());
        }
        return JodaLocalDateUtil.titleString(f, date);
    }

    @Override
    public String titleStringWithMask(final Object value, final String usingMask) {
        final LocalDate date = (LocalDate) value;
        return JodaLocalDateUtil.titleString(DateTimeFormat.forPattern(usingMask), date);
    }


    // //////////////////////////////////////////////////////////////////
    // EncoderDecoder
    // //////////////////////////////////////////////////////////////////

    @Override
    protected String doEncode(final Object object) {
        final LocalDate date = (LocalDate) object;
        return encode(date);
    }

    private synchronized String encode(final LocalDate date) {
        return encodingFormatter.print(date);
    }

    @Override
    protected LocalDate doRestore(final String data) {
        try {
            return parse(data);
        } catch (final IllegalArgumentException e) {
            throw new EncodingException(e);
        }
    }

    private synchronized LocalDate parse(final String data) {
        return encodingFormatter.parseLocalDate(data);
    }

    // //////////////////////////////////////////////////////////////////
    // JodaLocalDateValueFacet
    // //////////////////////////////////////////////////////////////////

    @Override
    public final LocalDate dateValue(final ObjectAdapter object) {
        return (LocalDate) (object == null ? null : object.getObject());
    }

    @Override
    public final ObjectAdapter createValue(final LocalDate date) {
        return getAdapterManager().adapterFor(date);
    }


    // //////////////////////////////////////
    
    @Override
    public String toString() {
        return "JodaLocalDateValueSemanticsProvider: " + titleStringFormatter;
    }

}
