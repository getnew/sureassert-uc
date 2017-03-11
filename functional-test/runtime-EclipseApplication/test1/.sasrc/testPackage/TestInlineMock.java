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
		 in = "test " + in + (_sauc.SAInterceptor.instance.isStubbed("stub1") ? (String)_sauc.SAInterceptor.instance.execStub("stub1") : "no stub");



  
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
		if (_sauc.SAInterceptor.instance.isStubbed("stub1")) return (String)_sauc.SAInterceptor.instance.execStub("stub1");
		throw new RuntimeException();
	} 
} 
