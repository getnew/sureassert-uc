package test2;

import org.sureassert.uc.annotation.Exemplar;

public interface TestUCInterface extends TestUCInterface2 {
	
	@Exemplar(args="testDTO3", //
			expect="(retval.equals(18))") 
	public int testUC(GenericDTO dto);   
} 
 