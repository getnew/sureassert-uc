package testPackage;

import org.sureassert.uc.annotation.Exemplar;

public class EnumDefects {

  enum EnumDefectsInnerEnum {

    ENUM1("e1", 5), ENUM2("e2"), ENUM3("e3");

    private String arg1;
    private int arg2;

    EnumDefectsInnerEnum(String arg1, int arg2) {
      this.arg1 = arg1;
      this.arg2 = arg2;
    }
 
    EnumDefectsInnerEnum(String arg1) {
      this(arg1, -1);
    }
  }  
 
  
  @Exemplar
  public void doSomething() {
  	  
  	System.out.println(CardSuit.HEART.toString()); 
  	//EnumDefectsInnerEnum.ENUM1.toString(); 
  }

}