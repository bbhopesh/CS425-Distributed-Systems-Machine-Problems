package edu.illinois.uiuc.sp17.cs425.team4.POC;

import java.lang.reflect.InvocationTargetException;

public class ClassLoadTest {
	
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		SomeClass x = new SomeClass("hi");
		String name = x.getClass().getName();
		Class c = Class.forName(name);
		SomeClass d = (SomeClass) c.getDeclaredConstructor(String.class).newInstance("hi");
		//throw new RuntimeException(d);
	}
	
	
	private static class SomeClass {
		public SomeClass(String hi) {
			
		}
	}
}
