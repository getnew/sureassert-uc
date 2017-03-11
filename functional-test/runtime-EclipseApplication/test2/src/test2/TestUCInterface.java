package test2;

import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.Exemplars;

public interface TestUCInterface extends TestUCInterface2 {
	  
	@Exemplars(set={
	@Exemplar(args="GenericDTOFactory/testDTO3", //
			expect="(retval.equals(18))") 
	})
	public int testUC(GenericDTO dto);   
}
  