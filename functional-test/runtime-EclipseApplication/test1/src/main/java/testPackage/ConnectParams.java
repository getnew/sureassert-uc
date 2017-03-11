package testPackage;

import java.util.Arrays;

import org.sureassert.uc.annotation.Exemplar;

import test2.GenericDTO;

public class ConnectParams {

	private String host;
	private int port;  
	  
	@Exemplar()  
	public String toString() { 
		MathUtils.add(5, 10);
		GenericDTO x = new GenericDTO("test1", 5, new boolean[] {true, false});
		return x.toString();
		//return "host = " + host + " | port = " + port;
	}             
	  
	@Exemplar
	public boolean test() {
		return true;
	}
	
	@Exemplar(args="'testParam1'")
	public void test3(String param1) {
	}
     
	/**
	 * 
	 * @param genericDTO
	 * @return
	 */   
	@Exemplar(args="GenericDTO/myGenericDTO1", expect="'[true, false, true]'")  
	public String doSomthingWithDTO(GenericDTO genericDTO) {
		return Arrays.toString(genericDTO.getMyField3());
	}
	    
	@Exemplar(args="GenericDTOFactory.myGenericDTO2", expect="16")   
	public int doSomthingWithDTO2(GenericDTO genericDTO) {
		return genericDTO.getMyField2(); 
	}  
}  
