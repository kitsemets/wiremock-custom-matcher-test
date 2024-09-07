package com.github.kitsemets.wiremock.matcher;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.common.JsonException;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.EagerMatchResult;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.NamedValueMatcher;
import com.github.tomakehurst.wiremock.stubbing.SubEvent;
import org.assertj.core.api.Assertions;

import java.util.List;

import static com.github.tomakehurst.wiremock.stubbing.SubEvent.JSON_ERROR;
import static com.github.tomakehurst.wiremock.stubbing.SubEvent.NON_MATCH_TYPE;

public class CustomJsonMatcher implements NamedValueMatcher<Request> {

    private final JsonNode expectedJson;

    public CustomJsonMatcher(@JsonProperty String expectedJson) {
        this.expectedJson = Json.read(expectedJson, JsonNode.class);
    }

    @Override
    public MatchResult match(Request actualRequest) {
        final String actualJson = actualRequest.getBodyAsString();
        try {
            final JsonNode actual = Json.read(actualJson, JsonNode.class);

            Assertions.assertThat(actual)
                    .usingRecursiveComparison()
                    .isEqualTo(this.expectedJson);

            return MatchResult.exactMatch();
        } catch (final JsonException e) {
            return MatchResult.noMatch(new SubEvent(JSON_ERROR, e.getErrors()));
        } catch (final AssertionError e) {
            return new EagerMatchResult(1,
                    List.of(SubEvent.message(NON_MATCH_TYPE, e.getMessage())),
                    List.of(new MatchResult.DiffDescription(this.getExpected(), actualJson, e.getMessage())));
        }
    }

    @Override
    public String getExpected() {
        return Json.prettyPrint(Json.write(this.expectedJson));
    }

    @Override
    public String getName() {
        return "custom json matcher";
    }
}
