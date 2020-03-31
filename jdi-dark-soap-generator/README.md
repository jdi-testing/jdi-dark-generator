In my search for solution to https://github.com/jdi-testing/jdi-dark/issues/264 I tried 4 different tools: wsimport, axis2, cxf, jaxb2.


| tool | object generation | client generation | is readable generated code | easy to configure | improvements|
|------|-------------------|-------------------|----------------------------|-------------------|-------------|
|WSimport| yes             | yes               | yes                        |yes                | connect generated client with jdi-dark|
| Axis2  | yes             | no,stubs instead  | absolutely not             | no                | Better to skip this variant,generated code awful|
| CXF    | yes             | no                | yes                        | yes               | client to communicate with SOAP service, connect client to jdi-dark|
| Jaxb2   | yes             | no, but easy to create (Spring) | yes          | yes               | create client:create methods + confugure beans,connect to jdi-dark|


**1. WSimport:**
(I leave example in this branch to tool uasge with Yandex Speller)

In my opinion it's the best option to use because:
1. Easy in usage and confuguration with Maven. ( in fact ONLY MAVEN CONFIGURATION REQUIRED)
2. Supports providing .wsdl file via URL/via path to .wsdl file on your computer.
3. Generates ready-to-use client-side of SOAP-service: all required objects to use, service class, SOAP service class,
supports chain-of-responsibility pattern to requests with params.
4. Ready to use, literally
<br><br>
  **How to use:** <br>
        1. mvn jaxws:generate <br>
        2. create entry point: test/ main ( to fast example I choose second: create class Main somewhere around generated classes)<br>
        3. Copy and paste code to it:<br>

      ```java
    public static void main(String[] args) {
        CheckTextRequest request = new CheckTextRequest();
        request.withLang("ru")
                .withText("кортошка");
        CheckTextResponse response = new SpellService().getSpellServiceSoap().checkText(request);

        response.getSpellResult().getError().forEach(spellError -> {
            System.out.println("Word in request: "+ spellError.getWord());
            System.out.println("Suggested by service word: "+spellError.getS());
            System.out.println("Col: "+spellError.getCol());
            System.out.println("Len: "+spellError.getLen());
            System.out.println("Pos: "+spellError.getPos());
            System.out.println("Row:"+spellError.getRow());

        });
    } 
    
 ![alt_text](https://github.com/jdi-testing/jdi-dark-generator/blob/soap_generator_investigating/jdi-dark-soap-generator/Capture.PNG)
<br><br>
  **What should be improved (how to connect with jdi-dark):**<br><br>
      1. WSimport generates it's own architecture for client: service class (service initialization) and SOAPService interface(here placed @WebMethod to make requests - ***should be improved to connect with Jdi-dark SOAP architecture, my thoughts***:
         <br>
          - may be parse with Reflection API this class & interface,extract needed to us info and based on it create jdi objects?
          <br>
          - may be build soap architecture in jdi-dark in way to use of ready-to-use generated services?
          <br>
          - may be try to get into tool generation flow and change generating of service class and soap interface to custom (I don't know how real this opportunity, is code availabe? Is it readable?
<br><br>
**2. Axis2:**

Unreadable generated code, some addiditional dependecies in pom.xml, too difficult to configuration,too much work because of axis2 stubs - you can contact me, I will provide more info about it
<br><br>
**3. CXF:**

It is WSimport on minimals - will generate all required object to SOAP service (same object generator to WSimport, I suppose), but will not generate service class and SOAP service interface
<br>
Improvements as in WSimport - client to communicate with SOAP service

i.e. IT IS WSIMPORT WITHOUT GENERATING SERVICE CLASS && SOAP SERVICE INTERFACE
<br><br>
**4. Jaxb2:**

Spring-boot based generator, required to write client to communicate with service by hands from scratch, look at: https://spring.io/guides/gs/consuming-web-service/
to more info.<br> 
By the way,generates all objects to service usage
