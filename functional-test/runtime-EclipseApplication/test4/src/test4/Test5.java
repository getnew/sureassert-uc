package test4;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.sureassert.uc.annotation.Exemplar;


public class Test5 {
 
	@Exemplar
	public void test() {
		if (true)
			System.out.println("do something");  
	}
	
	@Exemplar(args={"*", "*"})
	public Object test2(TestIF5<ArrayList<?>> arg, AbstractTest5<HashSet<Integer>, Integer> arg2) {
		return arg2.abstractMethod2(new HashSet<Integer>());
		//return arg.returnAT();   
	}     
}     
