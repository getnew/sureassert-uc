import com.sureassert.uc.annotation.UseCase;


public class TestCreate {
  
	@UseCase(args="'test'")
	public String toString(String x) {
		return "x" + x;
	}
} 
