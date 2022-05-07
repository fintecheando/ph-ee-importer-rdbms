package org.apache.fineract.paymenthub.importer.importer;

import static org.apache.fineract.paymenthub.importer.OperatorUtils.strip;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import org.apache.commons.text.StringEscapeUtils;

public class JsonPathReader {
    private static ParseContext jsonParser;

    static {
        Configuration config = Configuration.defaultConfiguration()
                .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL)
                .addOptions(Option.SUPPRESS_EXCEPTIONS);
        jsonParser = JsonPath.using(config);
    }

    public static DocumentContext parse(String json) {
        return jsonParser.parse(json);
    }

    public static DocumentContext parseEscaped(String escapedJson) {
        String rawString = StringEscapeUtils.unescapeJson(strip(escapedJson));
        return jsonParser.parse(rawString);
    }
}
