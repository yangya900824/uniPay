package com.issac.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;


/**
 * <一句话功能简述>
 * <功能详细描述>配置信息
 * 
 * @author  Administrator
 * @version  [版本号, 2014-8-29]
 * @see  [相关类/方法]
 * @since  [产品/模块版本]
 */
@Configuration
@ConfigurationProperties(prefix="pay")
@Data
public class SwiftpassConfig {
    
    /**
     * 交易密钥
     */
    private String key ;
    
    /**
     * 商户号
     */
    private String mch_id;
    
    /**
     * 请求url
     */
    private String req_url;
    
    /**
     * 通知url
     */
    private String notify_url;
    
}
