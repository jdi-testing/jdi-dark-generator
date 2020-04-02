package com.epam.jdi.generator;

import io.airlift.airline.Cli;

public class SwaggerCodegenJDI {

    public static void main(String[] args) {
        @SuppressWarnings("unchecked")
        Cli.CliBuilder<Runnable> builder =
                Cli.<Runnable>builder("dark-generator")
                        .withDescription(
                                "JDI Dark Generator")
                        .withCommands(Generate.class);

        builder.build().parse(args).run();
    }
}
