import java.util.*;
import java.util.stream.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;

/**
 * TROVE - A Treasure of Books
 * Library Management System | Java Console Edition
 *
 * HOW TO RUN IN VS CODE TERMINAL:
 *   1. javac TROVE.java
 *   2. java TROVE
 *
 * DEMO LOGINS:
 *   Admin   -> admin@library.com / admin123
 *   Student -> alice@college.edu / alice123
 */
public class TROVE {

    // =========================================================
    // ANSI COLOURS
    // =========================================================
    static final String R  = "\u001B[0m";
    static final String BD = "\u001B[1m";
    static final String DM = "\u001B[2m";
    static final String RD = "\u001B[31m";
    static final String GR = "\u001B[32m";
    static final String YL = "\u001B[33m";
    static final String BL = "\u001B[34m";
    static final String MG = "\u001B[35m";
    static final String CY = "\u001B[36m";
    static final String WH = "\u001B[37m";
    static final String BGC = "\u001B[46m";
    static final String BK  = "\u001B[30m";

    // =========================================================
    // MODELS
    // =========================================================
    static class Book {
        static int SEQ = 26;
        int id; String title, author, genre, status;
        int year; double rating;

        Book(int id, String title, String author, String genre,
             int year, String status, double rating) {
            this.id = id; this.title = title; this.author = author;
            this.genre = genre; this.year = year;
            this.status = status; this.rating = rating;
        }

        Book(String title, String author, String genre, int year) {
            this(SEQ++, title, author, genre, year, "Available", 0.0);
        }

        boolean available() { return "Available".equals(status); }

        String stars() {
            int s = (int) Math.round(rating);
            return YL + "*".repeat(Math.max(0, Math.min(s, 5)))
                 + DM + "-".repeat(Math.max(0, 5 - Math.min(s, 5))) + R
                 + DM + " " + String.format("%.1f", rating) + R;
        }
    }

    static class Member {
        static int SEQ = 5;
        int id; String name, year, roll, email, joined;
        List<String> interests;

        Member(int id, String name, String year, String roll,
               String email, String joined, List<String> interests) {
            this.id = id; this.name = name; this.year = year;
            this.roll = roll; this.email = email; this.joined = joined;
            this.interests = interests != null ? new ArrayList<>(interests) : new ArrayList<>();
        }

        Member(String name, String year, String roll, String email) {
            this(SEQ++, name, year, roll, email,
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                new ArrayList<>());
        }
    }

    static class User {
        String email, password, name, role, roll, year;
        List<String> interests;

        User(String email, String password, String name, String role,
             String roll, String year, List<String> interests) {
            this.email = email; this.password = password; this.name = name;
            this.role = role; this.roll = roll; this.year = year;
            this.interests = interests != null ? new ArrayList<>(interests) : new ArrayList<>();
        }

        boolean isAdmin()   { return "admin".equals(role); }
        boolean isStudent() { return "student".equals(role); }
    }

    static class Borrower {
        static int SEQ = 1;
        static final int FINE_PER_DAY = 2;
        static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

        int id, memberId, bookId;
        String memberName, memberEmail, bookTitle, status;
        LocalDate borrowed, due;

        Borrower(int memberId, String memberName, String memberEmail,
                 int bookId, String bookTitle, int dueDays) {
            this.id = SEQ++;
            this.memberId = memberId; this.memberName = memberName;
            this.memberEmail = memberEmail; this.bookId = bookId;
            this.bookTitle = bookTitle;
            this.borrowed = LocalDate.now();
            this.due = borrowed.plusDays(dueDays);
            this.status = "Active";
        }

        boolean returned()   { return "Returned".equals(status); }

        long daysOverdue() {
            if (returned()) return 0;
            return Math.max(0, ChronoUnit.DAYS.between(due, LocalDate.now()));
        }

        int fine()           { return (int)(daysOverdue() * FINE_PER_DAY); }
        String borrowedStr() { return borrowed.format(FMT); }
        String dueStr()      { return due.format(FMT); }

        String statusLabel() {
            if (returned()) return "Returned";
            long d = daysOverdue();
            if (d > 0) return "Overdue(" + d + "d)";
            long left = ChronoUnit.DAYS.between(LocalDate.now(), due);
            if (left <= 3) return "Due soon(" + left + "d)";
            return "Active";
        }
    }

    // =========================================================
    // DATA STORES
    // =========================================================
    static List<Book>     books     = new ArrayList<>();
    static List<Member>   members   = new ArrayList<>();
    static List<Borrower> borrowers = new ArrayList<>();
    static List<User>     users     = new ArrayList<>();
    static Deque<String>  notifs    = new ArrayDeque<>();
    static User           me        = null;

    static final String[] GENRES =
        {"Fiction", "Non-Fiction", "Science", "History", "Technology", "Biography", "Other"};

    // =========================================================
    // ENTRY POINT
    // =========================================================
    public static void main(String[] args) {
        seed();
        Scanner sc = new Scanner(System.in);
        while (true) {
            authScreen(sc);
            if (me == null) break;
            if (me.isAdmin()) adminMenu(sc);
            else              studentMenu(sc);
        }
        banner();
        say(CY + BD + "  Thanks for using TROVE! Happy Reading!" + R);
        System.out.println();
        sc.close();
    }

    // =========================================================
    // SEED DATA
    // =========================================================
    static void seed() {
        books.addAll(List.of(
            new Book(1,  "The Great Gatsby",                   "F. Scott Fitzgerald", "Fiction",     1925, "Available", 4.2),
            new Book(2,  "To Kill a Mockingbird",              "Harper Lee",          "Fiction",     1960, "Available", 4.5),
            new Book(3,  "1984",                               "George Orwell",       "Fiction",     1949, "Available", 4.7),
            new Book(4,  "A Brief History of Time",            "Stephen Hawking",     "Science",     1988, "Available", 4.3),
            new Book(5,  "Sapiens",                            "Yuval Noah Harari",   "History",     2011, "Available", 4.1),
            new Book(6,  "Pride and Prejudice",                "Jane Austen",         "Fiction",     1813, "Available", 4.6),
            new Book(7,  "The Alchemist",                      "Paulo Coelho",        "Fiction",     1988, "Available", 4.4),
            new Book(8,  "Brave New World",                    "Aldous Huxley",       "Fiction",     1932, "Available", 4.3),
            new Book(9,  "The Catcher in the Rye",             "J.D. Salinger",       "Fiction",     1951, "Available", 4.0),
            new Book(10, "The Hobbit",                         "J.R.R. Tolkien",      "Fiction",     1937, "Available", 4.8),
            new Book(11, "Harry Potter and the Sorcerers Stone","J.K. Rowling",       "Fiction",     1997, "Available", 4.9),
            new Book(12, "The Da Vinci Code",                  "Dan Brown",           "Fiction",     2003, "Available", 4.0),
            new Book(13, "Gone with the Wind",                 "Margaret Mitchell",   "Fiction",     1936, "Available", 4.3),
            new Book(14, "Thinking, Fast and Slow",            "Daniel Kahneman",     "Non-Fiction", 2011, "Available", 4.5),
            new Book(15, "Atomic Habits",                      "James Clear",         "Non-Fiction", 2018, "Available", 4.8),
            new Book(16, "The Power of Now",                   "Eckhart Tolle",       "Non-Fiction", 1997, "Available", 4.2),
            new Book(17, "Educated",                           "Tara Westover",       "Biography",   2018, "Available", 4.7),
            new Book(18, "The Selfish Gene",                   "Richard Dawkins",     "Science",     1976, "Available", 4.4),
            new Book(19, "Clean Code",                         "Robert C. Martin",    "Technology",  2008, "Available", 4.6),
            new Book(20, "The Pragmatic Programmer",           "David Thomas",        "Technology",  1999, "Available", 4.5),
            new Book(21, "Introduction to Algorithms",         "Cormen et al.",       "Technology",  2009, "Available", 4.7),
            new Book(22, "The Diary of a Young Girl",          "Anne Frank",          "Biography",   1947, "Available", 4.8),
            new Book(23, "Steve Jobs",                         "Walter Isaacson",     "Biography",   2011, "Available", 4.3),
            new Book(24, "Guns, Germs, and Steel",             "Jared Diamond",       "History",     1997, "Available", 4.2),
            new Book(25, "The Art of War",                     "Sun Tzu",             "History",     -500, "Available", 4.5)
        ));

        members.addAll(List.of(
            new Member(1, "Alice Johnson", "2nd Year", "CS-201", "alice@college.edu", "01 Jan 2024", List.of("Fiction", "Science")),
            new Member(2, "Bob Smith",     "1st Year", "EE-105", "bob@college.edu",   "15 Feb 2024", List.of("Technology", "Non-Fiction")),
            new Member(3, "Clara Mendes",  "3rd Year", "ME-312", "clara@college.edu", "10 Mar 2024", List.of("History", "Biography")),
            new Member(4, "David Kumar",   "2nd Year", "CS-205", "david@college.edu", "20 Mar 2024", List.of("Science", "Technology"))
        ));

        users.addAll(List.of(
            new User("admin@library.com", "admin123", "Admin",         "admin",   "",       "",         List.of()),
            new User("alice@college.edu", "alice123", "Alice Johnson", "student", "CS-201", "2nd Year", List.of("Fiction", "Science")),
            new User("bob@college.edu",   "bob123",   "Bob Smith",     "student", "EE-105", "1st Year", List.of("Technology", "Non-Fiction")),
            new User("clara@college.edu", "clara123", "Clara Mendes",  "student", "ME-312", "3rd Year", List.of("History", "Biography")),
            new User("david@college.edu", "david123", "David Kumar",   "student", "CS-205", "2nd Year", List.of("Science", "Technology"))
        ));
    }

    // =========================================================
    // AUTHENTICATION
    // =========================================================
    static void authScreen(Scanner sc) {
        while (true) {
            banner();
            say(BD + "  Welcome to TROVE - Library Management System" + R);
            say("");
            say(DM + "  Demo logins:" + R);
            say(DM + "    Admin   ->  admin@library.com  /  admin123" + R);
            say(DM + "    Student ->  alice@college.edu  /  alice123" + R);
            say("");
            menuHead("MAIN MENU");
            item(1, "Sign In");
            item(2, "Register (New Student)");
            item(0, "Exit");
            say("");
            switch (ask(sc, "Choice")) {
                case "1" -> { if (doLogin(sc)) return; }
                case "2" -> doRegister(sc);
                case "0" -> { me = null; return; }
                default  -> err("Invalid choice.");
            }
        }
    }

    static boolean doLogin(Scanner sc) {
        sec("SIGN IN");
        String email = ask(sc, "Email");
        String pass  = ask(sc, "Password");
        for (User u : users) {
            if (u.email.equalsIgnoreCase(email) && u.password.equals(pass)) {
                me = u;
                notif("Logged in as " + u.name);
                ok("Welcome back, " + BD + u.name + R + GR + "!  [" + u.role.toUpperCase() + "]");
                sleep(800);
                return true;
            }
        }
        err("Wrong email or password.");
        sleep(900);
        return false;
    }

    static void doRegister(Scanner sc) {
        sec("STUDENT REGISTRATION");
        String name  = ask(sc, "Full Name *");
        String roll  = ask(sc, "Roll No  (e.g. CS-301)");
        String year  = ask(sc, "Year     (e.g. 2nd Year)");
        String email = ask(sc, "Email *");
        String pass  = ask(sc, "Password *");
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            err("Name, email and password are required."); sleep(1000); return;
        }
        if (users.stream().anyMatch(u -> u.email.equalsIgnoreCase(email))) {
            err("Email already registered."); sleep(1000); return;
        }
        users.add(new User(email, pass, name, "student", roll, year, List.of()));
        members.add(new Member(name, year, roll, email));
        notif("New student registered: " + name);
        ok("Account created! You can now sign in.");
        sleep(1200);
    }

    // =========================================================
    // ADMIN MENU
    // =========================================================
    static void adminMenu(Scanner sc) {
        while (true) {
            banner();
            adminStats();
            menuHead("ADMIN DASHBOARD - " + me.name);
            item(1,  "Books         - View / Search / Sort / Add / Edit / Delete");
            item(2,  "Members       - View / Search / Add / Edit / Delete");
            item(3,  "Borrowers     - All records + Active / Overdue / Returned filter");
            item(4,  "Issue Book    - Lend a book to a member");
            item(5,  "Return Book   - Mark book as returned + auto fine");
            item(6,  "Fines         - Overdue fines (Rs.2 / day)");
            item(7,  "Genre Chart   - Bar chart: books per genre");
            item(8,  "ID Card       - Print student library ID card");
            item(9,  "Notifications - " + notifs.size() + " unread");
            item(10, "My Profile    - Stats + change password");
            item(11, "Export CSV    - Books / Members / Borrowers");
            item(0,  "Logout");
            say("");
            switch (ask(sc, "Choice")) {
                case "1"  -> booksMenu(sc);
                case "2"  -> membersMenu(sc);
                case "3"  -> borrowersMenu(sc);
                case "4"  -> issueBook(sc);
                case "5"  -> returnBook(sc);
                case "6"  -> finesPanel(sc);
                case "7"  -> genreChart(sc);
                case "8"  -> idCard(sc);
                case "9"  -> notifsPanel(sc);
                case "10" -> adminProfile(sc);
                case "11" -> exportMenu(sc);
                case "0"  -> { me = null; return; }
                default   -> { err("Invalid option."); sleep(500); }
            }
        }
    }

    static void adminStats() {
        long avail  = books.stream().filter(Book::available).count();
        long active = borrowers.stream().filter(b -> !b.returned()).count();
        long over   = borrowers.stream().filter(b -> b.daysOverdue() > 0).count();
        int  fines  = borrowers.stream().mapToInt(Borrower::fine).sum();
        say("");
        line('=');
        say(BD + "  LIBRARY OVERVIEW" + R);
        line('-');
        System.out.printf("    %-30s  %-20s  %-20s  %-20s  %-20s%n",
            CY + BD + "Total Books: " + books.size() + " (" + avail + " avail)" + R,
            GR + BD + "Members: " + members.size() + R,
            YL + BD + "Books Out: " + active + R,
            RD + BD + "Overdue: " + over + R,
            RD + BD + "Fines: Rs." + fines + R);
        line('=');
    }

    // ---------------------------------------------------------
    // BOOKS
    // ---------------------------------------------------------
    static void booksMenu(Scanner sc) {
        while (true) {
            sec("BOOKS MANAGEMENT");
            item(1, "View All Books");
            item(2, "Search / Filter Books");
            item(3, "Sort Books");
            item(4, "Add New Book");
            item(5, "Edit Book");
            item(6, "Delete Book");
            item(0, "Back");
            say("");
            switch (ask(sc, "Choice")) {
                case "1" -> showBooks(sc, new ArrayList<>(books), "All Books");
                case "2" -> searchBooks(sc);
                case "3" -> sortBooks(sc);
                case "4" -> addBook(sc);
                case "5" -> editBook(sc);
                case "6" -> deleteBook(sc);
                case "0" -> { return; }
                default  -> { err("Invalid option."); sleep(500); }
            }
        }
    }

    static void showBooks(Scanner sc, List<Book> list, String title) {
        ph("BOOKS", title, list.size() + " book(s)");
        if (list.isEmpty()) {
            warn("No books to display.");
        } else {
            say(DM + "  " + w("ID", 4) + "  " + w("Title", 38) + "  " + w("Author", 22)
                + "  " + w("Genre", 12) + "  " + w("Year", 5) + "  " + w("Status", 10) + "  Rating" + R);
            line('-');
            for (Book b : list) {
                String st = b.available()
                    ? GR + BD + w("Available", 10) + R
                    : RD + BD + w("Issued",    10) + R;
                System.out.printf("  " + CY + "%-4d" + R + "  %-38s  %-22s  %-12s  %5d  %s  %s%n",
                    b.id, w(b.title, 38), w(b.author, 22),
                    b.genre, b.year, st, b.stars());
            }
        }
        enter(sc);
    }

    static void searchBooks(Scanner sc) {
        sec("SEARCH BOOKS");
        String q  = ask(sc, "Keyword (title/author/genre - blank = all)").toLowerCase();
        String g  = pickGenre(sc, "Filter Genre (blank = all)");
        String st = ask(sc, "Filter Status [Available / Issued / blank]");
        List<Book> res = books.stream()
            .filter(b -> q.isEmpty() || b.title.toLowerCase().contains(q)
                || b.author.toLowerCase().contains(q) || b.genre.toLowerCase().contains(q))
            .filter(b -> g.isEmpty()  || b.genre.equalsIgnoreCase(g))
            .filter(b -> st.isEmpty() || b.status.equalsIgnoreCase(st))
            .collect(Collectors.toList());
        showBooks(sc, res, "Search Results");
    }

    static void sortBooks(Scanner sc) {
        sec("SORT BOOKS");
        item(1, "Title A to Z");
        item(2, "Title Z to A");
        item(3, "Author A to Z");
        item(4, "Genre");
        item(5, "Year Newest First");
        item(6, "Year Oldest First");
        item(7, "Rating High to Low");
        item(8, "Status");
        say("");
        List<Book> sorted = new ArrayList<>(books);
        switch (ask(sc, "Sort by")) {
            case "1" -> sorted.sort(Comparator.comparing(b -> b.title.toLowerCase()));
            case "2" -> sorted.sort(Comparator.comparing((Book b) -> b.title.toLowerCase()).reversed());
            case "3" -> sorted.sort(Comparator.comparing(b -> b.author.toLowerCase()));
            case "4" -> sorted.sort(Comparator.comparing(b -> b.genre));
            case "5" -> sorted.sort(Comparator.comparingInt((Book b) -> b.year).reversed());
            case "6" -> sorted.sort(Comparator.comparingInt(b -> b.year));
            case "7" -> sorted.sort(Comparator.comparingDouble((Book b) -> b.rating).reversed());
            case "8" -> sorted.sort(Comparator.comparing(b -> b.status));
            default  -> warn("Invalid, showing unsorted.");
        }
        showBooks(sc, sorted, "Sorted Books");
    }

    static void addBook(Scanner sc) {
        sec("ADD NEW BOOK");
        String title = ask(sc, "Title *");
        if (title.isEmpty()) { err("Title is required."); sleep(900); return; }
        String author = ask(sc, "Author");
        String genre  = pickGenre(sc, "Genre");
        if (genre.isEmpty()) genre = "Other";
        int year = intAsk(sc, "Year Published");
        Book b = new Book(title, author, genre, year);
        books.add(b);
        notif("Book added: \"" + title + "\" ID=" + b.id);
        ok("Book added!  ID = " + b.id);
        sleep(1000);
    }

    static void editBook(Scanner sc) {
        sec("EDIT BOOK");
        int id = intAsk(sc, "Book ID to edit");
        Book b = findBook(id);
        if (b == null) { err("Book not found."); sleep(900); return; }
        inf("Current: \"" + b.title + "\" by " + b.author + " [" + b.genre + "] " + b.year);
        say(DM + "  Leave blank to keep current value." + R + "\n");
        String t = askOpt(sc, "Title",  b.title);  if (!t.isEmpty()) b.title  = t;
        String a = askOpt(sc, "Author", b.author); if (!a.isEmpty()) b.author = a;
        String g = pickGenre(sc, "Genre (blank = keep)"); if (!g.isEmpty()) b.genre = g;
        String y = askOpt(sc, "Year",   String.valueOf(b.year));
        if (!y.isEmpty()) try { b.year = Integer.parseInt(y); } catch (Exception ignored) {}
        notif("Book updated: \"" + b.title + "\"");
        ok("Book updated.");
        sleep(900);
    }

    static void deleteBook(Scanner sc) {
        sec("DELETE BOOK");
        int id = intAsk(sc, "Book ID to delete");
        Book b = findBook(id);
        if (b == null)      { err("Book not found."); sleep(900); return; }
        if (!b.available()) { err("Cannot delete an issued book. Return it first."); sleep(1100); return; }
        say(YL + "  About to delete: \"" + b.title + "\"" + R);
        if ("YES".equals(ask(sc, "Type YES to confirm"))) {
            books.remove(b);
            notif("Book deleted: \"" + b.title + "\"");
            ok("Book deleted.");
        } else {
            inf("Cancelled.");
        }
        sleep(900);
    }

    // ---------------------------------------------------------
    // MEMBERS
    // ---------------------------------------------------------
    static void membersMenu(Scanner sc) {
        while (true) {
            sec("MEMBERS MANAGEMENT");
            item(1, "View All Members");
            item(2, "Search Members");
            item(3, "Add New Member");
            item(4, "Edit Member");
            item(5, "Delete Member");
            item(0, "Back");
            say("");
            switch (ask(sc, "Choice")) {
                case "1" -> showMembers(sc, new ArrayList<>(members), "All Members");
                case "2" -> searchMembers(sc);
                case "3" -> addMember(sc);
                case "4" -> editMember(sc);
                case "5" -> deleteMember(sc);
                case "0" -> { return; }
                default  -> { err("Invalid option."); sleep(500); }
            }
        }
    }

    static void showMembers(Scanner sc, List<Member> list, String title) {
        ph("MEMBERS", title, list.size() + " member(s)");
        if (list.isEmpty()) {
            warn("No members to display.");
        } else {
            say(DM + "  " + w("ID", 4) + "  " + w("Name", 22) + "  " + w("Year", 10)
                + "  " + w("Roll", 8) + "  " + w("Email", 28) + "  " + w("Joined", 12)
                + "  Out  Interests" + R);
            line('-');
            for (Member m : list) {
                long out = borrowers.stream()
                    .filter(b -> b.memberId == m.id && !b.returned()).count();
                System.out.printf("  " + CY + "%-4d" + R
                    + "  %-22s  %-10s  %-8s  %-28s  %-12s  %-4d  %s%n",
                    m.id, w(m.name, 22), w(m.year, 10), m.roll,
                    m.email, m.joined, out,
                    m.interests.isEmpty()
                        ? DM + "None" + R
                        : MG + String.join(", ", m.interests) + R);
            }
        }
        enter(sc);
    }

    static void searchMembers(Scanner sc) {
        sec("SEARCH MEMBERS");
        String q = ask(sc, "Keyword (name / roll / email - blank = all)").toLowerCase();
        List<Member> res = members.stream()
            .filter(m -> q.isEmpty() || m.name.toLowerCase().contains(q)
                || m.roll.toLowerCase().contains(q) || m.email.toLowerCase().contains(q))
            .collect(Collectors.toList());
        showMembers(sc, res, "Search Results");
    }

    static void addMember(Scanner sc) {
        sec("ADD NEW MEMBER");
        String name  = ask(sc, "Full Name *");
        if (name.isEmpty()) { err("Name is required."); sleep(900); return; }
        String year  = ask(sc, "Year  (e.g. 2nd Year)");
        String roll  = ask(sc, "Roll No");
        String email = ask(sc, "Email");
        Member m = new Member(name, year, roll, email);
        members.add(m);
        if (!email.isEmpty()) {
            boolean dup = users.stream().anyMatch(u -> u.email.equalsIgnoreCase(email));
            if (!dup) {
                String defPass = roll.isEmpty() ? "trove123" : roll.toLowerCase();
                users.add(new User(email, defPass, name, "student", roll, year, List.of()));
                inf("Login auto-created -> email: " + email + "  password: " + defPass);
            }
        }
        notif("Member added: " + name + " (ID " + m.id + ")");
        ok("Member added!  ID = " + m.id);
        sleep(1100);
    }

    static void editMember(Scanner sc) {
        sec("EDIT MEMBER");
        int id = intAsk(sc, "Member ID to edit");
        Member m = findMember(id);
        if (m == null) { err("Member not found."); sleep(900); return; }
        inf("Current: " + m.name + " | " + m.year + " | " + m.roll + " | " + m.email);
        say(DM + "  Leave blank to keep current value." + R + "\n");
        String n = askOpt(sc, "Name",  m.name);  if (!n.isEmpty()) m.name  = n;
        String y = askOpt(sc, "Year",  m.year);  if (!y.isEmpty()) m.year  = y;
        String r = askOpt(sc, "Roll",  m.roll);  if (!r.isEmpty()) m.roll  = r;
        String e = askOpt(sc, "Email", m.email); if (!e.isEmpty()) m.email = e;
        notif("Member updated: " + m.name);
        ok("Member updated.");
        sleep(900);
    }

    static void deleteMember(Scanner sc) {
        sec("DELETE MEMBER");
        int id = intAsk(sc, "Member ID to delete");
        Member m = findMember(id);
        if (m == null) { err("Member not found."); sleep(900); return; }
        boolean hasActive = borrowers.stream()
            .anyMatch(b -> b.memberId == id && !b.returned());
        if (hasActive) {
            err("Cannot delete - member has active borrows. Return all books first.");
            sleep(1300); return;
        }
        say(YL + "  About to delete: " + m.name + R);
        if ("YES".equals(ask(sc, "Type YES to confirm"))) {
            members.remove(m);
            users.removeIf(u -> u.email.equalsIgnoreCase(m.email));
            notif("Member deleted: " + m.name);
            ok("Member deleted.");
        } else {
            inf("Cancelled.");
        }
        sleep(900);
    }

    // ---------------------------------------------------------
    // BORROWERS
    // ---------------------------------------------------------
    static void borrowersMenu(Scanner sc) {
        while (true) {
            sec("BORROWERS - ISSUE RECORDS");
            item(1, "All Records");
            item(2, "Active Only");
            item(3, "Overdue Only");
            item(4, "Returned Only");
            item(0, "Back");
            say("");
            switch (ask(sc, "Choice")) {
                case "1" -> showBorrowers(sc, new ArrayList<>(borrowers), "All Records");
                case "2" -> showBorrowers(sc,
                    borrowers.stream().filter(b -> !b.returned() && b.daysOverdue() == 0).collect(Collectors.toList()),
                    "Active");
                case "3" -> showBorrowers(sc,
                    borrowers.stream().filter(b -> b.daysOverdue() > 0).collect(Collectors.toList()),
                    "Overdue");
                case "4" -> showBorrowers(sc,
                    borrowers.stream().filter(Borrower::returned).collect(Collectors.toList()),
                    "Returned");
                case "0" -> { return; }
                default  -> { err("Invalid option."); sleep(500); }
            }
        }
    }

    static void showBorrowers(Scanner sc, List<Borrower> list, String title) {
        ph("BORROWERS", title, list.size() + " record(s)");
        if (list.isEmpty()) {
            warn("No records found.");
        } else {
            say(DM + "  " + w("ID", 4) + "  " + w("Member", 20) + "  " + w("Book", 32)
                + "  " + w("Borrowed", 12) + "  " + w("Due", 12) + "  " + w("Status", 16) + "  Fine" + R);
            line('-');
            for (Borrower b : list) {
                String st;
                if (b.returned())           st = GR + w("Returned", 16) + R;
                else if (b.daysOverdue()>0) st = RD + BD + w("OVERDUE " + b.daysOverdue() + "d", 16) + R;
                else                        st = YL + w(b.statusLabel(), 16) + R;
                System.out.printf("  " + CY + "%-4d" + R
                    + "  %-20s  %-32s  %-12s  %-12s  %s  %s%n",
                    b.id, w(b.memberName, 20), w(b.bookTitle, 32),
                    b.borrowedStr(), b.dueStr(), st,
                    b.fine() > 0 ? RD + BD + "Rs." + b.fine() + R : GR + "Rs.0" + R);
            }
        }
        enter(sc);
    }

    // ---------------------------------------------------------
    // ISSUE BOOK
    // ---------------------------------------------------------
    static void issueBook(Scanner sc) {
        sec("ISSUE BOOK");
        if (members.isEmpty()) { err("No members registered."); sleep(900); return; }
        say(DM + "  Members:" + R);
        for (Member m : members)
            System.out.printf("    " + CY + "[%d]" + R + "  %-22s  %s%n",
                m.id, m.name, DM + m.roll + R);
        say("");
        int mid = intAsk(sc, "Select Member ID");
        Member member = findMember(mid);
        if (member == null) { err("Member not found."); sleep(900); return; }

        List<Book> avail = books.stream().filter(Book::available).collect(Collectors.toList());
        if (avail.isEmpty()) { err("No books available."); sleep(900); return; }
        say("");
        say(DM + "  Available Books:" + R);
        for (Book b : avail)
            System.out.printf("    " + CY + "[%d]" + R + "  %-38s  %s%n",
                b.id, b.title, DM + b.author + R);
        say("");
        int bid = intAsk(sc, "Select Book ID");
        Book book = avail.stream().filter(b -> b.id == bid).findFirst().orElse(null);
        if (book == null) { err("Book not available."); sleep(900); return; }

        say("");
        item(1, " 7 days");
        item(2, "14 days");
        item(3, "21 days");
        item(4, "30 days");
        say("");
        int days = switch (ask(sc, "Due period")) {
            case "2" -> 14;
            case "3" -> 21;
            case "4" -> 30;
            default  ->  7;
        };
        Borrower rec = new Borrower(member.id, member.name, member.email,
                                    book.id, book.title, days);
        borrowers.add(rec);
        book.status = "Issued";
        notif("Issued \"" + book.title + "\" to " + member.name);
        ok("Book issued to " + member.name + "! Due in " + days + " days.");
        sleep(1200);
    }

    // ---------------------------------------------------------
    // RETURN BOOK
    // ---------------------------------------------------------
    static void returnBook(Scanner sc) {
        sec("RETURN BOOK");
        List<Borrower> active = borrowers.stream()
            .filter(b -> !b.returned()).collect(Collectors.toList());
        if (active.isEmpty()) { warn("No active borrows."); sleep(900); return; }
        showBorrowers(sc, active, "Active Borrows");
        int id = intAsk(sc, "Borrower Record ID to return");
        Borrower rec = active.stream().filter(b -> b.id == id).findFirst().orElse(null);
        if (rec == null) { err("Record not found."); sleep(900); return; }
        rec.status = "Returned";
        Book book = findBook(rec.bookId);
        if (book != null) book.status = "Available";
        int fine = rec.fine();
        if (fine > 0) {
            notif("Fine Rs." + fine + " - " + rec.memberName + " for \"" + rec.bookTitle + "\"");
            warn("Book returned with fine of Rs." + fine + " (" + rec.daysOverdue() + " days overdue).");
        } else {
            notif("\"" + rec.bookTitle + "\" returned by " + rec.memberName);
            ok("Book returned successfully!");
        }
        sleep(1300);
    }

    // ---------------------------------------------------------
    // FINES
    // ---------------------------------------------------------
    static void finesPanel(Scanner sc) {
        sec("FINES - OVERDUE BOOKS (Rs.2 / day)");
        List<Borrower> over = borrowers.stream()
            .filter(b -> b.daysOverdue() > 0)
            .sorted(Comparator.comparingLong(Borrower::daysOverdue).reversed())
            .collect(Collectors.toList());
        if (over.isEmpty()) {
            say("");
            ok("No overdue books! All returns are on time.");
        } else {
            say(DM + "  " + w("ID", 4) + "  " + w("Member", 20) + "  " + w("Book", 34)
                + "  " + w("Due Date", 12) + "  " + w("Days Late", 9) + "  Fine" + R);
            line('-');
            int total = 0;
            for (Borrower b : over) {
                total += b.fine();
                System.out.printf("  " + CY + "%-4d" + R
                    + "  %-20s  %-34s  %-12s  " + RD + BD + "%9d" + R
                    + "  " + RD + BD + "Rs.%d" + R + "%n",
                    b.id, w(b.memberName, 20), w(b.bookTitle, 34),
                    b.dueStr(), b.daysOverdue(), b.fine());
            }
            line('-');
            say(BD + RD + "  TOTAL FINES DUE:  Rs." + total + R);
        }
        enter(sc);
    }

    // ---------------------------------------------------------
    // GENRE CHART
    // ---------------------------------------------------------
    static void genreChart(Scanner sc) {
        sec("LIBRARY OVERVIEW - BOOKS PER GENRE");
        say("");
        Map<String, Long> counts = books.stream()
            .collect(Collectors.groupingBy(b -> b.genre, Collectors.counting()));
        long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(1);
        int barW = 40;
        String[] cols = {CY, GR, YL, MG, BL, RD, WH};
        int ci = 0;
        for (String g : GENRES) {
            long cnt   = counts.getOrDefault(g, 0L);
            int  bar   = (int)((cnt * barW) / Math.max(max, 1));
            long avail = books.stream().filter(b -> b.genre.equals(g) && b.available()).count();
            String c   = cols[ci++ % cols.length];
            System.out.printf("  %s%-13s%s |%s%s%s %d  (%d avail)%n",
                BD, g, R, c, "#".repeat(bar), R, cnt, avail);
        }
        say("");
        say(DM + "  Each # = approx " + String.format("%.1f", (double) max / barW) + " books" + R);
        enter(sc);
    }

    // ---------------------------------------------------------
    // ID CARD
    // ---------------------------------------------------------
    static void idCard(Scanner sc) {
        sec("STUDENT ID CARD PRINTER");
        if (members.isEmpty()) { warn("No members."); sleep(900); return; }
        say(DM + "  Members:" + R);
        for (Member m : members)
            System.out.printf("    " + CY + "[%d]" + R + "  %s%n", m.id, m.name);
        say("");
        int id = intAsk(sc, "Member ID");
        Member m = findMember(id);
        if (m == null) { err("Member not found."); sleep(900); return; }
        long active = borrowers.stream().filter(b -> b.memberId == m.id && !b.returned()).count();
        long total  = borrowers.stream().filter(b -> b.memberId == m.id).count();
        say("");
        say(BGC + BK + BD + "  +----------------------------------------------------+  " + R);
        say(BGC + BK + BD + "  |       TROVE LIBRARY  -  STUDENT ID CARD            |  " + R);
        say(BGC + BK + BD + "  +----------------------------------------------------+  " + R);
        say("");
        System.out.printf("    %-20s  " + BD + "%s" + R + "%n", "Name:",         m.name);
        System.out.printf("    %-20s  " + BD + "%s" + R + "%n", "Roll No:",      m.roll);
        System.out.printf("    %-20s  " + BD + "%s" + R + "%n", "Year:",         m.year);
        System.out.printf("    %-20s  " + BD + "%s" + R + "%n", "Email:",        m.email);
        System.out.printf("    %-20s  " + BD + "%s" + R + "%n", "Member Since:", m.joined);
        System.out.printf("    %-20s  " + BD + "%d active  /  %d total" + R + "%n", "Books:", active, total);
        System.out.printf("    %-20s  " + MG + BD + "%s" + R + "%n", "Interests:",
            m.interests.isEmpty() ? "None" : String.join(", ", m.interests));
        say("");
        say(DM + "  ||| |||| | |||| ||||| || | ||| |||| | ||||| |||| ||" + R);
        say(DM + "  Member ID: " + String.format("%06d", m.id)
            + "          TROVE-LIB-" + LocalDate.now().getYear() + R);
        enter(sc);
    }

    // ---------------------------------------------------------
    // NOTIFICATIONS
    // ---------------------------------------------------------
    static void notifsPanel(Scanner sc) {
        sec("NOTIFICATIONS (" + notifs.size() + ")");
        if (notifs.isEmpty()) {
            inf("No notifications yet.");
        } else {
            int i = 1;
            for (String n : notifs) say("  " + DM + i++ + "." + R + "  " + n);
        }
        say("");
        item(1, "Clear All");
        item(0, "Back");
        say("");
        if ("1".equals(ask(sc, "Choice"))) {
            notifs.clear(); ok("Cleared."); sleep(700);
        }
    }

    static void notif(String msg) {
        String t = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        notifs.addFirst("[" + t + "]  " + msg);
        if (notifs.size() > 30) notifs.removeLast();
    }

    // ---------------------------------------------------------
    // ADMIN PROFILE
    // ---------------------------------------------------------
    static void adminProfile(Scanner sc) {
        sec("ADMIN PROFILE");
        say("");
        stat("Name",           me.name,  "cyan");
        stat("Email",          me.email, "cyan");
        stat("Role",           "Administrator", "magenta");
        stat("Total Books",    String.valueOf(books.size()),    "green");
        stat("Total Members",  String.valueOf(members.size()),  "green");
        stat("Total Borrowers",String.valueOf(borrowers.size()),"yellow");
        int tf = borrowers.stream().mapToInt(Borrower::fine).sum();
        stat("Fines Outstanding", "Rs." + tf, tf > 0 ? "red" : "green");
        say("");
        item(1, "Change Password");
        item(0, "Back");
        say("");
        if ("1".equals(ask(sc, "Choice"))) changePassword(sc);
    }

    static void changePassword(Scanner sc) {
        String cur = ask(sc, "Current Password");
        if (!cur.equals(me.password)) { err("Wrong current password."); sleep(1000); return; }
        String nw  = ask(sc, "New Password");
        if (nw.isEmpty()) { err("Password cannot be blank."); sleep(900); return; }
        String cf  = ask(sc, "Confirm New Password");
        if (!nw.equals(cf)) { err("Passwords do not match."); sleep(900); return; }
        me.password = nw;
        users.stream().filter(u -> u.email.equalsIgnoreCase(me.email))
            .findFirst().ifPresent(u -> u.password = nw);
        notif("Password changed for " + me.name);
        ok("Password updated!");
        sleep(1000);
    }

    // ---------------------------------------------------------
    // CSV EXPORT
    // ---------------------------------------------------------
    static void exportMenu(Scanner sc) {
        sec("EXPORT TO CSV");
        item(1, "Export Books");
        item(2, "Export Members");
        item(3, "Export Borrowers");
        item(0, "Back");
        say("");
        switch (ask(sc, "Choice")) {
            case "1" -> doExport("books");
            case "2" -> doExport("members");
            case "3" -> doExport("borrowers");
        }
    }

    static void doExport(String type) {
        String filename = "trove_" + type + "_" + LocalDate.now() + ".csv";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            switch (type) {
                case "books" -> {
                    pw.println("#,Title,Author,Genre,Year,Status,Rating");
                    int i = 1;
                    for (Book b : books)
                        pw.printf("%d,\"%s\",\"%s\",%s,%d,%s,%.1f%n",
                            i++, b.title, b.author, b.genre, b.year, b.status, b.rating);
                }
                case "members" -> {
                    pw.println("#,Name,Year,Roll,Email,Joined,Interests");
                    int i = 1;
                    for (Member m : members)
                        pw.printf("%d,\"%s\",%s,%s,%s,%s,\"%s\"%n",
                            i++, m.name, m.year, m.roll, m.email, m.joined,
                            String.join("|", m.interests));
                }
                case "borrowers" -> {
                    pw.println("#,Member,Book,Borrowed,Due,Status,Fine");
                    int i = 1;
                    for (Borrower b : borrowers)
                        pw.printf("%d,\"%s\",\"%s\",%s,%s,%s,Rs.%d%n",
                            i++, b.memberName, b.bookTitle,
                            b.borrowedStr(), b.dueStr(), b.statusLabel(), b.fine());
                }
            }
            ok("Exported -> " + filename);
        } catch (IOException e) {
            err("Export failed: " + e.getMessage());
        }
        sleep(1200);
    }

    // =========================================================
    // STUDENT MENU
    // =========================================================
    static void studentMenu(Scanner sc) {
        while (true) {
            stuBanner();
            studentStats();
            menuHead("STUDENT PORTAL - " + me.name);
            item(1, "My Dashboard       - Reading list, overdue alerts, recommendations");
            item(2, "Browse Books       - Search, filter, see recommended books");
            item(3, "My Borrowed Books  - Full borrowing history");
            item(4, "My Interests       - Set genres -> get recommendations");
            item(5, "My Profile         - Stats + change password");
            item(0, "Logout");
            say("");
            switch (ask(sc, "Choice")) {
                case "1" -> stuDashboard(sc);
                case "2" -> stuBrowse(sc);
                case "3" -> stuHistory(sc);
                case "4" -> stuInterests(sc);
                case "5" -> stuProfile(sc);
                case "0" -> { me = null; return; }
                default  -> { err("Invalid option."); sleep(500); }
            }
        }
    }

    static void studentStats() {
        List<Borrower> mine = myBorrows();
        long active  = mine.stream().filter(b -> !b.returned()).count();
        long overdue = mine.stream().filter(b -> b.daysOverdue() > 0).count();
        int  fine    = mine.stream().mapToInt(Borrower::fine).sum();
        say("");
        line('=');
        System.out.printf("    %-26s  %-24s  %-24s  %-24s%n",
            CY + BD + "Borrowed: " + mine.size() + R,
            GR + BD + "Reading: " + active + R,
            (overdue > 0 ? RD : YL) + BD + "Overdue: " + overdue + R,
            (fine > 0 ? RD : GR)   + BD + "Fine: Rs." + fine + R);
        line('=');
    }

    static void stuDashboard(Scanner sc) {
        ph("DASHBOARD", "My Dashboard", me.name);
        List<Borrower> active = myBorrows().stream()
            .filter(b -> !b.returned()).collect(Collectors.toList());
        say("");
        say(BD + "  Currently Reading:" + R);
        say("");
        if (active.isEmpty()) {
            inf("No books currently borrowed. Browse the collection!");
        } else {
            for (Borrower b : active) {
                String dc = b.daysOverdue() > 0 ? RD
                          : b.statusLabel().contains("soon") ? YL : GR;
                System.out.printf("  %-36s  Due: %s%-12s%s  %s%n",
                    CY + w(b.bookTitle, 36) + R, dc, b.dueStr(), R,
                    b.daysOverdue() > 0
                        ? RD + BD + "OVERDUE " + b.daysOverdue() + "d  Fine: Rs." + b.fine() + R
                        : b.statusLabel().contains("soon") ? YL + "Due soon" + R : GR + "On time" + R);
            }
        }
        List<Borrower> over = active.stream()
            .filter(b -> b.daysOverdue() > 0).collect(Collectors.toList());
        if (!over.isEmpty()) {
            say("");
            say(RD + BD + "  *** OVERDUE - Please return these books immediately! ***" + R);
            for (Borrower b : over)
                System.out.printf("     >> %-34s  %d days late  Fine: Rs.%d%n",
                    b.bookTitle, b.daysOverdue(), b.fine());
        }
        say("");
        say(BD + "  Recommended For You (based on your interests):" + R);
        say("");
        printRecs(6);
        enter(sc);
    }

    static void stuBrowse(Scanner sc) {
        sec("BROWSE BOOKS");
        String q  = ask(sc, "Search (title/author/genre - blank = all)").toLowerCase();
        String g  = pickGenre(sc, "Filter Genre (blank = all)");
        String st = ask(sc, "Filter Status [Available / Issued / blank]");
        List<String> ints = me.interests;
        List<Book> res = books.stream()
            .filter(b -> q.isEmpty() || b.title.toLowerCase().contains(q)
                || b.author.toLowerCase().contains(q) || b.genre.toLowerCase().contains(q))
            .filter(b -> g.isEmpty()  || b.genre.equalsIgnoreCase(g))
            .filter(b -> st.isEmpty() || b.status.equalsIgnoreCase(st))
            .collect(Collectors.toList());
        ph("BROWSE", "Browse Results", res.size() + " book(s)");
        if (res.isEmpty()) {
            warn("No books match your search.");
        } else {
            say(DM + "  " + w("ID", 4) + "  " + w("Title", 38) + "  " + w("Author", 22)
                + "  " + w("Genre", 12) + "  " + w("Status", 10) + "  Rating   Tag" + R);
            line('-');
            for (Book b : res) {
                boolean isRec = ints.contains(b.genre) && b.available();
                String s = b.available()
                    ? GR + BD + w("Available", 10) + R
                    : RD + BD + w("Issued",    10) + R;
                System.out.printf("  " + CY + "%-4d" + R
                    + "  %-38s  %-22s  %-12s  %s  %s  %s%n",
                    b.id, w(b.title, 38), w(b.author, 22), b.genre, s, b.stars(),
                    isRec ? MG + BD + "[Recommended]" + R : "");
            }
        }
        enter(sc);
    }

    static void stuHistory(Scanner sc) {
        ph("HISTORY", "My Borrowed Books", me.name);
        List<Borrower> mine = myBorrows();
        if (mine.isEmpty()) {
            inf("No borrowing history yet.");
        } else {
            say(DM + "  " + w("Book", 36) + "  " + w("Borrowed", 12) + "  " + w("Due", 12)
                + "  " + w("Status", 18) + "  Fine" + R);
            line('-');
            for (Borrower b : mine) {
                String s;
                if (b.returned())           s = GR + w("Returned", 18) + R;
                else if (b.daysOverdue()>0) s = RD + BD + w("OVERDUE " + b.daysOverdue() + "d", 18) + R;
                else                        s = YL + w(b.statusLabel(), 18) + R;
                System.out.printf("  %-36s  %-12s  %-12s  %s  %s%n",
                    w(b.bookTitle, 36), b.borrowedStr(), b.dueStr(), s,
                    b.fine() > 0 ? RD + "Rs." + b.fine() + R : GR + "Rs.0" + R);
            }
        }
        enter(sc);
    }

    static void stuInterests(Scanner sc) {
        sec("MY READING INTERESTS");
        List<String> cur = new ArrayList<>(me.interests);
        say(BD + "  Current interests: " + R
            + (cur.isEmpty() ? DM + "None set" + R : MG + String.join(", ", cur) + R));
        say("");
        say("  Available genres - enter numbers to toggle:");
        say("");
        for (int i = 0; i < GENRES.length; i++) {
            boolean sel = cur.contains(GENRES[i]);
            System.out.printf("    " + CY + "[%d]" + R + "  %-15s  %s%n",
                i + 1, GENRES[i],
                sel ? GR + BD + "[Selected]" + R : DM + "[ ]" + R);
        }
        say("");
        inf("Enter numbers to toggle, e.g. 1,3,5 - blank = keep as-is.");
        String input = ask(sc, "Toggle genres");
        if (!input.isBlank()) {
            for (String part : input.split(",")) {
                try {
                    int gi = Integer.parseInt(part.trim()) - 1;
                    if (gi >= 0 && gi < GENRES.length) {
                        String genre = GENRES[gi];
                        if (cur.contains(genre)) cur.remove(genre);
                        else                     cur.add(genre);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        me.interests = cur;
        members.stream().filter(m -> m.email.equalsIgnoreCase(me.email))
            .findFirst().ifPresent(m -> m.interests = new ArrayList<>(cur));
        users.stream().filter(u -> u.email.equalsIgnoreCase(me.email))
            .findFirst().ifPresent(u -> u.interests = new ArrayList<>(cur));
        notif("Interests updated for " + me.name);
        say("");
        say(BD + "  Updated interests: " + R
            + (cur.isEmpty() ? DM + "None" + R : MG + String.join(", ", cur) + R));
        say("");
        say(BD + "  Books You Might Like (sorted by rating):" + R);
        say("");
        printRecs(8);
        enter(sc);
    }

    static void printRecs(int limit) {
        if (me.interests.isEmpty()) {
            inf("Set your interests to see personalised recommendations!");
            return;
        }
        List<Book> recs = books.stream()
            .filter(b -> me.interests.contains(b.genre) && b.available())
            .sorted(Comparator.comparingDouble((Book b) -> b.rating).reversed())
            .limit(limit).collect(Collectors.toList());
        if (recs.isEmpty()) { inf("No available books match your interests right now."); return; }
        for (Book b : recs)
            System.out.printf("  >> %-36s  " + DM + "by %-22s" + R + "  [%-12s]  %s%n",
                b.title, b.author, b.genre, b.stars());
    }

    static void stuProfile(Scanner sc) {
        sec("MY PROFILE");
        say("");
        stat("Name",      me.name,  "cyan");
        stat("Email",     me.email, "cyan");
        stat("Roll No",   me.roll,  "cyan");
        stat("Year",      me.year,  "cyan");
        stat("Interests", me.interests.isEmpty() ? "None" : String.join(", ", me.interests), "magenta");
        List<Borrower> mine = myBorrows();
        stat("Total Borrowed",   String.valueOf(mine.size()), "green");
        long active  = mine.stream().filter(b -> !b.returned()).count();
        long overdue = mine.stream().filter(b -> b.daysOverdue() > 0).count();
        int  fine    = mine.stream().mapToInt(Borrower::fine).sum();
        stat("Currently Reading", String.valueOf(active),  "green");
        stat("Overdue Books",     String.valueOf(overdue), overdue > 0 ? "red" : "green");
        stat("Fine Due",          "Rs." + fine,            fine > 0 ? "red" : "green");
        say("");
        item(1, "Change Password");
        item(0, "Back");
        say("");
        if ("1".equals(ask(sc, "Choice"))) changePassword(sc);
    }

    // =========================================================
    // HELPERS
    // =========================================================
    static List<Borrower> myBorrows() {
        return borrowers.stream()
            .filter(b -> b.memberEmail.equalsIgnoreCase(me.email))
            .collect(Collectors.toList());
    }

    static Book   findBook(int id)   { return books.stream().filter(b -> b.id == id).findFirst().orElse(null); }
    static Member findMember(int id) { return members.stream().filter(m -> m.id == id).findFirst().orElse(null); }

    static String pickGenre(Scanner sc, String prompt) {
        System.out.print(DM + "  Genres: ");
        for (int i = 0; i < GENRES.length; i++)
            System.out.print((i + 1) + "." + GENRES[i] + "  ");
        System.out.println(R);
        String in = ask(sc, prompt + " (number or name, blank = skip)");
        if (in.isBlank()) return "";
        try {
            int gi = Integer.parseInt(in) - 1;
            if (gi >= 0 && gi < GENRES.length) return GENRES[gi];
        } catch (NumberFormatException ignored) {}
        for (String g : GENRES) if (g.equalsIgnoreCase(in)) return g;
        return in;
    }

    static int intAsk(Scanner sc, String label) {
        System.out.print(YL + "  > " + label + ": " + R);
        try { return Integer.parseInt(sc.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    static String ask(Scanner sc, String label) {
        System.out.print(YL + "  > " + label + ": " + R);
        return sc.nextLine().trim();
    }

    static String askOpt(Scanner sc, String label, String cur) {
        System.out.print(YL + "  > " + label + " [" + DM + cur + R + YL + "]: " + R);
        return sc.nextLine().trim();
    }

    // =========================================================
    // UI PRIMITIVES
    // =========================================================
    static void say(String s)  { System.out.println(s); }
    static void ok(String s)   { say(GR + BD + "  [OK]  " + s + R); }
    static void err(String s)  { say(RD + BD + "  [ERR] " + s + R); }
    static void warn(String s) { say(YL + BD + "  [!]   " + s + R); }
    static void inf(String s)  { say(BL       + "  [i]   " + s + R); }
    static void sleep(int ms)  { try { Thread.sleep(ms); } catch (Exception ignored) {} }
    static void line(char c)   { say(DM + String.valueOf(c).repeat(82) + R); }

    static void enter(Scanner sc) {
        say("");
        System.out.print(DM + "  [ Press ENTER to continue ] " + R);
        sc.nextLine();
    }

    static void sec(String title) {
        say(""); line('-');
        say(BD + CY + "  " + title + R);
        line('-');
    }

    static void ph(String section, String title, String sub) {
        say("");
        say(BD + BGC + BK + "  " + section + " > " + title + "  " + R + DM + "  " + sub + R);
        line('-');
    }

    static void menuHead(String t) {
        say(""); say(BD + MG + "  +-- " + t + " --+" + R); say("");
    }

    static void item(int n, String label) {
        System.out.printf("    " + CY + "[%2d]" + R + "  %s%n", n, label);
    }

    static void stat(String label, String value, String color) {
        String c = switch (color) {
            case "green"   -> GR;
            case "red"     -> RD;
            case "yellow"  -> YL;
            case "magenta" -> MG;
            case "cyan"    -> CY;
            default        -> WH;
        };
        System.out.printf("    %-22s  " + c + BD + "%s" + R + "%n",
            DM + label + ":" + R, value);
    }

    static String w(String s, int max) {
        if (s == null) s = "";
        if (s.length() > max) s = s.substring(0, max - 1) + ".";
        return String.format("%-" + max + "s", s);
    }

    static void banner() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        say("");
        say(CY + BD + "  ============================================" + R);
        say(CY + BD + "    TROVE - A Treasure of Books" + R);
        say(CY + BD + "    Library Management System" + R);
        say(CY + BD + "  ============================================" + R);
        say(""); line('=');
    }

    static void stuBanner() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        say("");
        say(MG + BD + "  ============================================" + R);
        say(MG + BD + "    TROVE - Student Portal" + R);
        say(MG + BD + "  ============================================" + R);
        say(""); line('=');
    }
}
