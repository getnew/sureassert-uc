import org.sureassert.uc.annotation.Exemplar;

public class Test5 {

	@Exemplar(a="5", e="6")
	public int x(int y) {
		return y + 1;
	}

	@Exemplar(a="5", e="6")
	public int x2(int y) {
		return y + 1;
	}
}
