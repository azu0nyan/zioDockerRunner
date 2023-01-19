import java.io.PrintWriter;
import java.util.Scanner;
import java.io.File;

public class Main{
    public static void main(String[] args) throws Throwable{
        System.out.println("STARTED");
        PrintWriter pw = new PrintWriter(new File("test.txt"));
        pw.write("AAA");
        pw.close();
        Scanner s = new Scanner(System.in);
        System.out.println("RESPONSE" + s.nextLine());
        Thread.sleep(2000);
        System.out.println("RESPONSEE");
    }
}