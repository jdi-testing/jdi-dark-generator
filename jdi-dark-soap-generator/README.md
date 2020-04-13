# JDI Dark Soap Objects Generator

## Generating

To generate jdi-dark soap objects for your wsdl url:
 - modify pom.xml: 
    1. set wsdlUrls, for example:
      <wsdlUrls>
        <wsdlUrl>https://speller.yandex.net/services/spellservice?WSDL</wsdlUrl>
      </wsdlUrls>
    2. set package, for example:
    <packageName>com.epam.jdi.soap.net.yandex.speller.services.spellservice</packageName>
    
 - run maven plugin jaxws:wsimport
 
 
