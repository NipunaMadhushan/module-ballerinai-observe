/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.observability.metrics;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.ballerina.identifier.Utils;
import io.ballerina.runtime.observability.metrics.PercentileValue;
import io.ballerina.runtime.observability.metrics.Snapshot;
import io.ballerina.runtime.observability.metrics.Tag;
import io.ballerina.stdlib.observability.ObservabilityBaseTest;
import io.ballerina.stdlib.observe.mockextension.model.Metrics;
import io.ballerina.stdlib.observe.mockextension.model.MockGauge;
import io.ballerina.stdlib.observe.mockextension.model.MockMetric;
import org.ballerinalang.test.util.HttpClientRequest;
import org.ballerinalang.test.util.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Metrics related Test Cases.
 */
@Test(groups = "metrics-test")
public class MetricsTestCase extends ObservabilityBaseTest {

    protected static final String TEST_SRC_PROJECT_NAME = "metrics_tests";
    private static final String TEST_SRC_ORG_NAME = "intg_tests";
    protected static final String TEST_SRC_PACKAGE_NAME = "metrics_tests";
    private static final String PATH_SEPARATOR = "/";
    private static final String MODULE_ID = TEST_SRC_ORG_NAME + PATH_SEPARATOR + TEST_SRC_PACKAGE_NAME + ":0.0.1";

    protected static final String MOCK_CLIENT_OBJECT_NAME = TEST_SRC_ORG_NAME + PATH_SEPARATOR + TEST_SRC_PACKAGE_NAME
            + "/MockClient";
    protected static final String OBSERVABLE_ADDER_OBJECT_NAME = TEST_SRC_ORG_NAME + PATH_SEPARATOR +
            TEST_SRC_PACKAGE_NAME + "/ObservableAdder";
    private static final String SRC_OBJECT_NAME = "src.object.name";
    private static final String SRC_FUNCTION_NAME = "src.function.name";
    private static final String ENTRYPOINT_FUNCTION_MODULE = "entrypoint.function.module";
    private static final String ENTRYPOINT_FUNCTION_NAME = "entrypoint.function.name";
    private static final String SRC_SERVICE_RESOURCE = "src.service.resource";
    private static final String SRC_RESOURCE_PATH = "src.resource.path";
    private static final String SRC_RESOURCE_ACCESSOR = "src.resource.accessor";
    private static final String LISTENER_NAME = "listener.name";
    private static final String ENTRYPOINT_SERVICE_NAME = "entrypoint.service.name";
    private static final String ENTRYPOINT_RESOURCE_ACCESSOR = "entrypoint.resource.accessor";
    private static final String PROTOCOL = "protocol";
    private static final String HTTP_URL = "http.url";
    private static final String HTTP_METHOD = "http.method";

    @BeforeClass(alwaysRun = true)
    public void setup() throws Exception {
        super.setupServer(TEST_SRC_PROJECT_NAME, TEST_SRC_PACKAGE_NAME, new int[]{10090, 10091, 10092, 10093});
    }

    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        super.cleanupServer();
    }

    /**
     * Get all the collected metrics from the test service.
     *
     * @return All the collected metrics.
     * @throws IOException if fetching metrics from the test service fails
     */
    private Metrics getMetrics() throws IOException {
        String requestUrl = "http://localhost:10090/metricsRegistry/getMetrics";
        String data = HttpClientRequest.doPost(requestUrl, "", Collections.emptyMap()).getData();
        Type type = new TypeToken<Metrics>() {
        }.getType();
        return new Gson().fromJson(data, type);
    }

    /**
     * Filter metrics by a tag and the expected value of the tag.
     * If the metric should be filtered by absence of a particular tag, null should be passed as the value.
     *
     * @param metrics The list of metrics to be filtered
     * @param key     The key of the tag
     * @param value   The value of the tag
     * @return The filtered metrics list
     */
    private Metrics filterByTag(Metrics metrics, String key, String value) {
        Metrics filteredMetrics = new Metrics();
        filteredMetrics.addAllCounters(filterByTag(metrics.getCounters(), key, value));
        filteredMetrics.addAllGauges(filterByTag(metrics.getGauges(), key, value));
        filteredMetrics.addAllPolledGauges(filterByTag(metrics.getPolledGauges(), key, value));
        return filteredMetrics;
    }

    /**
     * Filter a list of a particular type of metric by a tag and the expected value of the tag.
     * If the metric should be filtered by absence of a particular tag, null should be passed as the value.
     *
     * @param metrics The list of metrics to be filtered
     * @param key     The key of the tag
     * @param value   The value of the tag
     * @param <M>     The type of metric to filter
     * @return The filtered metrics list
     */
    private <M extends MockMetric> List<M> filterByTag(List<M> metrics, String key, String value) {
        return metrics.stream()
                .filter(metric -> {
                    Optional<Tag> tag = metric.getId().getTags().stream()
                            .filter(t -> Objects.equals(t.getKey(), key))
                            .findFirst();
                    return (tag.isEmpty() && value == null)
                            || (tag.isPresent() && Objects.equals(tag.get().getValue(), value));
                })
                .collect(Collectors.toList());
    }

    /**
     * Test the metrics generated for a particular function invocation.
     *
     * @param allMetrics         All the metrics collected from Metrics Registry
     * @param invocationPosition The invocation position of the function invocation
     * @param invocationCount    The number of times the function should have been called
     * @param additionalTags     Additional tags that should be present in the metrics
     */
    private void testFunctionMetrics(Metrics allMetrics, String invocationPosition, long invocationCount,
                                     Tag... additionalTags) {
        testFunctionMetrics(allMetrics, invocationPosition, invocationCount, additionalTags, null);
    }

    /**
     * Test the metrics generated for a particular function invocation for a given start and end tag set.
     *
     * @param allMetrics                All the metrics collected from Metrics Registry
     * @param invocationPosition        The invocation position of the function invocation
     * @param invocationCount           The number of times the function should have been called
     * @param startObservationTags      tags at the observation start which should be present in the metrics
     * @param additionalObservationTags tags added additionally which should be present in the metrics
     */
    private void testFunctionMetrics(Metrics allMetrics, String invocationPosition, long invocationCount,
                                     Tag[] startObservationTags, Tag[] additionalObservationTags) {
        Metrics functionMetrics = filterByTag(allMetrics, "src.position", invocationPosition);

        Set<Tag> startTags = new HashSet<>(Arrays.asList(startObservationTags));
        startTags.add(Tag.of("src.module", MODULE_ID));
        startTags.add(Tag.of("src.position", invocationPosition));

        Set<Tag> endTags = new HashSet<>(startTags);
        if (additionalObservationTags != null) {
            endTags.addAll(Arrays.asList(additionalObservationTags));
        }
        testFunctionGauges(invocationCount, functionMetrics, startTags, endTags);
        testFunctionCounters(invocationCount, functionMetrics, endTags);
        Assert.assertEquals(functionMetrics.getPolledGauges().size(), 0);
    }

    /**
     * Test the counter metrics generated for a particular function invocation.
     *
     * @param invocationCount The number of times the function should have been called
     * @param functionMetrics All the metrics generated for the function invocation
     * @param tags            Tags that should be present in all the metrics
     */
    private void testFunctionCounters(long invocationCount, Metrics functionMetrics, Set<Tag> tags) {
        Assert.assertEquals(functionMetrics.getCounters().stream()
                        .map(gauge -> gauge.getId().getName())
                        .collect(Collectors.toSet()),
                new HashSet<>(Arrays.asList("requests_total", "response_time_nanoseconds_total")));
        Assert.assertEquals(functionMetrics.getCounters().size(), 2);
        functionMetrics.getCounters().forEach(counter -> {
            Assert.assertEquals(counter.getId().getTags(), tags);
            if (Objects.equals(counter.getId().getName(), "requests_total")) {
                Assert.assertEquals(counter.getValue(), invocationCount);
            } else if (Objects.equals(counter.getId().getName(), "response_time_nanoseconds_total")) {
                if (invocationCount > 0) {
                    Assert.assertTrue(counter.getValue() > 0,
                            "response_time_nanoseconds_total expected to be greater than 0, but found "
                                    + counter.getValue());
                } else {
                    Assert.assertEquals(counter.getValue(), 0);
                }
            } else {
                Assert.fail("Unexpected metric " + counter.getId().getName());
            }
        });
    }

    /**
     * Test the gauge metrics generated for a particular function invocation for given start and end tag set.
     *
     * @param invocationCount The number of times the function should have been called
     * @param functionMetrics All the metrics generated for the function invocation
     * @param startTags       Tags that should be present in metrics at observation start
     * @param endTags         Tags that should be present in metrics at observation end
     */
    private void testFunctionGauges(long invocationCount, Metrics functionMetrics, Set<Tag> startTags,
                                    Set<Tag> endTags) {
        Assert.assertEquals(functionMetrics.getGauges().stream()
                        .map(gauge -> gauge.getId().getName())
                        .collect(Collectors.toSet()),
                new HashSet<>(Arrays.asList("response_time_seconds", "inprogress_requests")));
        Assert.assertEquals(functionMetrics.getGauges().size(), 2);
        functionMetrics.getGauges().forEach(gauge -> {
            if (Objects.equals(gauge.getId().getName(), "response_time_seconds")) {
                testFunctionResponseTimeGaugeMetrics(invocationCount, endTags, gauge);
            } else if (Objects.equals(gauge.getId().getName(), "inprogress_requests")) {
                // Creating a new tag set without error metric as it is added after the observation is started and
                // hence not included in progress metrics
                Set<Tag> inProgressGaugeTags = new HashSet<>(startTags);
                inProgressGaugeTags.remove(Tag.of("error", "true"));

                Assert.assertEquals(gauge.getId().getTags(), inProgressGaugeTags);
                Assert.assertEquals(gauge.getCount(), invocationCount * 2);
                Assert.assertEquals(gauge.getSum(), (double) invocationCount);
                Assert.assertEquals(gauge.getValue(), 0.0);
                Assert.assertEquals(gauge.getSnapshots().length, 0);
            } else {
                Assert.fail("Unexpected metric " + gauge.getId().getName());
            }
        });
    }

    /**
     * Test a response time gauge of a function invocation.
     *
     * @param invocationCount The number of times the function should have been called
     * @param tags            Tags that should be present in metrics
     * @param gauge           The gauge to be tested
     */
    private void testFunctionResponseTimeGaugeMetrics(long invocationCount, Set<Tag> tags, MockGauge gauge) {
        Assert.assertEquals(gauge.getId().getTags(), tags);
        Assert.assertEquals(gauge.getCount(), invocationCount);
        if (invocationCount > 0) {
            Assert.assertTrue(gauge.getSum() > 0, "Expected sum of response_time_seconds "
                    + "to be greater than 0, but got " + gauge.getSum());
            Assert.assertTrue(gauge.getValue() > 0, "Expected value of response_time_seconds "
                    + "to be greater than 0, but got " + gauge.getValue());
        } else {
            Assert.assertEquals(gauge.getSum(), 0);
            Assert.assertEquals(gauge.getValue(), 0);
        }

        // Testing all the snapshots recorded in the gauge
        Assert.assertEquals(gauge.getSnapshots().length, 3);
        for (Snapshot snapshot : gauge.getSnapshots()) {
            testFunctionResponseTimeGaugeSnapshot(invocationCount, snapshot);
        }
    }

    /**
     * Test a particular snapshot recorded in a response time gauge of a function invocation.
     *
     * @param invocationCount The number of times the function should have been called
     * @param snapshot        The snapshot to test
     */
    private void testFunctionResponseTimeGaugeSnapshot(long invocationCount, Snapshot snapshot) {
        List<Duration> durations = Arrays.asList(Duration.ofSeconds(10), Duration.ofMinutes(1),
                Duration.ofMinutes(5));
        Assert.assertTrue(durations.contains(snapshot.getTimeWindow()), "time window "
                + snapshot.getTimeWindow() + " of snapshot not equal to either one of " + durations);

        if (invocationCount > 0) {
            Assert.assertTrue(snapshot.getMax() > 0, "Expected max of "
                    + snapshot.getTimeWindow().getSeconds() + " seconds time window to be greater than 0,"
                    + " but got " + snapshot.getMax());
            Assert.assertTrue(snapshot.getMin() > 0, "Expected min of "
                    + snapshot.getTimeWindow().getSeconds() + " seconds time window to be greater than 0,"
                    + " but got " + snapshot.getMin());
            Assert.assertTrue(snapshot.getMean() > 0, "Expected mean of "
                    + snapshot.getTimeWindow().getSeconds() + " seconds time window to be greater than 0,"
                    + " but got " + snapshot.getMean());
        } else {
            Assert.assertEquals(snapshot.getMax(), 0);
            Assert.assertEquals(snapshot.getMin(), 0);
            Assert.assertEquals(snapshot.getMean(), 0);
        }
        Assert.assertTrue(snapshot.getStdDev() >= 0, "Expected standard deviation of "
                + snapshot.getTimeWindow().getSeconds() + " seconds time window to be greater than 0,"
                + " but got " + snapshot.getStdDev());

        // Testing all the percentiles of the current snapshot
        Assert.assertEquals(snapshot.getPercentileValues().length, 7);
        for (PercentileValue percentileValue : snapshot.getPercentileValues()) {
            testFunctionResponseTimeGaugeSnapshotPercentile(invocationCount, percentileValue);
        }
    }

    /**
     * Test a particular percentile value of a response time snapshot recorded in a gauge of a function invocation.
     *
     * @param invocationCount The number of times the function should have been called
     * @param percentileValue The percentile value to test
     */
    private void testFunctionResponseTimeGaugeSnapshotPercentile(long invocationCount,
                                                                 PercentileValue percentileValue) {
        List<Double> expectedPercentiles = Arrays.asList(0.33, 0.5, 0.66, 0.75, 0.95, 0.99, 0.999);
        Assert.assertTrue(expectedPercentiles.contains(percentileValue.getPercentile()),
                "percentile " + percentileValue.getPercentile()
                        + " is not one of the expected percentiles " + expectedPercentiles);
        if (invocationCount > 0) {
            Assert.assertTrue(percentileValue.getValue() > 0, "percentile "
                    + percentileValue.getPercentile() + " expected to be greater than 0, but found "
                    + percentileValue.getValue());
        } else {
            Assert.assertEquals(percentileValue.getValue(), 0);
        }
    }

    @Test
    public void testMainFunction() throws Exception {
        String fileName = "01_main_function.bal";

        Metrics metrics = this.getMetrics();
        testFunctionMetrics(metrics, fileName + ":19:1", 1,
                Tag.of(SRC_FUNCTION_NAME, "main"),
                Tag.of("src.main", "true"),
                Tag.of(ENTRYPOINT_FUNCTION_MODULE, MODULE_ID),
                Tag.of(ENTRYPOINT_FUNCTION_NAME, "main")
        );
        testFunctionMetrics(metrics, fileName + ":24:24", 1,
                Tag.of(SRC_OBJECT_NAME, OBSERVABLE_ADDER_OBJECT_NAME),
                Tag.of(SRC_FUNCTION_NAME, "getSum"),
                Tag.of(ENTRYPOINT_FUNCTION_MODULE, MODULE_ID),
                Tag.of(ENTRYPOINT_FUNCTION_NAME, "main")
        );
        testFunctionMetrics(metrics, fileName + ":38:12", 3,
                Tag.of(SRC_FUNCTION_NAME, "calculateSumWithObservability"),
                Tag.of(ENTRYPOINT_FUNCTION_MODULE, MODULE_ID),
                Tag.of(ENTRYPOINT_FUNCTION_NAME, "main")
        );
    }

    @Test
    public void testResourceFunction() throws Exception {
        String fileName = "02_resource_function.bal";
        String serviceName = "/testServiceOne";
        String resourceName = "/resourceOne";

        HttpResponse httpResponse = HttpClientRequest.doPost(
                "http://localhost:10091" + serviceName + resourceName, "15", Collections.emptyMap());
        Assert.assertEquals(httpResponse.getResponseCode(), 500);
        Assert.assertEquals(httpResponse.getData(), "Test Error");
        Thread.sleep(1000);

        Metrics metrics = this.getMetrics();
        testFunctionMetrics(metrics, fileName + ":23:5", 1,
                Tag.of(SRC_SERVICE_RESOURCE, "true"),
                Tag.of(SRC_OBJECT_NAME, serviceName),
                Tag.of(SRC_RESOURCE_PATH, resourceName),
                Tag.of(SRC_RESOURCE_ACCESSOR, "post"),
                Tag.of(LISTENER_NAME, SERVER_CONNECTOR_NAME),
                Tag.of(ENTRYPOINT_SERVICE_NAME, serviceName),
                Tag.of(ENTRYPOINT_FUNCTION_NAME, resourceName),
                Tag.of(ENTRYPOINT_RESOURCE_ACCESSOR, "post"),
                Tag.of(ENTRYPOINT_FUNCTION_MODULE, MODULE_ID),
                Tag.of(PROTOCOL, "http"),
                Tag.of(HTTP_URL, serviceName + resourceName),
                Tag.of(HTTP_METHOD, "POST"),
                Tag.of("error", "true")
        );
        testFunctionMetrics(metrics, fileName + ":24:24", 1,
                Tag.of("src.client.remote", "true"),
                Tag.of("error", "true"),
                Tag.of(SRC_OBJECT_NAME, MOCK_CLIENT_OBJECT_NAME),
                Tag.of(SRC_FUNCTION_NAME, "callWithPanic"),
                Tag.of(ENTRYPOINT_FUNCTION_MODULE, MODULE_ID),
                Tag.of(ENTRYPOINT_FUNCTION_NAME, resourceName),
                Tag.of(ENTRYPOINT_RESOURCE_ACCESSOR, "post"),
                Tag.of(ENTRYPOINT_SERVICE_NAME, serviceName)
        );
        testFunctionMetrics(metrics, fileName + ":29:20", 1,
                Tag.of("src.client.remote", "true"),
                Tag.of("error", "true"),
                Tag.of(SRC_OBJECT_NAME, MOCK_CLIENT_OBJECT_NAME),
                Tag.of(SRC_FUNCTION_NAME, "callWithErrorReturn"),
                Tag.of(ENTRYPOINT_FUNCTION_MODULE, MODULE_ID),
                Tag.of(ENTRYPOINT_FUNCTION_NAME, resourceName),
                Tag.of(ENTRYPOINT_RESOURCE_ACCESSOR, "post"),
                Tag.of(ENTRYPOINT_SERVICE_NAME, serviceName)
        );
    }

    @Test
    public void testPathsWithEscapeCharacters() throws Exception {
        String fileName = "02_resource_function.bal";
        String serviceName = "/this\\-is\\-service\\-path/still\\-service\\-path";
        String resourceName = "/this\\-is\\-resource\\-function/still\\-resource\\-function";
        String serviceNameUnescaped = "/this-is-service-path/still-service-path";
        String resourceNameUnescaped = "/this-is-resource-function/still-resource-function";
        if (!serviceNameUnescaped.equals(Utils.unescapeBallerina(serviceNameUnescaped)) ||
                !resourceNameUnescaped.equals(Utils.unescapeBallerina(resourceNameUnescaped))) {
            throw new AssertionError("Unescaped path is not correct");
        }

        HttpResponse httpResponse = HttpClientRequest.doPost("http://localhost:10093" + serviceName +
                resourceName, "15", Collections.emptyMap());
        Assert.assertEquals(httpResponse.getResponseCode(), 200);
        Assert.assertEquals(httpResponse.getData(), "Invocation Successful");
        Thread.sleep(1000);

        Metrics metrics = this.getMetrics();
        testFunctionMetrics(metrics, fileName + ":83:5", 1,
                Tag.of(SRC_SERVICE_RESOURCE, "true"),
                Tag.of(SRC_OBJECT_NAME, serviceNameUnescaped),
                Tag.of(PROTOCOL, "http"),
                Tag.of(HTTP_URL, serviceName + resourceName),
                Tag.of(ENTRYPOINT_FUNCTION_MODULE, MODULE_ID),
                Tag.of(SRC_RESOURCE_ACCESSOR, "post"),
                Tag.of(ENTRYPOINT_RESOURCE_ACCESSOR, "post"),
                Tag.of(ENTRYPOINT_SERVICE_NAME, serviceNameUnescaped),
                Tag.of(ENTRYPOINT_FUNCTION_NAME, resourceNameUnescaped),
                Tag.of(LISTENER_NAME, "testobserve_listener"),
                Tag.of(SRC_RESOURCE_PATH, resourceNameUnescaped),
                Tag.of(HTTP_METHOD , "POST")
        );
    }

    @Test
    public void testWorkers() throws Exception {
        String fileName = "02_resource_function.bal";
        String serviceName = "/testServiceOne";
        String resourceName = "/resourceTwo";

        HttpResponse httpResponse = HttpClientRequest.doPost("http://localhost:10091" + serviceName +
                resourceName, "15", Collections.emptyMap());
        Assert.assertEquals(httpResponse.getResponseCode(), 200);
        Assert.assertEquals(httpResponse.getData(), "Invocation Successful");
        Thread.sleep(1000);

        Metrics metrics = this.getMetrics();
        testFunctionMetrics(metrics, fileName + ":34:5", 1,
                Tag.of(SRC_SERVICE_RESOURCE, "true"),
                Tag.of(SRC_OBJECT_NAME, serviceName),
                Tag.of(PROTOCOL, "http"),
                Tag.of(HTTP_URL, serviceName + resourceName),
                Tag.of(ENTRYPOINT_FUNCTION_MODULE, MODULE_ID),
                Tag.of(SRC_RESOURCE_ACCESSOR, "post"),
                Tag.of(ENTRYPOINT_RESOURCE_ACCESSOR, "post"),
                Tag.of(ENTRYPOINT_SERVICE_NAME, serviceName),
                Tag.of(ENTRYPOINT_FUNCTION_NAME, resourceName),
                Tag.of(LISTENER_NAME, "testobserve_listener"),
                Tag.of(SRC_RESOURCE_PATH, resourceName),
                Tag.of(HTTP_METHOD , "POST")
        );
        testFunctionMetrics(metrics, fileName + ":66:15", 1,
                Tag.of("src.worker", "true"),
                Tag.of(ENTRYPOINT_FUNCTION_MODULE, MODULE_ID),
                Tag.of(SRC_FUNCTION_NAME, "w1"),
                Tag.of(ENTRYPOINT_RESOURCE_ACCESSOR, "post"),
                Tag.of(ENTRYPOINT_SERVICE_NAME, serviceName),
                Tag.of(ENTRYPOINT_FUNCTION_NAME, resourceName)
        );
        testFunctionMetrics(metrics, fileName + ":71:15", 1,
                Tag.of("src.worker", "true"),
                Tag.of(ENTRYPOINT_FUNCTION_MODULE, MODULE_ID),
                Tag.of(SRC_FUNCTION_NAME, "w2"),
                Tag.of(ENTRYPOINT_RESOURCE_ACCESSOR, "post"),
                Tag.of(ENTRYPOINT_SERVICE_NAME, serviceName),
                Tag.of(ENTRYPOINT_FUNCTION_NAME, resourceName)
        );
        testFunctionMetrics(metrics, fileName + ":36:20", 1,
                Tag.of("src.client.remote", "true"),
                Tag.of(SRC_OBJECT_NAME, "ballerina/testobserve/Caller"),
                Tag.of(SRC_FUNCTION_NAME, "respond"),
                Tag.of(ENTRYPOINT_FUNCTION_MODULE, MODULE_ID),
                Tag.of(ENTRYPOINT_RESOURCE_ACCESSOR, "post"),
                Tag.of(ENTRYPOINT_SERVICE_NAME, serviceName),
                Tag.of(ENTRYPOINT_FUNCTION_NAME, resourceName)
        );
    }

    @Test
    public void testCustomMetricTags() throws Exception {
        String fileName = "03_custom_metric_tags.bal";
        String serviceName = "/testServiceTwo";
        String resourceName = "/testAddTagToMetrics";

        HttpResponse httpResponse = HttpClientRequest.doPost("http://localhost:10092" + serviceName +
                resourceName, "15", Collections.emptyMap());
        Assert.assertEquals(httpResponse.getResponseCode(), 200);
        Assert.assertEquals(httpResponse.getData(), "Invocation Successful");
        Thread.sleep(1000);

        Tag[] startTags = {
                Tag.of(SRC_SERVICE_RESOURCE, "true"),
                Tag.of(SRC_OBJECT_NAME, serviceName),
                Tag.of(ENTRYPOINT_FUNCTION_MODULE, MODULE_ID),
                Tag.of(SRC_RESOURCE_ACCESSOR, "post"),
                Tag.of(ENTRYPOINT_RESOURCE_ACCESSOR, "post"),
                Tag.of(LISTENER_NAME, "testobserve_listener"),
                Tag.of(ENTRYPOINT_FUNCTION_NAME, resourceName),
                Tag.of(ENTRYPOINT_SERVICE_NAME, serviceName),
                Tag.of(SRC_RESOURCE_PATH, resourceName),
                Tag.of(PROTOCOL, "http"),
                Tag.of(HTTP_URL, serviceName + resourceName),
                Tag.of(HTTP_METHOD , "POST")
        };
        Tag[] endTags = {Tag.of("metric", "Metric Value")};

        Metrics metrics = this.getMetrics();
        testFunctionMetrics(metrics, fileName + ":22:5", 1, startTags, endTags);
    }

}
