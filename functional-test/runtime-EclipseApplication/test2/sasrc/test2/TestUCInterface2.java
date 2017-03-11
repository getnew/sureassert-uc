package test2;

import org.sureassert.uc.annotation.Exemplar;

public interface TestUCInterface2 {

	@Exemplar(args = "testDTO3", //
			expect="'GenericDTO must be switched': (testDTO3.isSwitched())")   
	public int testUC(GenericDTO dto);
} 
