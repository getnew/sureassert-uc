package testPackage;

import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.IgnoreTestCoverage;
 
@IgnoreTestCoverage 
public class TestInlineMock {
	
	//TODO: preferences: 
	// enter license/email; 
	// enable source stubs
	  
	@Exemplar(args="'arg1'", stubs="stub1=getStubVar()")
	public String test(String in) {
		   
	 	in  = in + " test 1 ";
		// [@Stub  in = "test " + in + ([?stub1] ? (String)[$stub1] : "no stub");]
	 	if (true) {
	 		in = in + externalResource();    
	 		System.out.println(); 
	 	}  
		in = in + " test 2 ";
		return in; 
	}           
	       
	public static String getStubVar() {
		return "llama"; 
	}        
	                 
	@Exemplar(args="a:'arg1','arg2'", stubs={"stub3='alpaca'", "stub1='vicuna'"})  
	public String test2(String... in) {
		return in[0] + externalResource();   
	}  

	public String externalResource() {
		// [@StubInsert if ([?stub1]) return (String)[$stub1];]
		throw new RuntimeException();
	} 
} 
