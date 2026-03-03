import java.time.LocalDate;

public class Calendar {
    public static void main(String[] args) {
        System.out.println(getTodayDate());
    }

    public static String getTodayDate() {
        LocalDate today = LocalDate.now();
        return today.toString();
    }
}
