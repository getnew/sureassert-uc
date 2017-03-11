package testPackage;

import java.io.InputStreamReader;

import org.sureassert.uc.annotation.Exemplar;

public class CSVReader {
  
  private InputStreamReader in;
  protected String[][] data;
  protected int row;
  protected int col;
  protected boolean isOpen;
 
  public CSVReader(InputStreamReader in) { 
    this.in = in; 
  }
  
  static class MockCSVReader extends CSVReader {
    
    @Exemplar(args = { "null" }, 
        expect = { "=(0, retval.data.length)", "=(retval.row,0)", "=(retval.col,0)", "retval.isOpen" })
    public MockCSVReader(String[][] data) {
      super(new InputStreamReader(System.in));
      if (data == null) {  
        this.data = new String[0][0];
      } else {  
        this.data = data;   
      }
      row = 0;
      col = 0;
      isOpen = true; 
    }
    
  }
}
