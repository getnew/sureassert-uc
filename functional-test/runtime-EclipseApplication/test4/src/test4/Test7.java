package test4;

import org.sureassert.uc.annotation.Exemplar;

public class Test7 {

	@Exemplar(args={"5"}, expect="5.0")
	public static double multiply(int x) { 
		return 5;          
	}   
	
	@Exemplar(args={"5"}, expect="5.0")
	public double multiply2(int x) { 
		return 5;          
	}     
} 
   