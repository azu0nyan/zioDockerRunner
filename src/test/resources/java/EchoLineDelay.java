import java.util.Scanner;

public class Main{
    public static void main(String[] args) throws Throwable{
        Scanner s = new Scanner(System.in);
        long t = System.currentTimeMillis();
        while (System.currentTimeMillis() < t + 500){}
        System.out.print(s.nextLine());
    }
}