package com.salesforce.dbuccola.s3lab;

import static com.salesforce.dbuccola.s3lab.S3Lab.BUCKET_NAME;
import static com.salesforce.dbuccola.s3lab.S3Lab.DEFAULT_ENDPOINT;
import static com.salesforce.dbuccola.s3lab.S3Lab.DEFAULT_PASSWORD;
import static com.salesforce.dbuccola.s3lab.S3Lab.DEFAULT_REGION;
import static com.salesforce.dbuccola.s3lab.S3Lab.DEFAULT_USERNAME;
import static com.salesforce.dbuccola.s3lab.S3Lab.FILE_NAME_PREFIX;
import static java.lang.Integer.parseInt;
import static net.davidbuccola.commons.CommandLineUtils.parseCommandLine;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.endpointdiscovery.DaemonThreadFactory;
import com.amazonaws.metrics.RequestMetricCollector;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.AWSRequestMetrics;
import com.amazonaws.util.AWSRequestMetrics.Field;
import com.amazonaws.util.TimingInfo;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TestPooling {

    private final Logger log = LoggerFactory.getLogger(TestPooling.class);

    private final AmazonS3 amazonS3;
    private final int concurrency;
    private final int busyDuration;
    private final int idleDuration;

    private final AtomicReference<AWSRequestMetrics> mostRecentMetrics = new AtomicReference<>(null);
    private final AtomicInteger requestsInLastInterval = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        CommandLine command = parseCommandLine("s3lab testPooling", getOptions(), args);
        new TestPooling(command).run();
    }

    private TestPooling(CommandLine command) {
        String endpoint = command.getOptionValue("endpoint", DEFAULT_ENDPOINT);
        String region = command.getOptionValue("region", DEFAULT_REGION);
        String username = command.getOptionValue("username", DEFAULT_USERNAME);
        String password = command.getOptionValue("password", DEFAULT_PASSWORD);
        int maxConnections = parseInt(command.getOptionValue("max-connections", "150"));
        int connectionTimeout = parseInt(command.getOptionValue("connectionTimeout", "10000"));

        concurrency = parseInt(command.getOptionValue("concurrency", "1000"));
        busyDuration = parseInt(command.getOptionValue("busy-duration", "10"));
        idleDuration = parseInt(command.getOptionValue("idle-duration", "100"));

        amazonS3 = AmazonS3ClientBuilder
                .standard()
                .withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(username, password)))
                .withClientConfiguration(new ClientConfiguration()
                        .withMaxConnections(maxConnections)
                        .withConnectionTimeout(connectionTimeout))
                .withMetricsCollector(new RequestMetricCollector() {
                    @Override
                    public void collectMetrics(Request<?> request, Response<?> response) {
                        mostRecentMetrics.set(request.getAWSRequestMetrics());
                        requestsInLastInterval.addAndGet(1);
                    }
                })
                .build();
    }

    private void run() throws Exception {
        if (!amazonS3.doesBucketExistV2(BUCKET_NAME)) {
            amazonS3.createBucket(BUCKET_NAME);
        }

        String sampleData = IOUtils.toString(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("constitution.txt")), StandardCharsets.UTF_8);
        for (int i = 0; i < concurrency; i++) {
            if (!amazonS3.doesObjectExist(BUCKET_NAME, FILE_NAME_PREFIX + i)) {
                amazonS3.putObject(BUCKET_NAME, FILE_NAME_PREFIX + i, sampleData);
            }
        }

        // Periodically log connection pool stats.
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(concurrency + 1, new DaemonThreadFactory());
        scheduler.scheduleWithFixedDelay(() -> {
            AWSRequestMetrics metrics = mostRecentMetrics.getAndSet(null);
            if (metrics != null) {
                TimingInfo timingInfo = metrics.getTimingInfo();
                log.info(
                        requestsInLastInterval.getAndSet(0) + " requests handled, current pool={" +
                        timingInfo.getCounter(Field.HttpClientPoolPendingCount.name()).toString() + " pending, " +
                        timingInfo.getCounter(Field.HttpClientPoolAvailableCount.name()).toString() + " available, " +
                        timingInfo.getCounter(Field.HttpClientPoolLeasedCount.name()).toString() + " leased}");
            }
        }, 0, 1, TimeUnit.SECONDS);

        // Fire up concurrent test threads.
        for (int i = 0; i < concurrency; i++) {
            String fileName = FILE_NAME_PREFIX + i;
            scheduler.scheduleWithFixedDelay(() -> {
                try {
                    S3Object s3Object = amazonS3.getObject(BUCKET_NAME, fileName);
                    Thread.sleep(busyDuration);
                    log.debug("Consumed " + IOUtils.consume(s3Object.getObjectContent()) + " bytes");

                } catch (Exception e) {
                    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                        if (cause instanceof InterruptedException) {
                            return; // Already on the way down so no need to log anything
                        }
                    }

                    log.error("S3 getObject failed", e);
                    scheduler.shutdownNow();
                }
            }, 10, idleDuration, TimeUnit.MILLISECONDS);
        }

        scheduler.awaitTermination(1, TimeUnit.DAYS);
    }

    @SuppressWarnings("DuplicatedCode")
    private static Options getOptions() {
        Options options = new Options();
        options.addOption(Option.builder().longOpt("endpoint").hasArg().argName("endpoint")
                .desc("Endpoint").build());
        options.addOption(Option.builder().longOpt("region").hasArg().argName("region")
                .desc("Signing Region").build());
        options.addOption(Option.builder().longOpt("username").hasArg().argName("username")
                .desc("Username").build());
        options.addOption(Option.builder().longOpt("password").hasArg().argName("password")
                .desc("Password").build());
        options.addOption(Option.builder().longOpt("concurrency").hasArg().argName("count")
                .desc("The number of concurrent threads").type(Integer.class).build());
        options.addOption(Option.builder().longOpt("max-connections").hasArg().argName("count")
                .desc("The size of the connection pool").type(Integer.class).build());
        options.addOption(Option.builder().longOpt("connection-timeout").hasArg().argName("milliseconds")
                .desc("The maximum time to wait for a connection from the pool").type(Integer.class).build());
        options.addOption(Option.builder().longOpt("busy-duration").hasArg().argName("milliseconds")
                .desc("The amount of extra time to hold on to the connection").type(Integer.class).build());
        options.addOption(Option.builder().longOpt("idle-duration").hasArg().argName("milliseconds")
                .desc("The amount of time to pause before the next iteration").type(Integer.class).build());

        return options;
    }
}