package testPackage;

import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.Exemplars;

import test2.GenericDTO;
import test2.GenericDTOFactory;
    
//@HasJUnit(jUnitClassNames="junittests.TestMathUtils")
public class MathUtils {
	 
	@Exemplar(args={"0","1"}, expect="1l") 
	public static long add(int a, int b) {
		//while (new Integer(5) == 5);   
		return a + b;
	} 
 
	public static int mod(int a, int b) {
		return a % b;       
	}

	@Exemplar(args={"2.0", "5.0"}, expect="10.0")
	public static double multiply(double a, double b) { 
		return a * b;          
	}  
    
	@SuppressWarnings("unused")
	private static final int INT_10 = 10;
	
	public static int testX() {  
		return GenericDTOFactory.myInt1;
	}
	  
	/**
	 * test 
	 * @param a
	 * @param b
	 * @return 
	 */
	@Exemplar(args={"1", "2l"}, expect="3l")
	public static long add(int a, long b) { 
		return a + b;         
	}
	   
   
	@Exemplars(set={ 
		@Exemplar(args="GenericDTOFactory.myGenericDTO3", expect="18"),
		@Exemplar(args="GenericDTOFactory/test123", expect="18")
	})
	public int getATestDTO(GenericDTO dto) { 
		return dto.getMyField2();           
	}
	
	public int doSomethingElse09(int xyz) {
		
		return 201;
	}
	
	@Exemplar(args={"0"}, expect="")
	public int doSomethingElse096(int xyz) {
		
		return 201;
	}
 
	@Exemplars(set={   
		@Exemplar(args="GenericDTOFactory.myGenericDTO3", expect="18"),
		@Exemplar(args="GenericDTOFactory/test123", expect="13", stubs="GenericDTO.getMyField2()=13")
	}) 
	public int getATestDTO2(GenericDTO dto) {  
		return dto.getMyField2();
	}   
  
	@Exemplar(expect="18")
	public int getATestDTO2() { 
		return GenericDTOFactory.myInt1;     
	}      
	
	@Exemplar(expect="")
	public void test() {

		String x = "ikj";
		x.substring(1, 2);
	}

	public interface Validator {
		
		public boolean validate(Command command);
	}
	
	public interface Command {
		
		public void setError(CommandError error);
		
		public boolean isValid();
		
		public CommandError getError();
	}
	
	public interface CommandError {
		 
	}
}
