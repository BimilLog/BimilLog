package jaeik.growfarm.infrastructure.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>운영용 ElasticSearch 설정</h2>
 * <p>AWS OpenSearch Serverless를 활용한다.</p>
 * @author Jaeik
 * @version  2.0.0
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String esUri;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(HttpHost.create(esUri)).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
