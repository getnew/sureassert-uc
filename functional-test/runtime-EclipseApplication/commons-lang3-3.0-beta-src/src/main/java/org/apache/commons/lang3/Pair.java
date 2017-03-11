/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package org.apache.commons.lang3;  
  
import java.io.Serializable;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.sureassert.uc.annotation.HasJUnit;
import org.sureassert.uc.annotation.Exemplars;
import org.sureassert.uc.annotation.NamedClass;
import org.sureassert.uc.annotation.Exemplar;

/** 
 * A basic immutable Object pair.
 *
 * <p>#ThreadSafe# if the objects are threadsafe</p>
 * @since Lang 3.0
 * @author Matt Benson
 * @version $Id: Pair.java 967237 2010-07-23 20:08:57Z mbenson $
 */
@HasJUnit(jUnitClassNames="org.apache.commons.lang3.PairTest")
@NamedClass(name="Pair")
public final class Pair<L, R> implements Serializable { 
    /** Serialization version */
    private static final long serialVersionUID = 4954918890077093841L;

    /** Left object */
    public final L left;  
 
    /** Right object */
    public final R right;

    /**
     * Create a new Pair instance.
     * @param left
     * @param right
     */ 
    @Exemplars(set={ 
    @Exemplar(name="Pair/1", args={"0", "'foo'"}, expect={"retval.left.equals(0)", "retval.right.equals('foo')"}), 
    @Exemplar(name="Pair/2", args={"null", "'bar'"}, expect={"=(retval.left, null)", "retval.right.equals('bar')"}) })
    public Pair(L left, R right) { 
        this.left = left;  
        this.right = right;      
    }           

    /**  
     * {@inheritDoc} 
     */  
    @Override
    @Exemplars(set={ 
    @Exemplar(instance="new Pair(null,'foo')", args="new Pair(null,'foo')", expect="true"), 
    @Exemplar(instance="new Pair('foo',0)", args="new Pair('foo',null)", expect="false"), 
    @Exemplar(instance="new Pair('foo',0)", args="new java/lang/String('foo')", expect="false"), 
    @Exemplar(instance="Pair/1", args="Pair/1", expect="true")})
    public boolean equals(Object obj) { 
    	
        if (obj == this) {   
            return true;  
        }    
        if (obj instanceof Pair<?, ?> == false) { 
            return false;
        }
        Pair<?, ?> other = (Pair<?, ?>) obj;
        return ObjectUtils.equals(left, other.left) && ObjectUtils.equals(right, other.right);
    }

    /**
     * {@inheritDoc}   
     */
    @Override
    @Exemplar(instance="Pair.of(null,'foo')", expect="Pair.of(null,'foo').hashCode()")
    public int hashCode() { 
        return new HashCodeBuilder().append(left).append(right).toHashCode();
    }      

    /**
     * Returns a String representation of the Pair in the form: (L,R)
     */
    @Override
    @Exemplars(set={
    @Exemplar(instance="Pair.of(null, null)", expect="'(null,null)'"),
    @Exemplar(instance="Pair.of(null, 'two')", expect="'(null,two)'"),
    @Exemplar(instance="Pair.of('one', null)", expect="'(one,null)'"),
    @Exemplar(instance="Pair.of('one', 'two')", expect="'(one,two)'")})
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        builder.append(left);
        builder.append(",");
        builder.append(right); 
        builder.append(")");
        return builder.toString();
    }

    /**
     * Static creation method for a Pair<L, R>.
     * @param <L>
     * @param <R>
     * @param left
     * @param right
     * @return Pair<L, R>(left, right)
     */
    @Exemplars(set={  
    @Exemplar(name="Pair/of0Foo", args={"0", "'foo'"},
    	expect={"retval.left.equals(0)", "retval.right.equals('foo')"}), 
    @Exemplar(name="Pair/ofNullBar", args={"null", "'bar'"},
        	expect={"=(retval.left, null)", "retval.right.equals('bar')"}) })
    public static <L, R> Pair<L, R> of(L left, R right) {
    	
        return new Pair<L, R>(left, right);
    } 
    
    @Exemplar(args="#(ExceptionUtils/f2, 0)", debug={"ExceptionUtils/f2", ">=(1, 2)"})
    public static String getStr(String x) {
    	return x + " alpaca";
    }
}
