package cn.extrasky.zebra.config;

import cn.extrasky.zebra.BufferAllocatorTemplate;
import cn.extrasky.zebra.cache.RedisClient;
import cn.extrasky.zebra.exception.IdGeneratorException;
import cn.extrasky.zebra.model.IdStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;

/**
 * @description:
 * @author: 田培融
 * @date: 2019-09-25 18:42
 */
// 相当于一个普通的 java 配置类
@Configuration
// 当 HelloworldService 在类路径的条件下
@ConditionalOnClass({BufferAllocatorTemplate.class})
// 将 application.properties 的相关的属性字段与该类一一对应，并生成 Bean
@EnableConfigurationProperties({BufferProperties.class,IdStoreProperties.class})
public class BufferAllocatorTemplateAutoConfiguration {

    // 注入属性类
    @Autowired
    private BufferProperties bufferProperties;

    @Bean
    // 当容器没有这个 Bean 的时候才创建这个 Bean
    @ConditionalOnMissingBean(BufferAllocatorTemplate.class)
    public BufferAllocatorTemplate helloworldService() throws IdGeneratorException {

        RedisConnectionFactory.RedisProperties properties = new RedisConnectionFactory.RedisProperties();
        properties.setHost(bufferProperties.getHost());
        properties.setDatabase(bufferProperties.getDatabase());
        properties.setPort(bufferProperties.getPort());
        properties.setPassword(bufferProperties.getPassword());
        properties.setPrefix(bufferProperties.getPrefix());
        RedisConnectionFactory factory =  RedisConnectionFactory.with(properties).build();
        RedisClient redisClient = new RedisClient();
        redisClient.setRedisClientFactory( () -> {
            Jedis jedis = factory.fetchJedisConnector();
            jedis.select(bufferProperties.getDatabase());
            return jedis;
        });
        BufferAllocatorTemplate template = BufferAllocatorTemplate.start(redisClient);
        if (CollectionUtils.isEmpty(bufferProperties.getIdStoreList())){
            throw new IdGeneratorException("initialize allocator error, because of lack store configuration");
        }
        for (IdStoreProperties idStore : bufferProperties.getIdStoreList()) {
            template.build(new IdStore().setKey(idStore.getKey()).setStep(idStore.getStep()).setFactor(idStore.getFactor()).setWasteQuota(idStore.getWasteQuota()));
        }
        return template;
    }
}
