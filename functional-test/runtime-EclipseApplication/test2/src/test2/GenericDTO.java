package test2;

import java.util.Arrays;

import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.SINType;

public class GenericDTO extends ValueObject {
	
	private final String myField1; 
	  
	private final int myField2;
   
	private final boolean[] myField3; 
	   
	private boolean switched = false;         
	     
	@Exemplar(args={"'val1'", "i:2", "pa:[b:true],[b:false],[b:true]"}, //
	name="myGenericDTO1")    
	public GenericDTO(String myField1, int myField2, boolean[] myField3) {
		this.myField1 = myField1; 
		this.myField2 = myField2;   
		this.myField3 = myField3;   
	}     

	@SINType(prefix="gd")
	public static GenericDTO getGenericDTO(String myField1, int myField2, boolean[] myField3) {
		
		return new GenericDTO(myField1, myField2, myField3);
	}
	 
	public String testNew(String x) {
		 return x;  
	}    
	
	//@Exemplar
	public void testLoop() {
		// method start
		for (int x = 0; x < 10; x++) {
			System.out.println("first line");
			System.out.println("second line");
			System.out.println(x);
		}
	}

	@Exemplar
	public void testLoop2() {
		// method start
		for (int x = 0; x < 10; x++);
	} 

	
	public void testLoop3() {
		// method start
		int x = 0;   
		while (x < 10);
	}
    
	@Exemplar(expect="retval.equals(new java/lang/String('val1').toLowerCase())")
	public String getMyField1() { 
		return myField1;
	}  
	  
	@Exemplar(expect="1")  
	public int return1() {
		return 1;
	}   
  
	@Exemplar(expect="2") 
	public int getMyField2() { 
		return myField2;
	}        

	public boolean[] getMyField3() {
		return myField3;   
	}
	
	public void setSwitched() {
		switched = true;
	}
	
	public boolean isSwitched() {
		return switched;
	}     

	@Override
	protected Object[] getImmutableState() { 

		return new Object[] {myField1, myField2, Arrays.toString(myField3)};
	}
	     
	public String toString() {
		return super.toString(); 
	}
}
