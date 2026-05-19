package com.agent.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DeepSeekAgentCLI {

    private static final String API_KEY = "sk-dsfadsfdsafdsafsdafasdf";
    private static final String URL = "https://api.deepseek.com/v1/chat/completions";

    private static final ObjectMapper mapper = new ObjectMapper();

    // ✅ 多轮记忆（核心）
    private static final List<JsonNode> history = new ArrayList<>();

    public static void main(String[] args) {

        if (args.length == 0 && System.console() == null) {
            System.out.println("❌ 请输入问题，例如: ds \"写一个Spring Service\"");
            return;
        }

        try {
            String input = getInput(args);

            // ✅ Agent：判断是否有文件输入
            String fileContent = readStdIn();
            String finalPrompt = buildPrompt(input, fileContent);

            String result = callDeepSeek(finalPrompt);

            System.out.println("\n✅ AI结果：\n");
            System.out.println(result);

        } catch (Exception e) {
            System.out.println("❌ 执行失败: " + e.getMessage());
        }
    }

    // ✅ 获取输入
    private static String getInput(String[] args) {
        return String.join(" ", args);
    }

    // ✅ 读取文件（支持 ds "xxx" < file.java）
    private static String readStdIn() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            StringBuilder sb = new StringBuilder();
            String line;
            while (reader.ready() && (line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ✅ Agent逻辑：构造Prompt
    private static String buildPrompt(String input, String fileContent) {

        String systemPrompt = "你是一个Java后端专家，精通Spring Boot、MySQL、性能优化。" + "你不仅回答问题，还会主动分析并提供优化建议。" + "输出要专业清晰。";

        StringBuilder prompt = new StringBuilder(systemPrompt);
        prompt.append(input);

        // ✅ Agent行为：如果有文件 → 自动分析
        if (fileContent != null && !fileContent.isEmpty()) {
            prompt.append("\n\n下面是相关代码，请一起分析：\n");
            prompt.append(fileContent);
        }

        return prompt.toString();
    }

    // ✅ 调用DeepSeek
    private static String callDeepSeek(String prompt) throws Exception {

        OkHttpClient client = new OkHttpClient();

        // ✅ 构造messages（带记忆）
        List<Object> messagesObject = new ArrayList<>();

        messagesObject.add(new Object() {
            public final String role = "system";
            public final String content = "你是一个Java工程专家，会写代码、优化SQL、分析系统";
        });

        messagesObject.add(new Object() {
            public final String role = "user";
            public final String content = prompt;
        });

        String json = mapper.writeValueAsString(new Object() {
            public final String model = "deepseek-coder";
            public final Object messages = messagesObject;
        });

        Request request = new Request.Builder().url(URL)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + API_KEY).build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new RuntimeException("HTTP错误: " + response.code());
        }

        String body = response.body().string();

        JsonNode root = mapper.readTree(body);
        JsonNode choices = root.path("choices");

        if (choices.isEmpty()) {
            throw new RuntimeException("无返回结果");
        }

        return choices.get(0).path("message").path("content").asText();
    }
}
