package jaeik.growfarm.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>Swagger 설정 클래스</h2>
 * <p>
 * API 문서화를 위한 Swagger 설정
 * </p>
 * 
 * @since 1.0.0
 * @author Jaeik
 */
@Configuration
public class SwaggerConfig {

    /**
     * <h3>OpenAPI 설정</h3>
     *
     * <p>
     * Swagger UI를 위한 OpenAPI 설정을 구성한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @return OpenAPI 설정 객체
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GrowFarm API")
                        .description("GrowFarm 서비스 API 문서입니다.")
                        .version("v1.0"))
                .addSecurityItem(new SecurityRequirement().addList("JWT"))
                .components(new Components().addSecuritySchemes("JWT",
                        new SecurityScheme()
                                .name("JWT")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)));
    }

    /**
     * <h3>API 그룹 설정</h3>
     *
     * <p>
     * API를 그룹별로 분류하여 문서화한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @return 그룹화된 OpenAPI 설정
     */
    @Bean
    public GroupedOpenApi groupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("GrowFarm API")
                .pathsToMatch("/**")
                .build();
    }
}
