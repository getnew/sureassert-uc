package testPackage;

import org.sureassert.uc.annotation.Exemplar;

public class TestClassC {
	
	private int var1; 
	private String var2;
	      
	@Exemplar(depends={"TestClassC/init"}, expect="12")
	public int getVar1() {
		return var1;  
	}

	@Exemplar(name="TestClassC/2", args={"10"})
	public TestClassC(int var1) {
		this(var1, "defaultStr"); 
		this.var1++;   
	}    
	
	@Exemplar(name="TestClassC/1", args={"5", "'1'"})
	public TestClassC(int var1, String var2) {
		this.var1 = var1; 
		this.var2 = var2; 
	}         
	 
	@Exemplar(name="init", instance="TestClassC/2", expect="12")
	public int init() {
		var1++; 
		return var1;
	}  
	
	@Exemplar(args="'test'")
	public static String testStr(String x) {
		return x;  
	}
	    
	@Exemplar(depends="TestClassC/2")
	public String toString() {
		return var1 + " | " + var2; 
	} 
	 
	//@Exemplar  //TODO: fix problems with build stop error reporting / persistent data
	public static void testRecursionError() { 
		
		int x = 5; 
		for (int i = 0; i < 10; ) { 
			; 
			//x++;   
		}     
	} 
}
