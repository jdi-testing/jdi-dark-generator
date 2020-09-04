package com.epam.jdi.generator.v2;

import com.epam.jdi.generator.v2.cli.Generate;
import io.airlift.airline.Cli;

public class SwaggerCodegenJDI {

public static void main(String[] args) {
    @SuppressWarnings("unchecked") Cli.CliBuilder<Runnable> builder = Cli.<Runnable>builder("dark-generator").withDescription("JDI Dark Generator").withCommands(Generate.class);
    
    final Runnable parse = builder.build().parse(args);
    parse.run();
}
}
