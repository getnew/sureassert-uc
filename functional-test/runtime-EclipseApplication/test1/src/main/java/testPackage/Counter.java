package testPackage;

import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.Exemplars;

public class Counter {

	private int val;
	private int threshold;
	  
	@Exemplar(name="Counter/0-2", args={"0","2"})
	public Counter(int val, int threshold) {
		this.val = val;
		this.threshold = threshold;
	} 
	
	@Exemplars(set={
	@Exemplar(instance="Counter/0-2!", instanceout="Counter/1-2", expect="=(this.val,1)"),
	@Exemplar(instance="Counter/0-2!1", before="Counter/0-2!1{val=15,threshold=15}", debug="this.val") })
	public void increment() {
		if (val < threshold) 
			val++;
	}  

	@Exemplar(instance="Counter/1-2!", expect={"=(this.val,0)"})
	public void decrement() {
		if (val > 0)  
			val--; 
	}
	
	@Exemplar(expect="false")
	public boolean x() {   
		return false;
	}
	
	@Exemplar(expect="String")
	public Class<?> y() {
		return String.class;
	}

}
