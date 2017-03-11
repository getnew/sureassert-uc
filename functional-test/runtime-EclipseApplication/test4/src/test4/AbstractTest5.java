package test4;

import java.util.Set;

public abstract class AbstractTest5 <X extends Set<S>, S> {

	protected abstract X abstractMethod(X param);

	protected abstract <Y extends X> Y abstractMethod2(Y param);
}
 