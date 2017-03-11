package test2;

import org.sureassert.uc.annotation.Exemplar;

public interface InternationalHello {

	/**
	 * Gets a hello message for the given person.
	 * @param name The name of the person to say hello to
	 * @return The hello message
	 */
	@Exemplar(args={"'Joe'"}, expect="retval.contains($arg1)")
	public String hello(String name);
}
