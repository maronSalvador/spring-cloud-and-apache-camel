package com.example.orchestrator.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * 1:1 structural XML -> JSON conversion. If the target shape needs
 * restructuring (renames, flattening, dropped wrappers), replace with
 * a JOLT spec or XSLT stylesheet.
 */
public class XmlToJsonProcessor implements Processor {

    private static final XmlMapper XML = new XmlMapper();
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void process(Exchange exchange) throws Exception {
        String xml = exchange.getIn().getBody(String.class);
        JsonNode node = XML.readTree(xml);
        exchange.getIn().setBody(JSON.writeValueAsString(node));
    }
}
