package test2;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Base class used by classes that require compare-by-value.
 * 
 * @author Nathan Dolan
 * 
 */
public abstract class ValueObject implements Serializable {

	private static final long serialVersionUID = 1L;

	private Object[] state;
	private int hashCode = Integer.MIN_VALUE;

	private boolean gotState() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","gotState",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","gotState",""))) return Boolean.parseBoolean(com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","gotState","")).toString());

		return hashCode != Integer.MIN_VALUE;
	}

	/**
	 * The state of this object used to compare it to other instances.
	 * All objects returned must be immutable.
	 * 
	 * @return the immutable state of this instance.
	 */
	protected abstract Object[] getImmutableState();
	
	private Integer[][] test(String[][] param) {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","test","[[Ljava.lang.String;"), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","test","[[Ljava.lang.String;"))) return (java.lang.Integer[][])com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","test","[[Ljava.lang.String;"));
		return null;
	}

	private void init() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","init",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","init",""))) { com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","init","")); return; }

		state = getImmutableState();

		final int prime = 31;
		hashCode = 1;
		hashCode = prime * hashCode + Arrays.deepHashCode(state);
		if (hashCode == Integer.MIN_VALUE)
			hashCode = Integer.MIN_VALUE + 1;

	}

	@Override
	public int hashCode() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","hashCode",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","hashCode",""))) return Integer.parseInt(com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","hashCode","")).toString());

		if (!gotState())
			init();

		return hashCode;
	}

	private Object[] getImmutableStateInternal() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","getImmutableStateInternal",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","getImmutableStateInternal",""))) return (java.lang.Object[])com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","getImmutableStateInternal",""));

		if (!gotState())
			init();

		return state;
	}

	@Override
	public boolean equals(Object obj) {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","equals","java.lang.Object"), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","equals","java.lang.Object"))) return Boolean.parseBoolean(com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","equals","java.lang.Object")).toString());

		if (!gotState())
			init();

		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ValueObject other = (ValueObject) obj;
		if (!Arrays.deepEquals(state, other.getImmutableStateInternal()))
			return false;
		return true;
	}

	@Override
	public String toString() {com.sureassert.uc.runtime.SAInterceptor.instance.registerMethodStart(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","toString",""), false);com.sureassert.uc.runtime.SAInterceptor.instance.interruptedCheck();if (com.sureassert.uc.runtime.SAInterceptor.instance.isStubbed(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","toString",""))) return (java.lang.String)com.sureassert.uc.runtime.SAInterceptor.instance.execStub(com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignature("test2.ValueObject","toString",""));

		return Arrays.toString(getImmutableStateInternal());
	}

}
