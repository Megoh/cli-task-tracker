public class Main {
    public static void main(String[] args) {
        if (args.length > 0) {
            String command = args[0];
            try {
                switch (Command.valueOf(command.toUpperCase())) {
                    case Command.ADD -> System.out.println("Task added.");
                    case Command.UPDATE -> System.out.println("Task updated.");
                    case Command.DELETE -> System.out.println("Task deleted.");
                    case Command.MARK_IN_PROGRESS -> System.out.println("Task in progress.");
                    case Command.MARK_DONE -> System.out.println("Task done.");
                    case Command.LIST -> System.out.println("Task(s) listed.");
                    default -> System.out.println("Wrong argument. Try again.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid command: \"" + command + "\" is not recognized.");
                System.out.println("Valid commands are:");
                for (Command c : Command.values()) {
                    System.out.println("  " + c.toString().toLowerCase());
                }
                System.out.println("Usage: java Main <command>");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No arguments provided.");
            System.out.println("Usage: java Main <command>");
        }
    }

    enum Command {
        ADD,
        UPDATE,
        DELETE,
        MARK_IN_PROGRESS,
        MARK_DONE,
        LIST
    }
}