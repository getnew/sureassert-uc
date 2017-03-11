package junittests;

import org.junit.Test;


public class TestStringEquals {

  @Test
  public void testString() {
    
    StringBuilder _str1 = new StringBuilder();
    for (int i = 0; i < 10000000; i++) {
      _str1.append("x");
    }
    String str1 = _str1.toString();
    StringBuilder _str2 = new StringBuilder();
    for (int i = 0; i < 10000000; i++) {
      _str2.append("x");
    }
    String str2 = _str2.toString();
    
    long startTime;
    double totalMs;
    boolean bool;
    
    startTime = System.nanoTime();
    bool = str1.equals(str2);
    totalMs = (System.nanoTime() - startTime) / 1000000d;
    System.out.println("str1.equals(str2) time = " + totalMs);
    
    startTime = System.nanoTime();
    bool = !str1.equals(str2);
    totalMs = (System.nanoTime() - startTime) / 1000000d;
    System.out.println("!str1.equals(str2) time = " + totalMs);
  }
}
