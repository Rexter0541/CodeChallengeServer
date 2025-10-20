import spark.Spark;
import org.codehaus.janino.SimpleCompiler;
import java.io.*;
import java.lang.reflect.Method;

public class CodeServer {
    public static void main(String[] args) {
        Spark.port(8080);

        Spark.get("/", (req, res) -> "Server is running!");

        Spark.post("/run", (req, res) -> {
            res.type("text/plain");
            String code = req.queryParams("code");
            if (code == null || code.isEmpty()) return "Error: No code received!";

            try {
                code = code.replace("public class", "class"); // Janino fix
                SimpleCompiler compiler = new SimpleCompiler();
                compiler.cook(code);

                String className = extractClassName(code);
                if (className == null) return "Error: Could not find class name!";

                Class<?> cls = compiler.getClassLoader().loadClass(className);

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(output);
                PrintStream oldOut = System.out;
                System.setOut(ps);

                Method mainMethod = cls.getDeclaredMethod("main", String[].class);
                mainMethod.setAccessible(true);
                mainMethod.invoke(null, (Object) new String[]{});

                System.setOut(oldOut);
                return output.toString().trim();
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        });
    }

    private static String extractClassName(String code) {
        code = code.replace("\n", " ").replace("\r", " ");
        String[] tokens = code.split("\\s+");
        for (int i = 0; i < tokens.length - 1; i++) {
            if (tokens[i].equals("class")) return tokens[i + 1];
        }
        return null;
    }
}
