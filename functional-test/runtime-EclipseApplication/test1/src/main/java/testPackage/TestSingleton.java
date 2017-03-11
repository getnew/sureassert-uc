package testPackage;

public class TestSingleton {

  private static class SingletonHolder {
    private static final TestSingleton instance = new TestSingleton();
  }

  public static TestSingleton getInstance() {

    return SingletonHolder.instance;
  }
      
  public void registerCoverage(String className, int lineNum) {
     
  } 
}   
