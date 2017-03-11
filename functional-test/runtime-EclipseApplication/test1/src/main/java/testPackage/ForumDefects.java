package testPackage;

import java.util.Arrays;

import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.Exemplars;

public class ForumDefects {    
   
  @Exemplars(set={ 
      @Exemplar(args={"[pa:1f]"}, e="=(1f,#($arg1,0))"),
      @Exemplar(args={"[pa:1f]"}, e="#=($arg1,pa:1f)")
  })
  private static float returnFirst(float[] x){     
    return x[0]; 
  }              
         
  @Exemplar(a={"[ea:[ea:[d:]]]"}, ee="ArrayIndexOutOfBoundsException", e="retval.getMessage().endsWith('0')")
  private static double returnFirst(double[][] x){   
    return x[0][0]; 
  }
  
  @Exemplar(args={"pa:[pa:1.5],[pa:Double.NaN]"}, expectexception="java/lang/IllegalArgumentException")
  private int[] asRanks3(double[][] in) { 
    throw new IllegalArgumentException(Arrays.toString(in));     
  }    
  @Exemplars(set = { @Exemplar(expect = "'null'"), // Fails
      @Exemplar(expect = "+('nul','l')"),// Note the workaround
  })
  public static String nullString() {
    return "null";
  }

  @Exemplar(args = { "null" }, expect = "")
  public static boolean bug(Object object) {
    if (object != null)
      if (object.hashCode() == 0)
        return (true);

    return (false); 
  } 

  @Exemplar(args = { "null" }, expect = "")
  public static boolean noBug(Object object) {
    if (object != null) { 
      if (object.hashCode() == 0)
        return (true);
    }          

    return (false); 
  }

//  @Exemplar(args={"java/lang/reflect/Array.newInstance(Double.TYPE, 0)"}, expect="=(0,retval.length)")
//  private double[] asRanks(double[] in) {
//    return in;  
//  } 
//
//  @Exemplar(args={"ea:[d:]"}, expect="=(0,retval.length)")
//  private double[] asRanks2(double[] in) {
//    return in; 
//  }
   
}
