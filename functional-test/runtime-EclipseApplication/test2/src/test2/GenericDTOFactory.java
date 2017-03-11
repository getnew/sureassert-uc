package test2;

import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.NamedInstance;

public class GenericDTOFactory {    
    
	private static GenericDTO myGenericDTO2 = new GenericDTO("val15", 16, new boolean[] {false, true, true});
	      
	@NamedInstance(name="test123") 
	public static GenericDTO myGenericDTO3 = new GenericDTO("val15", 18, new boolean[] {false, true, true});
	   
	public static final int myInt1 = 18;     
   	                    
	@Exemplar(name="testDTO3") 
	private static GenericDTO getTestDTO3() {    
		return new GenericDTO("val15", 18, new boolean[] {false, true, true});
	}     
	    
	public static GenericDTO getMyGenericDTO3() {  
		return myGenericDTO3;
	}
	   
	public void x() {
		
	}
} 