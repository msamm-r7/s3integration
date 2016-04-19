package com.sammm.spring.aws;

import java.util.concurrent.CountDownLatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.aws.outbound.S3MessageHandler;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.messaging.MessageHandler;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.transfer.PersistableTransfer;
import com.amazonaws.services.s3.transfer.internal.S3ProgressListener;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

@SpringBootApplication
public class S3integrationApplication {
    
    private static final String bucketName = "test.upload.aws-dev.us-east-1.insight.rapid7.com";
    
    private static final String keyPrefix = "/dev/msamm/";

	public static void main(String[] args) {
		SpringApplication.run(S3integrationApplication.class, args);
	}
	
	/** 
	 * Setup up our S3 Client using env/AWSAML config.
	 * See here:
	 * <ul>
	 * <li><a href="http://cloud.spring.io/spring-cloud-aws/spring-cloud-aws.html">Spring Cloud AWS Documentation</a></li>
	 * <li><a href="https://github.com/spring-projects/spring-integration-aws"></a>
	 * </ul> 
	 */
	
	@Bean
	public DefaultAWSCredentialsProviderChain defaultAWSCredentialsProviderChain() { 
	    
	    return new DefaultAWSCredentialsProviderChain();
	    
	}
	
	@Bean
	public ClientConfiguration clientConfiguration() { 
	    return 
	        new ClientConfiguration()
            .withProtocol(Protocol.HTTPS)
            ;
	}
	
	@Bean
	public S3ClientOptions s3ClientOptions() {  
        return 
            new S3ClientOptions()
            // required otherwise we run into SSL Cert issues
            .withPathStyleAccess(true)
            ;
	}
	
	private static final Region REGION = Regions.getCurrentRegion();
    private static SpelExpressionParser PARSER = new SpelExpressionParser();
	
	@Bean
	public AmazonS3 amazonS3() { 
	    AmazonS3 s3client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain(), clientConfiguration());
        s3client.setS3ClientOptions(s3ClientOptions());
        return s3client;
	}
	
	

    @Bean
    @ServiceActivator(inputChannel = "s3SendChannel")
    public MessageHandler s3MessageHandler() {
        S3MessageHandler s3MessageHandler = new S3MessageHandler(amazonS3(), bucketName);
        s3MessageHandler.setCommandExpression(PARSER.parseExpression("headers.s3Command"));

        Expression keyExpression =
            PARSER.parseExpression("payload instanceof T(java.io.File) ? payload.name : headers.key");
        s3MessageHandler.setKeyExpression(keyExpression);
        s3MessageHandler.setObjectAclExpression(new ValueExpression<>(CannedAccessControlList.PublicReadWrite));
        s3MessageHandler.setProgressListener(s3ProgressListener());
        
        
        return s3MessageHandler;
    }


    @Bean
    public S3ProgressListener s3ProgressListener() {
        return new S3ProgressListener() {

            @Override
            public void onPersistableTransfer(PersistableTransfer persistableTransfer) {

            }

            @Override
            public void progressChanged(ProgressEvent progressEvent) {
                if (ProgressEventType.TRANSFER_COMPLETED_EVENT.equals(progressEvent.getEventType())) {
                    transferCompletedLatch().countDown();
                }
            }

        };
    }

    @Bean
    public CountDownLatch transferCompletedLatch() {
        return new CountDownLatch(1);
    }
    
    
}
