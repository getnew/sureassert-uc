package testPackage;

import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.Exemplars;



public class Consignment {

	private int consignmentID;
	private Address address;
	private int deliveryStatus;
	
	@Exemplar(name="Consignment/preinit", args={"1"})
	public Consignment(int consignmentID) {
		this.consignmentID = consignmentID;
	} 
	
	@Exemplar(instance="Consignment/preinit!", args={"'5a'","'AB1234'"}, instanceout="Consignment/1")
	public void setAddress(String houseNumber, String postalCode) {
		
		PostalCodeService postalCodeService = new PostalCodeService(); 
		Address address = postalCodeService.getAddress(houseNumber, postalCode);
		if (address == null) {
			address = new Address(houseNumber, "Unknown", "Unknown", "Unknown", postalCode, "Unknown");
		}
	}
	   
	@Exemplars(set={
	@Exemplar(args={"5"}, expect="1"),
	@Exemplar(name="deliver1", args={"-1"}, before="", expectexception="IllegalArgumentException", 
			expect="'oh dear': (retval.getMessage().contains('deliveryFirmID'))"),
	@Exemplar(template="deliver1") })
	public int deliver(int deliveryFirmID) {
		   
		if (deliveryFirmID < 0)
			throw new IllegalArgumentException("deliveryFirmID must be >= 0");
		 
		deliveryStatus = 1;
		return deliveryStatus; 
	}  
	
//	@Exemplar(expect="")
	public float doSomething() {
	  System.out.println("oijioj");
	  int x = 5;
	  x++;
	  return x;
	}
} 
