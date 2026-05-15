package com.am.observability.logback;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the shared {@code am-logback-include.xml} resource.
 *
 * <p>Confirms that:
 * <ol>
 *     <li>the file is well-formed XML;</li>
 *     <li>each JSON encoder declares a {@code <callerData>} provider so
 *         Loki/ELK pick up {@code caller.class}, {@code caller.method},
 *         {@code caller.file}, and {@code caller.line} from every JSON
 *         log line.</li>
 * </ol>
 *
 * <p>Catches encoder mis-configuration at build time instead of waiting
 * for the first service boot to fail.</p>
 */
class CallerDataLoggingTest {

    @Test
    void includeResourceDeclaresCallerDataProvider() throws Exception {
        String content;
        try (var in = getClass().getClassLoader().getResourceAsStream("am-logback-include.xml")) {
            assertThat(in).isNotNull();
            content = new String(in.readAllBytes());
        }

        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(content));

        int callerDataCount = 0;
        try {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamReader.START_ELEMENT && "callerData".equals(reader.getLocalName())) {
                    callerDataCount++;
                }
            }
        } finally {
            reader.close();
        }

        assertThat(callerDataCount)
                .as("expect one <callerData> provider per JSON encoder (STDOUT, STDERR, FILE)")
                .isEqualTo(3);
        assertThat(content).contains("caller.class", "caller.method", "caller.file", "caller.line");
        assertThat(content).contains("\"!caller-off\"");
    }
}
