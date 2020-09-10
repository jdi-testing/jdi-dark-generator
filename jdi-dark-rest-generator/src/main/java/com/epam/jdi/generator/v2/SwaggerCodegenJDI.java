package com.epam.jdi.generator.v2;

import com.epam.jdi.generator.v2.cli.Generate;
import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.help.Help;

//If Jar name will be updated the name here should be changed as well

@com.github.rvesse.airline.annotations.Cli(name = "java -jar DarkGenerator.jar", description = "Provides CLI to generate JDI Dark annotated classes from Open API/Swagger specification",
        defaultCommand = Help.class, commands = {
        Generate.class,
        Help.class
})
public class SwaggerCodegenJDI {

public static void main(String[] args) {
    Cli<Runnable> cli = new Cli<>(SwaggerCodegenJDI.class);
    
    final Runnable parse = cli.parse(args);
    parse.run();
    
}
}