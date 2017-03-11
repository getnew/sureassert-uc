package test4;
import org.sureassert.uc.annotation.Exemplar;

public enum CardSuite {
	
	SPADE("black"),
	CLUB("black"),
	HEART("red"),
	DIAMOND("red");
	
	private String colour;
	
	private static final String test = "";
	
	static String test2;
	static {
		test2 = "abc";
	}
	
	CardSuite(String colour) {
		System.out.println(test);
		this.colour = colour;
	} 

	@Exemplar(instance="CardSuite.SPADE", expect="'blackabc'")
	public String getColour() {
		return colour + test2;  
	} 
	
	@Exemplar(instance="CardSuite.SPADE")
	public int throwException() {  

		_sauc.SAInterceptor.instance.stubExecuted(33,37,"test4.CardSuite");  System.out.println("stub executed")   ;    




		return 5;
	}
}  
