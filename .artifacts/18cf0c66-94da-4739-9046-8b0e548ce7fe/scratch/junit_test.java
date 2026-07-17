import org.junit.Assert;

public class junit_test {
    public static void main(String[] args) {
        long l = 300L;
        try {
            Assert.assertEquals(300, l);
            System.out.println("Success with int");
        } catch (AssertionError e) {
            System.out.println("Failed with int: " + e.getMessage());
        }
        
        try {
            Assert.assertEquals(300L, l);
            System.out.println("Success with long");
        } catch (AssertionError e) {
            System.out.println("Failed with long: " + e.getMessage());
        }
    }
}