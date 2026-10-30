import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {
    enum Status { PRESENT, ABSENT }
    static final class Student {
        private final String rollNo, name;
        Student(String rollNo, String name) {
            this.rollNo = required(rollNo, "Roll number");
            this.name = required(name, "Name");
        }
        String getRollNo() { return rollNo; }
        String getName() { return name; }
    }
    static final class Course {
        private final String code, name;
        Course(String code, String name) {
            this.code = required(code, "Course code").toUpperCase(Locale.ROOT);
            this.name = required(name, "Course name");
        }
        String getCode() { return code; }
        String getName() { return name; }
    }
    static final class AttendanceRecord {
        private final Student student;
        private final Course course;
        private final LocalDate date;
        private final Status status;
        AttendanceRecord(Student s, Course c, LocalDate d, Status st) {
            student = Objects.requireNonNull(s);
            course = Objects.requireNonNull(c);
            date = Objects.requireNonNull(d);
            status = Objects.requireNonNull(st);
        }
    }
    static final class AttendanceManager {
        private final Map<String, Student> students = new LinkedHashMap<>();
        private final Map<String, Course> courses = new LinkedHashMap<>();
        private final List<AttendanceRecord> records = new ArrayList<>();
        void addStudent(String roll, String name) {
            Student s = new Student(roll, name);
            if (students.containsKey(s.rollNo))
                throw new IllegalArgumentException("Student already exists.");
            students.put(s.rollNo, s);
        }
        void addCourse(String code, String name) {
            Course c = new Course(code, name);
            if (courses.containsKey(c.code))
                throw new IllegalArgumentException("Course already exists.");
            courses.put(c.code, c);
        }
        Student findStudent(String roll) {
            Student s = students.get(required(roll, "Roll number"));
            if (s == null) throw new IllegalArgumentException("Unknown student.");
            return s;
        }
        Course findCourse(String code) {
            String key = required(code, "Course code").toUpperCase(Locale.ROOT);
            Course c = courses.get(key);
            if (c == null) throw new IllegalArgumentException("Unknown course.");
            return c;
        }
        void markAttendance(String roll, String code, LocalDate date, Status status) {
            Student s = findStudent(roll);
            Course c = findCourse(code);
            Objects.requireNonNull(date);
            Objects.requireNonNull(status);
            for (AttendanceRecord r : records)
                if (r.student == s && r.course == c && r.date.equals(date))
                    throw new IllegalArgumentException("Duplicate attendance.");
            records.add(new AttendanceRecord(s, c, date, status));
        }
        List<AttendanceRecord> getRecords(String roll, String code) {
            Student s = findStudent(roll);
            Course c = findCourse(code);
            List<AttendanceRecord> result = new ArrayList<>();
            for (AttendanceRecord r : records)
                if (r.student == s && r.course == c) result.add(r);
            result.sort(Comparator.comparing(r -> r.date));
            return result;
        }
        OptionalDouble percentage(String roll, String code) {
            List<AttendanceRecord> list = getRecords(roll, code);
            if (list.isEmpty()) return OptionalDouble.empty();
            long present = list.stream()
                .filter(r -> r.status == Status.PRESENT).count();
            return OptionalDouble.of(present * 100.0 / list.size());
        }
        void listStudents() {
            if (students.isEmpty()) System.out.println("No students registered.");
            for (Student s : students.values())
                System.out.println(s.rollNo + " | " + s.name);
        }
        void listCourses() {
            for (Course c : courses.values())
                System.out.println(c.code + " | " + c.name);
        }
    }
    static String required(String value, String label) {
        if (value == null || value.trim().isEmpty())
            throw new IllegalArgumentException(label + " cannot be blank.");
        return value.trim();
    }
    static String ask(Scanner s, String prompt) {
        System.out.print(prompt);
        return s.nextLine().trim();
    }
    static Status parseStatus(String text) {
        if (text.equalsIgnoreCase("P") || text.equalsIgnoreCase("Present"))
            return Status.PRESENT;
        if (text.equalsIgnoreCase("A") || text.equalsIgnoreCase("Absent"))
            return Status.ABSENT;
        throw new IllegalArgumentException("Enter P/Present or A/Absent.");
    }
    static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError("FAILED: " + name);
        System.out.println("PASS: " + name);
    }
    static void rejects(Runnable action, String name) {
        try { action.run(); }
        catch (IllegalArgumentException e) { check(true, name); return; }
        throw new AssertionError("FAILED: " + name);
    }
    static void selfTest() {
        AttendanceManager m = new AttendanceManager();
        m.addStudent("S001", "Demo Student");
        m.addCourse("JAVA", "Java OOP");
        check(m.findStudent("S001").name.equals("Demo Student"), "registration");
        check(!m.percentage("S001", "JAVA").isPresent(), "no records");
        LocalDate d = LocalDate.of(2026, 10, 8);
        m.markAttendance("S001", "JAVA", d, Status.PRESENT);
        m.markAttendance("S001", "JAVA", d.plusDays(1), Status.ABSENT);
        check(m.getRecords("S001", "JAVA").size() == 2, "retrieval");
        check(m.percentage("S001", "JAVA").getAsDouble() == 50.0, "percentage");
        rejects(() -> m.markAttendance("S001", "JAVA", d, Status.ABSENT),
            "duplicate attendance");
        rejects(() -> m.markAttendance("X", "JAVA", d, Status.PRESENT),
            "unknown student");
        rejects(() -> m.markAttendance("S001", "X", d, Status.PRESENT),
            "unknown course");
        rejects(() -> m.addStudent("S001", "Duplicate"), "duplicate student");
        rejects(() -> m.addCourse("java", "Duplicate"), "duplicate course");
        rejects(() -> m.addStudent("", "Blank"), "blank roll number");
        rejects(() -> parseStatus("X"), "invalid status");
        System.out.println("All 11 self-tests passed.");
    }
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--self-test")) {
            selfTest(); return;
        }
        AttendanceManager m = new AttendanceManager();
        m.addCourse("CCSE0355", "Object Oriented Techniques using Java");
        try (Scanner s = new Scanner(System.in)) {
            while (true) {
                System.out.println("\nSMART ATTENDANCE MANAGEMENT SYSTEM");
                System.out.println("1. Add student\n2. Add course");
                System.out.println("3. List students\n4. List courses");
                System.out.println("5. Mark attendance\n6. View attendance");
                System.out.println("7. Attendance percentage\n0. Exit");
                if (!s.hasNextLine()) break;
                String choice = ask(s, "Choose: ");
                try {
                    switch (choice) {
                        case "1":
                            m.addStudent(ask(s, "Roll: "), ask(s, "Name: "));
                            System.out.println("Student added."); break;
                        case "2":
                            m.addCourse(ask(s, "Code: "), ask(s, "Name: "));
                            System.out.println("Course added."); break;
                        case "3": m.listStudents(); break;
                        case "4": m.listCourses(); break;
                        case "5": {
                            String roll = ask(s, "Roll: ");
                            String code = ask(s, "Course code: ");
                            LocalDate date = LocalDate.parse(
                                ask(s, "Date (YYYY-MM-DD): "));
                            Status status = parseStatus(ask(s, "Status (P/A): "));
                            m.markAttendance(roll, code, date, status);
                            System.out.println("Attendance recorded."); break;
                        }
                        case "6": {
                            List<AttendanceRecord> list = m.getRecords(
                                ask(s, "Roll: "), ask(s, "Course code: "));
                            if (list.isEmpty())
                                System.out.println("No attendance records.");
                            for (AttendanceRecord r : list)
                                System.out.println(r.date + " | " + r.student.name
                                    + " | " + r.course.code + " | " + r.status);
                            break;
                        }
                        case "7": {
                            OptionalDouble p = m.percentage(
                                ask(s, "Roll: "), ask(s, "Course code: "));
                            if (p.isPresent())
                                System.out.printf(Locale.ROOT,
                                    "Attendance: %.2f%%%n", p.getAsDouble());
                            else System.out.println("No records; no percentage.");
                            break;
                        }
                        case "0":
                            System.out.println("Exiting. Records will be lost.");
                            return;
                        default: System.out.println("Invalid menu option.");
                    }
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid date. Use YYYY-MM-DD.");
                } catch (IllegalArgumentException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
    }
}
