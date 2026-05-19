
import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeepSeekClient {

    private static final String API_KEY = System.getenv("DEEPSEEK_KEY");
    private static final String URL = "https://api.deepseek.com/v1/chat/completions";

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("❌ 请输入问题，例如:");
            System.out.println("ds \"写一个Spring批量更新方法\"");
            return;
        }

        String prompt = String.join(" ", args);

        try {
            String result = callDeepSeek(prompt);
            System.out.println("\n✅ AI 返回结果：\n");
            System.out.println(result);
        } catch (Exception e) {
            System.out.println("❌ 调用失败:");
            System.out.println(e.getMessage());
        }
    }

    private static String callDeepSeek(String prompt) throws Exception {

        OkHttpClient client = new OkHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        String json = mapper.writeValueAsString(new Object() {
            public final String model = "deepseek-coder";
            public final Object[] messages = new Object[]{
                    new Object() {
                        public final String role = "system";
                        public final String content = "你是一个精通Java、Spring Boot、MySQL的高级工程师，输出高质量代码";
                    },
                    new Object() {
                        public final String role = "user";
                        public final String content = prompt;
                    }
            };
        });

        Request request = new Request.Builder()
                .url(URL)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new RuntimeException("HTTP错误: " + response.code());
        }

        String body = response.body().string();

        JsonNode root = mapper.readTree(body);
        JsonNode choices = root.path("choices");

        if (choices.isEmpty()) {
            throw new RuntimeException("没有返回结果");
        }

        return choices.get(0)
                .path("message")
                .path("content")
                .asText();
    }
}
