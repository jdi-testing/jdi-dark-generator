package com.epam.jdi.generator.v2.core;

import io.swagger.codegen.v3.CodegenConstants;

/** Created by oksana_cherniavskaia on 01.09.2020. */
public class JavaCodegenConstantsJDI  {

  public static final String SERIALIZATION_LIBRARY = "serializationLibrary";
  public static final String SERIALIZATION_LIBRARY_DESC =
      "Serialization library (Default: Jackson)";
  public static final String DATE_LIBRARY = "dateLibrary";

  public static final String DATE_LIBRARY_DESC =
      "Option. Date library to use\n. joda - Joda (for legacy app only)\n. legacy - Legacy java.util.Date (if you really have a good reason not to use threetenbp\n. java8-localdatetime - Java 8 using LocalDateTime (for legacy app only)\n. java8 - Java 8 native JSR310 (preferred for jdk 1.8+) - note: this also sets \"java8\" to true\n. threetenbp - Backport of JSR310 (preferred for jdk < 1.8)\n";

  public static final String FULL_JAVA_UTIL = "fullJavaUtil";
  public static final String FULL_JAVA_UTIL_DESC =
      " whether to use fully qualified name for classes under java.util. This option only works for Java API client (Default: false)";

  public static final String WITH_XML_DESC =
      " whether to include support for application/xml content type and include XML annotations in the model (works with libraries that provide support for JSON and XML) (Default: false)";

  public static final String JAVA_8 = "java8";
  public static final String JAVA_8_DESC =
      " Option. Use Java8 classes instead of third party equivalents\ntrue - Use Java 8 classes such as Base64\nfalse - Various third party libraries as needed";

  public static final String OUTPUT_FOLDER = "outputFolder";

  public static final String JACKSON = "jackson";
  public static final String GSON = "gson";
  public static final String SERIALIZATION_LIBRARY_GSON = "gson";
  public static final String SERIALIZATION_LIBRARY_JACKSON = "jackson";

  public static final String THREETENBP_DATE_LIBRARY = "threetenbp";
  public static final String JAVA_8_DATE_LIBRARY = "java8";
  public static final String JAVA_8_LOCALDATETIME_DATE_LIBRARY = "java8-localdatetime";
  public static final String LEGACY_DATE_LIBRARY = "legacy";
  public static final String JODA_DATE_LIBRARY = "joda";
}
