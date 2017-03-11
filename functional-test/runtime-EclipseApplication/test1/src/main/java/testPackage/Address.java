package testPackage;

public class Address {

	private final String houseNumber;
	private final String addressLine1;
	private final String addressLine2;
	private final String region;
	private final String postalCode;
	private final String country;
	
	public Address(String houseNumber, String addressLine1, String addressLine2, String region, String postalCode, String country) {
		super();
		this.houseNumber = houseNumber;
		this.addressLine1 = addressLine1;
		this.addressLine2 = addressLine2;
		this.region = region;
		this.postalCode = postalCode;
		this.country = country;
	}
	
	public String getHouseNumber() {
		return houseNumber;
	}
	
	public String getAddressLine1() {
		return addressLine1;
	}
	
	public String getAddressLine2() {
		return addressLine2;
	}
	
	public String getRegion() {
		return region;
	}
	
	public String getPostalCode() {
		return postalCode;
	}
	
	public String getCountry() {
		return country;
	}
}
