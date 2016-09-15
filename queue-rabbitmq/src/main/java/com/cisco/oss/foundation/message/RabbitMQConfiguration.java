package com.cisco.oss.foundation.message;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by Yair Ogen (yaogen) on 15/09/2016.
 */
@Configuration
@PropertySource("classpath:queue-rabbitmq.properties")
public class RabbitMQConfiguration {
}
