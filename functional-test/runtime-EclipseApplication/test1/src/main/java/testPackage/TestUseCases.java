package testPackage;
 
import org.sureassert.uc.annotation.Exemplar;

import test2.GenericDTO;
import test2.TestUCInterface;
  
public class TestUseCases implements TestUCInterface  {
	 
	/**  
	 * test   
	 */     
	@Exemplar(args = {"1", "2"}, //   
			expect = {"=(MathUtils.add(TestClassB/testClassB, MathUtils.add(TestClassB/testClassB, MathUtils.INT_10)), 20l)", //
			"3l"})    
	public long add(int a, int b) { 
		new ConnectParams().toString();  
		return a + b;
	}
	
	public int testUC(GenericDTO dto) {
		dto.setSwitched(); 
		return dto.getMyField2();  
	}               
	 
	@Exemplar  
	public String getJMSDelegate() {
		return new JMSServerDelegate("testHost1", 123).toString();
	}   
}  
