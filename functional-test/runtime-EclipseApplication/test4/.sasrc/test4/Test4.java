package test4;
import java.sql.SQLException;

import org.sureassert.uc.annotation.Exemplar;

import test2.GenericDTO;
import test2.TestUCInterface;


public class Test4 implements TestUCInterface {
	
	private int field;
	
	@Exemplar(name="Test4/1")
	public Test4() {
		
		field = 5;
	}
  
	@Exemplar(stubs="stub1=sf:resources/test[[1]].xml")
	private void test4() {
		/* 
		 * [@Insert System.out.println([$stub1]);]  
		 * */
		try { 
			System.out.println("debug test4");
		} catch (Exception e) {
			System.err.println(e);  
		}  
 
		System.out.println(132);
 
		CardSuite suite = CardSuite.SPADE; 
		suite.getColour(); 
		int x = suite.throwException();      
	} 
	     
	public String toString() {
		return "Test4: " + field;
	} 
 
	public int testUC(GenericDTO dto) {
		
		dto.setSwitched();
		return 18;     
	} 
	
	@Exemplar(stubs="stub1='nathan'")
	public int testWhile() {
		int x =0; 

		_sauc.SAInterceptor.instance.stubExecuted(52,53,"test4.Test4");
 
		return x + testWhile2();  
	}      
	
	public int testWhile2() {
		int x =0;

		_sauc.SAInterceptor.instance.stubExecuted(60,60,"test4.Test4");System.out.println(_sauc.SAInterceptor.instance.execStub("stub1"));
		while (x<10) {
			x++;
		}
		return x; 
	} 
	
	public void testThrownException() throws SQLException {
	  
	  throw new SQLException("oijoij");
	}
}    
         