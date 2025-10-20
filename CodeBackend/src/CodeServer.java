import spark.Spark;
import org.codehaus.janino.SimpleCompiler;
import java.io.*;
import java.lang.reflect.Method;

public class CodeServer {
    public static void main(String[] args) {
        Spark.port(8080);

        // ✅ Health check
        Spark.get("/", (req, res) -> "Server is running!");

        // 🧠 Code execution route
        Spark.post("/run", (req, res) -> {
            res.type("text/plain");

            String code = req.queryParams("code");
            String idParam = req.queryParams("challenge_id");
            int challengeId = idParam != null ? Integer.parseInt(idParam) : -1;

            if (code == null || code.isEmpty()) {
                return "Error: No code received!";
            }

            // ✅ Expected outputs (must match Unity order)
            String[] expectedOutputs = new String[]{
                "Hello, NPC!",
                "Hello, NPC!",
                "Hello, NPC!",
                "Hello, NPC!",
                "15",
                "JAVA",
                "2",
                "Loop 0\nLoop 1\nLoop 2",
                "Coding is fun!",
                "Hello, NPC",
                "6"
            };

            // ✅ Basic structure validation rules
            String[] requiredSnippets = new String[]{
                "System.out.println(\"Hello, NPC!\")",
                "class Test",
                "static void main",
                "Hello, NPC",
                "x + y",
                "toUpperCase",
                "nums[1]",
                "for(int i",
                "if(isJavaFun)",
                "\"Hello, \" + name",
                "a * b"
            };

            try {
                // Safety fix for Janino
                code = code.replace("public class", "class");

                // ⚙️ Compile
                SimpleCompiler compiler = new SimpleCompiler();
                compiler.cook(code);

                String className = extractClassName(code);
                if (className == null) {
                    return "Error: Could not find class name!";
                }

                Class<?> cls = compiler.getClassLoader().loadClass(className);

                // Redirect stdout
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(output);
                PrintStream oldOut = System.out;
                System.setOut(ps);

                Method mainMethod = cls.getDeclaredMethod("main", String[].class);
                mainMethod.setAccessible(true);
                mainMethod.invoke(null, (Object) new String[]{});

                System.setOut(oldOut);
                String result = output.toString().trim();

                // 🧩 Challenge validation
                if (challengeId >= 0 && challengeId < expectedOutputs.length) {
                    String expected = expectedOutputs[challengeId].trim();
                    String mustContain = requiredSnippets[challengeId];

                    boolean structureValid = code.contains(mustContain);
                    boolean outputValid = result.equals(expected);

                    if (structureValid && outputValid) {
                        return "CORRECT";
                    } else if (!structureValid) {
                        return "WRONG (Your code structure doesn't match the challenge!)";
                    } else {
                        return "WRONG (Expected: " + expected + ", Got: " + result + ")";
                    }
                } else {
                    return "Error: Invalid challenge ID!";
                }

            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        });
    }

    // ✅ Extract class name for Janino
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
