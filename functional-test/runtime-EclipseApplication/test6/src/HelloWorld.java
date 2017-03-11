import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sureassert.uc.annotation.Exemplar;

public class HelloWorld {
  
  Logger logger = LoggerFactory.getLogger(HelloWorld.class);
  
  @Exemplar(expect="")
  @Test
  public void main() {
    logger.info("Hello World");
  }
} 