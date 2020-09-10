package com.epam.jdi.generator.v2;

import com.epam.jdi.generator.v2.cli.Generate;
import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.builder.CliBuilder;
import com.github.rvesse.airline.help.Help;

public class SwaggerCodegenJDI {

public static void main(String[] args) {
    
    //If Jar name will be updated the name here should be changed as well
    
    CliBuilder<Runnable> builder =
            Cli.<Runnable>builder("java -jar DarkGenerator.jar").withDescription("JDI Dark Codegen Generator").withCommands(Generate.class, Help.class).withDefaultCommand(Help.class);
    
    // We can enable command and option abbreviation, this allows users to only
    // type part of the group/command/option name provided that the portion they
    // type is unambiguous
    builder.withParser().withCommandAbbreviation().withOptionAbbreviation();
    final Runnable generatorCli = builder.build().parse(args);
    
    generatorCli.run();
    
}
}