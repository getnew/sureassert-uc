/*
 * Copyright 2011 Nathan Dolan. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this 
 *    list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER ``AS IS'' AND ANY EXPRESS OR 
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO 
 * EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR 
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 * SUCH DAMAGE.
 * 
 */
package org.sureassert.uc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a method that should be tested and specifies test data and optionally stubs and/or an
 * expected result.
 * <p>
 * <b>Simple Invocation Notation</b> <br>
 * <br>
 * Simple Invocation Notation (SIN) is used within Exemplars to specify objects to use when invoking
 * methods and evaluating results.<br>
 * When nesting SIN types the nested type must be bracketed by []. <br>
 * e.g. for a map of string to list of integers: <br>
 * m:'key1'=[l:1,2,3], 'key2'=[l:4,5,6] <br>
 * <br>
 * <br>
 * <b>SIN Types</b><br>
 * <br>
 * <li><code>'str' or str:str</code> = String type = new String("str") <br>
 * <li><code>a:'x','y','z'</code> = Object array type <br>
 * <li><code>true or b:true</code> = boolean true <br>
 * <li><code>false or b:false</code> = boolean false <br>
 * <li><code>1 or i:1</code> = integer type = 1 (integer) <br>
 * <li><code>d:1.5, 1.5</code> = double type = 1.5 (double) <br>
 * <li><code>float:1.5 or 1.5f</code> = float type = 1.5f (float) <br>
 * <li><code>lg:1 or 1l</code> = long type = 1l (long) <br>
 * <li><code>short:1 or 1s</code> = short type = 1 (short) <br>
 * <li><code>l:1,2,3</code> = list type = new ArrayList containing Integers 1, 2 and 3. <br>
 * <li><code>m:'key1'=1,'key2'=2</code> = HashMap type = new HashMap containing string keys key1 and
 * key2 mapped to integers 1 and 2 respectively. <br>
 * <li><code>s:1,2,3</code> = HashSet type = new HashSet containing Integers 1, 2 and 3. <br>
 * <li><code>pa:1,2,3</code> = primitive array type = new int[3]{1,2,3} <br>
 * <li><code>ea:[d:]</code> = empty primitive array type of given type = new double[] {} <br>
 * <li><code>f:resources/test.xml</code> = File type = new File("resources/test.xml"), relative from
 * project root <br>
 * <li><code>sf:resources/test.xml</code> = String from File type = contents of file
 * resources/test.xml, relative from project root, as a String <br>
 * <li><code>sl:1,2,3</code> = Stub expression list type = special list type used to define stub
 * invocation results. <br>
 * <br>
 * <br>
 * <b>SIN Expressions</b> <br>
 * <br>
 * <li><code>instanceA or ni:instanceA</code> = named instance "instanceA". However by convention
 * names should be in the format "ClassA/instanceA"<br>
 * <li><code>ClassA/xyz</code> = named instance "ClassA/xyz" <br>
 * <li><code>ClassA/ExemplarA!</code> = Re-execute Exemplar named "ClassA/ExemplarA" and return the
 * value it returns<br>
 * <li><code>ClassA/ExemplarA!123</code> = named instance "ClassA/ExemplarA!123" if it exists.
 * Otherwise re-execute and return value returned by Exemplar named "ClassA/ExemplarA", and store
 * value as "ClassA/ExemplarA!123"<br>
 * <li><code>retval</code> = return value of Exemplar <br>
 * <li><code>this</code> = the instance of this class used by this Exemplar <br>
 * <li><code>namedInstance</code> = value of a named instance i.e. a Exemplar or NamedInstance name
 * <br>
 * <li><code>MyClass1</code> = MyClass1.class, where MyClass1 is any source class within a
 * Sureassert-active project in the workspace<br>
 * <li><code>mypackage/mysubpackage/MyClass1</code> = mypackage.mysubpackage.MyClass1.class (any
 * class on project classpath)<br>
 * <li><code>MyClass.field1</code> = value of static field on class <br>
 * <li><code>this.field1</code> = value of field on the Exemplar's instance <br>
 * <li><code>MyClass.staticMethod(arg1, arg2)</code> = value of static method invoked on class <br>
 * <li><code>namedInstance.method(arg1, arg2)</code> = value of method invoked on instance <br>
 * <li><code>method(arg1, arg2)</code> = value of method invoked on this instance i.e.
 * this.method(arg1, arg2) <br>
 * <li><code>new MyClass(arg1, arg2)</code> = new instance of class <br>
 * <li><code>new MyClass(arg1, arg2) {field1=val1, field2=val2}</code> = new instance of class, with
 * some fields set to values. <br>
 * <li><code>new MyClass(arg).method(arg).field</code> = value of field on return value of method
 * invoked on new class. <br>
 * <li>
 * <code>retval.equals('a string with [']with quotes['] and /[square brackets/] and a single // forward-slash')</code>
 * = Example of escaping: evaluates to: a string with 'with quotes' and [square brackets] and a
 * single / forward-slash'. <br>
 * <li><code>*</code> <i>(in args)</i> = Generate default implementation of interface or abstract
 * class argument and pass as argument. <br>
 * <li><code>*MyInterface</code> = Generated default implementation of MyInterface <i>(MyInterface
 * must have been declared for default implementation generation using defaultimpls)</i><br>
 * <br>
 * <br>
 * <b>Operators</b> <br>
 * <br>
 * The following built-in method symbols should be used to replace java operators in SIN
 * expressions: <br>
 * <br>
 * <li><code>=(a,b)</code> = (a==null && b==null) || (a!=null && a.equals(b))<br>
 * <li><code>==(a,b)</code> = a == b<br>
 * <li><code>!=(a,b)</code> = a != b<br>
 * <li><code>isa(a,b)</code> = a instanceof b <i>where b is a class e.g. MyClass or java/io/File</i>
 * <br>
 * <li><code>&gt;(a,b)</code> = a &gt; b<br>
 * <li><code>&lt;(a,b)</code> = a &lt; b<br>
 * <li><code>&gt;=(a,b)</code> = a &gt;= b<br>
 * <li><code>&lt;=(a,b)</code> = a &lt;= b<br>
 * <li><code>!(a)</code> = !o<br>
 * <li><code>&(a,b)</code> or <code>&&(a,b)</code> = a && b<br>
 * <li><code>|(a,b)</code> or <code>||(a,b)</code> = a || b<br>
 * <li><code>+(a,b)</code> = append two strings<br>
 * <li><code>#(a,n)</code> = a[n] <i>where a is an array and n is an element index</i><br>
 * <li><code>#=(a,b)</code> = deep array value equality i.e. Arrays.deepEquals(a, b) <i>where a and
 * b are arrays</i>, or Arrays.equals(a,b) where a and b are primitive arrays.<br>
 * <br>
 * <br>
 * 
 * @since 1.0
 * @author Nathan Dolan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface Exemplar {

	/**
	 * If set, for non-void methods, the name to assign the instance returned by this Exemplar. <br>
	 * The named instance returned by this Exemplar is available for use by any other Exemplars
	 * within
	 * the workspace. <br/>
	 * <br/>
	 * Alternatively, rather than use the returned instance itself, other Exemplars may use the
	 * <code>exempName!</code> notation to re-run the Exemplar named <code>exempName</code> and use
	 * the new
	 * instance returned by the re-run.<br/>
	 * <br/>
	 * By convention, names should be of the format <code>Clazz/name</code> where <code>Clazz</code>
	 * is the simple name of the class on which the Exemplar is defined, and <code>name</code> is a
	 * unique name within that class.
	 */
	String name() default "";

	/** Shorthand for <code>name</code> */
	String n() default "";

	/**
	 * An optional free-text description of this Exemplar for documentation purposes.
	 */
	String description() default "";

	/** Shorthand for <code>description</code> */
	String d() default "";

	/** The arguments to pass to the method; one SIN value/expression per argument. */
	String[] args() default {};

	/** Shorthand for <code>args</code> */
	String[] a() default {};

	/**
	 * An expression returning an instance on which this Exemplar will be invoked. <br>
	 * <br>
	 * It must be an instance assignable to the class or interface in which this Exemplar is
	 * declared.
	 * Use <code>exempName!</code> notation to re-execute a named Exemplar (typically defined on a
	 * constructor) to return a new instance. <br>
	 * <br>
	 * Only one of <code>instance</code> and <code>depends</code> can be
	 * specified.
	 */
	String instance() default "";

	/** Shorthand for <code>instance</code> */
	String i() default "";

	/**
	 * A list of names of Exemplars on which this Exemplar depends, used to order execution of
	 * Exemplars where it is necessary to override the default execution order calcaulted
	 * by the Sureassert UC engine. These Exemplars will be executed in the specified order before
	 * execution of this Exemplar. Generally, specifying this is not necessary or desirable.<br>
	 * <br>
	 * This Exemplar will be invoked on the instance on which the last Exemplar in the depends list
	 * was invoked. <br>
	 * <br>
	 * Note any dependencies implied by references from the Exemplar definition or annotated method
	 * are determined automatically and <b>do not</b> need to be explicitly declared here. <br>
	 * <br>
	 * Only one of <code>instance</code> and <code>depends</code> can be
	 * specified.
	 */
	String[] depends() default {};

	/**
	 * Expressions to evaluate against the value returned by the method, each a boolean SIN
	 * Expression.<br>
	 * <br>
	 * If evaluating the return value the expression <code>=(retval, &lt;expr&gt;)</code> may be
	 * replaced
	 * by the shorthand <code>&lt;expr&gt;</code>. I.e. if the return value is not a boolean or a
	 * class (or the expression itself is a boolean literal), <code> =(retval, &lt;expr&gt;)</code>
	 * is assumed. <br>
	 * If the return value is a class, the expression defaults to
	 * <code> isa(retval, &lt;expr&gt;)</code>. <br>
	 * If empty returns true. <br>
	 * <br>
	 * Optionally the notation <code>'failure message':expectExpr</code> can be used
	 * where <code>'failure message'</code> is a message to use in the error marker if
	 * <code>expectExpr</code> evaluates to false.
	 */
	String[] expect() default {};

	/** Shorthand for <code>expect</code> */
	String[] e() default {};

	/**
	 * The simple class name of an Exception that is expected to be thrown by this Exemplar.
	 */
	String expectexception() default "";

	/** Shorthand for <code>expectexception</code> */
	String ee() default "";

	/**
	 * A list of method invocations that are expected to be executed under the Exemplar.
	 * The expression used is the same as is used for the matcher element (left-hand-side)
	 * of <code>stubs</code> expressions.<br>
	 * <br>
	 * For example to verify that method Foo.bar(int) was invoked, specify: <br>
	 * <code>Foo.bar(int)</code> or omit the param to match any method with this name. <br>
	 * <br>
	 * To match only invocations with a value of less than 5, specify:<br>
	 * <code>Foo.bar(<(arg,5))</code>.
	 */
	String[] verify() default {};

	/** Shorthand for <code>verify</code> */
	String[] v() default {};

	/**
	 * A name to assign the instance on which the Exemplar is executed, post-execution.
	 * Other Exemplars may then reference this instance thereby implicitly declaring a
	 * dependency on this Exemplar, i.e. the instance returned is always the instance
	 * state after execution of this Exemplar.
	 */
	String instanceout() default "";

	/** Shorthand for <code>instanceout</code> */
	String io() default "";

	/**
	 * A list of SIN Expressions to execute before executing this Exemplar. <br>
	 * <br>
	 * The before expressions are executed between fetching the instance and executing
	 * the method-under-test. Therefore the <code>this</code> named instance is available
	 * for use, but <code>retval</code> and <code>$argN</code> are not.
	 */
	String[] before() default {};

	/**
	 * A list of SIN Expressions to execute after executing this Exemplar. <br>
	 * <br>
	 * All named instances available to <code>expect</code> expressions are available to the
	 * <code>after</code> expressions.
	 */
	String[] after() default {};

	/**
	 * A list of method or source stub definitions.
	 * Stubs are defined as either:
	 * <p>
	 * <li><b>Source stubs:</b> <code>sourceStubVar=expression</code>, where
	 * <code>sourceStubVar</code> is the name of a source stub variable defined in-line within code
	 * comments of any class executed under the Exemplar. <code>expression</code> is a SIN
	 * Expression that replaces the implementation of the stubbed method.
	 * <li><b>Method stubs:</b> <code>signature=expression</code>, where <code>signature</code> is
	 * the signature of the method to be stubbed and <code>expression</code> is a SIN Expression
	 * that replaces the implementation of the stubbed method.
	 * </p>
	 * <br>
	 * Method stubs can also be defined as <code>signature=[sl:expression]</code>, in order for a
	 * method to be assigned a &quot;stub list&quot; of expressions to be used as return values of
	 * the stubbed method. The items in the list are iterated for each invocation of the stubbed
	 * method by the Exemplar. Where the number of invocations exceeds the number of stub
	 * expressions in the list, the last defined stub expression is used. <br>
	 * <br>
	 * Use <code>signature^=exceptionExpression or [sl:exceptionExpression]</code> to throw an
	 * exception from the stubbed method, where exceptionExpression is an expression which
	 * evaluates to an exception throwable from the stubbed method. <br>
	 * <br>
	 * Method <code>signature</code>s are defined as <code>class.method</code>,
	 * <code>class.method()</code> or <code>class.method(arg1class, ..., argNclass)</code> where:
	 * <li><code>class</code> is the simple or fully-qualified class name of the stubbed method.
	 * Fully-qualified names must use <code>/</code> as the package separator rather than
	 * <code>.</code> <li><code>method</code> is the name of the method to stub and <li>optionally
	 * <code>argXclass</code> is the simple or fully-qualified class name of each argument of the
	 * stubbed method. Note that methods are matched based on whether the classes of arguments
	 * passed at runtime are assignable to the specified argument classes.<br>
	 * <br>
	 * If the <code>class.method</code> notation is used, the stub expression will be applied to all
	 * methods with the given name.
	 */
	String[] stubs() default {};

	/** Shorthand for <code>stubs</code> */
	String[] s() default {};

	/**
	 * Specifies stubs as per <code>stubs</code>. However stubs specified with <code>vstubs</code>
	 * <i>must</i> be invoked during Exemplar execution in order for the Exemplar to pass.
	 * Effectively <code>vstubs</code> are treated as both <code>stubs</code> and
	 * <code>verify</code> expressions.
	 */
	String[] vstubs() default {};

	/**
	 * Short-hand for <code>vstubs</code>
	 */
	String[] vs() default {};

	/**
	 * A list of <i>fully-qualified</i> class names of interfaces or abstract classes
	 * for which a default implementation will be created. The generated
	 * default implementation can be referenced elsewhere using the <code>*simpleClassName</code>
	 * notation, e.g. to use a new instance of the generated class, specify
	 * <code>new *simpleClassName()</code>
	 */
	String[] defaultimpls() default {};

	/**
	 * One or more Exemplar names from which properties will be copied over to this Exemplar. <br>
	 * <br>
	 * The named Exemplar must be in the same source file as this Exemplar. <br>
	 * <br>
	 * Use the template property to take an existing Exemplar definition and modify it -
	 * any properties defined by this Exemplar will override the template one. <br>
	 * <br>
	 * If you define a property using an empty string or array, you will override the template
	 * property with nothing, i.e. remove the template-defined property from this Exemplar. <br>
	 * <br>
	 * If you specify multiple template Exemplars, properties from each template will override those
	 * from subsequently defined templates. For example, properties defined in the first given
	 * template will override those defined in the second.
	 */
	String[] template() default {};

	/**
	 * Short-hand for <code>template</code>
	 */
	String[] t() default {};

	/**
	 * A number of SIN expressions that the user wishes to be evaluated
	 * for debug purposes.
	 */
	String[] debug() default {};
}
