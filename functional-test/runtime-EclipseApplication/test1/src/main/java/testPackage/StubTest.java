package testPackage;

import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.Exemplars;

import test2.GenericDTOFactory;

public class StubTest {

  @Exemplars(set={   
    @Exemplar(args={"1","2"}, expect="4", stubs="GenericDTO.getMyField2=1")
  })     
  public int add(int x, int y) {  
    TestSingleton.getInstance().registerCoverage("llamaClassName", 1979);  
    return x + y + GenericDTOFactory.getMyGenericDTO3().getMyField2();
  }
}
            