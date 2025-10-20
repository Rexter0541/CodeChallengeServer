import spark.Spark;
import org.codehaus.janino.SimpleCompiler;
import java.io.*;
import java.lang.reflect.Method;
import java.util.Map;

public class CodeServer {
    public static void main(String[] args) {
        Spark.port(8080);

        // ✅ Health check
        Spark.get("/", (req, res) -> "Server is running!");

        // Expected outputs for your 4 challenges
        Map<Integer, String> expectedOutputs = Map.of(
            0, "Hello, NPC!",
            1, "Hello, NPC!",
            2, "Hello, NPC!",
            3, "Hello, NPC!"
        );

        // 🧠 Code execution route
        Spark.post("/run", (req, res) -> {
            res.type("text/plain");

            String code = req.queryParams("code");
            String challengeIdStr = req.queryParams("challenge_id");

            if (code == null || code.isEmpty() || challengeIdStr == null) {
                return "Error: Missing code or challenge ID!";
            }

            int challengeId;
            try {
                challengeId = Integer.parseInt(challengeIdStr);
            } catch (NumberFormatException e) {
                return "Error: Invalid challenge ID!";
            }

            if (!expectedOutputs.containsKey(challengeId)) {
                return "Error: Unknown challenge ID!";
            }

            try {
                // Remove public class for Janino
                code = code.replace("public class", "class");

                // Compile dynamically
                SimpleCompiler compiler = new SimpleCompiler();
                compiler.cook(code);

                // Extract class name
                String className = extractClassName(code);
                if (className == null) {
                    return "Error: Could not find class name!";
                }

                Class<?> cls = compiler.getClassLoader().loadClass(className);

                // Capture System.out
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(output);
                PrintStream oldOut = System.out;
                System.setOut(ps);

                // Run main
                Method mainMethod = cls.getDeclaredMethod("main", String[].class);
                mainMethod.setAccessible(true);
                mainMethod.invoke(null, (Object) new String[]{});

                // Restore System.out
                System.setOut(oldOut);

                String result = output.toString().trim();

                // ✅ Strict check
                if (result.equals(expectedOutputs.get(challengeId))) {
                    return "CORRECT";
                } else {
                    return "INCORRECT";
                }

            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        });
    }

    private static String extractClassName(String code) {
        code = code.replace("\n", " ").replace("\r", " ");
        String[] tokens = code.split("\\s+");
        for (int i = 0; i < tokens.length - 1; i++) {
            if (tokens[i].equals("class")) {
                return tokens[i + 1];
            }
        }
        return null;
    }
}
