package org.apache.commons.lang3.builder;

import org.sureassert.uc.annotation.Exemplar;

public class Pair {
	
	public static Pair t1 = new Pair(null, null);
	
	@Exemplar(args={"null","null"})
    public Pair(Object left, Object right) {
    }      
}
