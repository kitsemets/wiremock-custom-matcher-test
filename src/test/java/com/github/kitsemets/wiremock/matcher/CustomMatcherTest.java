package com.github.kitsemets.wiremock.matcher;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.restassured.RestAssured.given;

@WireMockTest
public class CustomMatcherTest {
    @Test
    void testCustomMatcher(final WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlEqualTo("/foo"))
                .willReturn(ok()));

        final OffsetDateTime timestamp = Instant.now().atOffset(ZoneOffset.UTC);
        final Bar bar1 = new Bar("bar", timestamp);

        given().port(wmRuntimeInfo.getHttpPort())
                .body(bar1)
                .when()
                .post("/foo");

        final Bar bar2 = new Bar("bar", timestamp.plusSeconds(5));

        /*
        prints out:
        com.github.tomakehurst.wiremock.client.VerificationException: No requests exactly matched. Most similar request was:  expected:<
        POST
        /foo

        {
          "name" : "bar",
          "timestamp" : "2024-09-07T14:04:44.465193Z"
        }> but was:<
        POST
        /foo

        {"name":"bar","timestamp":"2024-09-07T14:04:39.465193Z"}>
         */
        verify(postRequestedFor(urlPathEqualTo("/foo"))
                .andMatching(new CustomJsonMatcher(Json.write(bar2)))
        );
    }

    @Test
    void testAssertJ() {
        final OffsetDateTime timestamp = Instant.now().atOffset(ZoneOffset.UTC);
        final Bar bar1 = new Bar("bar", timestamp);
        final Bar bar2 = new Bar("bar", timestamp.plusSeconds(5));

        /*
        prints out:
        java.lang.AssertionError:
        Expecting actual:
          Bar[name=bar, timestamp=2024-09-07T14:03:51.188032Z]
        to be equal to:
          Bar[name=bar, timestamp=2024-09-07T14:03:56.188032Z]
        when recursively comparing field by field, but found the following difference:

        field/property 'timestamp' differ:
        - actual value  : 2024-09-07T14:03:51.188032Z (java.time.OffsetDateTime)
        - expected value: 2024-09-07T14:03:56.188032Z (java.time.OffsetDateTime)
        ...
         */
        Assertions.assertThat(bar1)
                .usingRecursiveComparison()
                .isEqualTo(bar2);
    }

    record Bar(String name, @JsonFormat(shape = JsonFormat.Shape.STRING) OffsetDateTime timestamp) {
    }
}
