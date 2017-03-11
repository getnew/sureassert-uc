package testPackage;

import org.sureassert.uc.annotation.Exemplar;

public class TestGeneric {
	
	public <I extends Integer> int plusOneGeneric(I var) {
		
		return var + 1;
	}   
	 
	@Exemplar(args="1", stubs="stub1='plusTwoGenericUC'")
	public <I> int plusTwoGeneric(I var) {
		
		System.out.println(new TestInlineMock().test2(((Integer)var).toString()));
		return (Integer)var + 1;
	}           
}
