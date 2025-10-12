import spark.Spark;
import org.codehaus.janino.SimpleCompiler;
import java.io.*;

public class CodeServer {
    public static void main(String[] args) {
        Spark.port(8080);

        // ✅ Health check
        Spark.get("/", (req, res) -> "Server is running!");

        // 🧠 Code execution route
        Spark.post("/run", (req, res) -> {
            res.type("text/plain");

            // Get code from form field "code"
            String code = req.queryParams("code");

            if (code == null || code.isEmpty()) {
                return "Error: No code received!";
            }

            try {
                // ✅ Fix: remove "public" before class to prevent Janino error
                code = code.replace("public class", "class");

                // Compile dynamically
                SimpleCompiler compiler = new SimpleCompiler();
                compiler.cook(code);

                // Extract class name from the code
                String className = extractClassName(code);
                if (className == null) {
                    return "Error: Could not find class name!";
                }

                // Load the class
                Class<?> cls = compiler.getClassLoader().loadClass(className);

                // Capture System.out
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(output);
                PrintStream oldOut = System.out;
                System.setOut(ps);

                // Run main method
                cls.getMethod("main", String[].class).invoke(null, (Object) new String[]{});

                // Restore System.out
                System.setOut(oldOut);

                return output.toString().trim();
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        });
    }

    // ✅ Extract first class name from code
    private static String extractClassName(String code) {
        code = code.replace("\n", " ").replace("\r", " "); // remove line breaks
        String[] tokens = code.split("\\s+");
        for (int i = 0; i < tokens.length - 1; i++) {
            if (tokens[i].equals("class")) {
                return tokens[i + 1];
            }
        }
        return null; // no class found
    }
}
