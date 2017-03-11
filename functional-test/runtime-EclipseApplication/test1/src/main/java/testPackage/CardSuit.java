package testPackage;

import org.sureassert.uc.annotation.Exemplar;


public enum CardSuit {
	
	SPADE("black", true), CLUB("black", false), HEART("red", false), DIAMOND("red", false);
	
	private String colour;
	private boolean priority;
	
//	@Exemplar(args={"'SPADE'", "0", "null"}, expect="")
	CardSuit(String colour) {
		this(colour, false);
	}  
	                    
//	@Exemplar(args={"'SPADE'", "0", "null", "false"}, expect="")
	CardSuit(String colour, boolean priority) {
		this.colour = colour;
		this.priority = priority; 
	} 
	
	//@Exemplar(instance="CardSuit.SPADE", expect="")
	public String doSomething() {
		return "test";  
	}
}
