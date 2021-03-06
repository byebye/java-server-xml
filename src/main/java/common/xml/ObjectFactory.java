//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.02.22 at 09:09:36 PM CET 
//


package common.xml;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the common.xml package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: common.xml
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link CarsInformation }
     * 
     */
    public CarsInformation createCarsInformation() {
        return new CarsInformation();
    }

    /**
     * Create an instance of {@link PersonType }
     * 
     */
    public PersonType createPersonType() {
        return new PersonType();
    }

    /**
     * Create an instance of {@link CarsInformation.Citizens }
     * 
     */
    public CarsInformation.Citizens createCarsInformationCitizens() {
        return new CarsInformation.Citizens();
    }

    /**
     * Create an instance of {@link CarsInformation.DeregisteredCars }
     * 
     */
    public CarsInformation.DeregisteredCars createCarsInformationDeregisteredCars() {
        return new CarsInformation.DeregisteredCars();
    }

    /**
     * Create an instance of {@link CarType }
     * 
     */
    public CarType createCarType() {
        return new CarType();
    }

    /**
     * Create an instance of {@link PersonType.CarsOwned }
     * 
     */
    public PersonType.CarsOwned createPersonTypeCarsOwned() {
        return new PersonType.CarsOwned();
    }

}
