package com.usinsa.backend.global.init;

import org.apache.commons.lang3.SystemUtils;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Profile("dev")
@Configuration
public class DevInitData {

    @Bean
    public ApplicationRunner devApplicationRunner() {
        return args -> {
            String apiDocsUrl = "http://localhost:8080/v3/api-docs/apiV1"; // Swagger 그룹
            String apiJsonPath = "apiV1.json"; // backend 기준 저장
            String frontendSchemaPath = "../usinsa-fe/usinsa/libs/backend/apiV1/schema.d.ts"; // FE 타입 경로

            // 1) 최신 API JSON 자동 생성
            genApiJsonFile(apiDocsUrl, apiJsonPath);

            // 2) Typescript schema 자동 생성
            runCmd(getGenerateSchemaCommand(apiJsonPath, frontendSchemaPath));
        };
    }

    /**
     * openapi-typescript 스키마 생성 명령어 구성
     */
    private List<String> getGenerateSchemaCommand(String jsonPath, String tsOutputPath) {
        String command = String.format(
                "npx --package typescript --package openapi-typescript --package punycode " +
                        "openapi-typescript %s -o %s", jsonPath, tsOutputPath);

        if (SystemUtils.IS_OS_WINDOWS) {
            return List.of("cmd.exe", "/c", command);
        } else {
            return List.of("sh", "-c", command);
        }
    }

    /**
     * OS 대응 커맨드 실행
     */
    private void runCmd(List<String> command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("명령어 실행 완료, 종료 코드: " + exitCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * OpenAPI JSON 저장
     */
    private void genApiJsonFile(String url, String filename) {
        Path filePath = Path.of(filename);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Files.writeString(filePath, response.body(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);

                System.out.println("✅ OpenAPI JSON 생성 완료: " + filePath.toAbsolutePath());
            } else {
                System.err.println("❌ API 문서 다운로드 실패 (HTTP " + response.statusCode() + ")");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
