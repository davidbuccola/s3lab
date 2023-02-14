package com.salesforce.dbuccola.s3lab;

import java.util.Arrays;

public class S3Lab {
    static final String DEFAULT_ENDPOINT = "http://127.0.0.1:9000";
    static final String DEFAULT_REGION = "us-east-1";
    static final String DEFAULT_USERNAME = "root";
    static final String DEFAULT_PASSWORD = "password";
    static final String BUCKET_NAME = "s3lab-bucket";
    static final String FILE_NAME_PREFIX = "s3lab-file";

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage("No command specified");
            System.exit(1);
        }

        String[] commandSpecificArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (args[0]) {
            case "testPooling":
                TestPooling.main(commandSpecificArgs);
                break;

            default:
                usage("Unknown command - " + args[0]);
                System.exit(1);
        }
    }

    private static void usage(String errorMessage) {
        if (errorMessage != null) {
            System.err.println(errorMessage);
        }

        System.out.println(
                "usage: activitytool <command> [options]\n\n" +
                "Commands:\n" +
                "    testPooling       - Tests connection pooling behavior with lots of getObject calls");
    }
}
