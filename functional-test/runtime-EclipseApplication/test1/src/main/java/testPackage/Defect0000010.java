package testPackage;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.sureassert.uc.annotation.Exemplar;

public interface Defect0000010 { 

	static class Bar1 {

		void baz1() {
		}
		 
		static class Bar4 { 

			void baz2() {
				
				new List() { 

					public boolean add(Object arg0) {
						// TODO Auto-generated method stub
						return false;
					}

					@Exemplar(args={"0","null"}, expect="")
					public void add(int arg0, Object arg1) {
						// TODO Auto-generated method stub
						
					}

					public boolean addAll(Collection arg0) {
						// TODO Auto-generated method stub
						return false;
					}

					public boolean addAll(int arg0, Collection arg1) {
						// TODO Auto-generated method stub
						return false;
					}

					public void clear() {
						// TODO Auto-generated method stub
						
					}

					public boolean contains(Object arg0) {
						// TODO Auto-generated method stub
						return false;
					}

					public boolean containsAll(Collection arg0) {
						// TODO Auto-generated method stub
						return false;
					}

					public Object get(int arg0) {
						// TODO Auto-generated method stub
						return null;
					}

					public int indexOf(Object arg0) {
						// TODO Auto-generated method stub
						return 0;
					}

					public boolean isEmpty() {
						// TODO Auto-generated method stub
						return false;
					}

					public Iterator iterator() {
						// TODO Auto-generated method stub
						return null;
					}

					public int lastIndexOf(Object arg0) {
						// TODO Auto-generated method stub
						return 0;
					}

					public ListIterator listIterator() {
						// TODO Auto-generated method stub
						return null;
					}

					public ListIterator listIterator(int arg0) {
						// TODO Auto-generated method stub
						return null;
					}

					public boolean remove(Object arg0) {
						// TODO Auto-generated method stub
						return false;
					}

					public Object remove(int arg0) {
						// TODO Auto-generated method stub
						return null;
					}

					public boolean removeAll(Collection arg0) {
						// TODO Auto-generated method stub
						return false;
					}

					public boolean retainAll(Collection arg0) {
						// TODO Auto-generated method stub
						return false;
					}

					public Object set(int arg0, Object arg1) {
						// TODO Auto-generated method stub
						return null;
					}

					public int size() {
						// TODO Auto-generated method stub
						return 0;
					}

					public List subList(int arg0, int arg1) {
						// TODO Auto-generated method stub
						return null;
					}

					public Object[] toArray() {
						// TODO Auto-generated method stub
						return null;
					}

					public Object[] toArray(Object[] arg0) {
						// TODO Auto-generated method stub
						return null;
					}
					
				}.get(0);
			}
		} 
	} 
   
	class Bar2 { 

		void baz2() {
		}
	}
	
	interface X53 {

		void baz5();
		   
		class Bar6 { 

			void ba8() {
			}
		}
	}

}
class Foo {
	
	@Exemplar(name="Foo/1")
	public Foo() {
	}
	
	class InnerFoo {
		
		private int x;
		
		@Exemplar(name="InnerFoo/1", args={"Foo/1"})
		public InnerFoo() {
			this.x = -1;
		}  
		
		@Exemplar(name="InnerFoo/2", args={"Foo/1", "5"})
		public InnerFoo(int x) {
			this.x = x;
		}  
		 
		@Exemplar(instance="InnerFoo/2", expect="5")
		public int getX() {
			return x;
		}
	}
}