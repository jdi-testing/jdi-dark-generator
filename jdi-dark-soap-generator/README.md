# JDI Dark Soap Objects Generator

## Generating

To generate jdi-dark soap objects for your wsdl url:
 - modify pom.xml: 
    * set wsdlUrls, for example:
    ```
      <wsdlUrls>
        <wsdlUrl>https://speller.yandex.net/services/spellservice?WSDL</wsdlUrl>
      </wsdlUrls>
    ```
    * set package, for example:
    ```
    <packageName>com.epam.jdi.soap.net.yandex.speller.services.spellservice</packageName>
    ```
    
 - run maven plugin jaxws:wsimport
 
 
