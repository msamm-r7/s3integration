package com.sammm.spring.aws;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.junit4.SpringRunner;

import org.springframework.messaging.MessageChannel;

import org.springframework.integration.aws.outbound.S3MessageHandler;

@RunWith(SpringRunner.class)
@SpringBootTest
public class S3integrationApplicationTests {
    
    @Autowired 
    private MessageHandler s3MessageHandler;
    
    @Autowired
    private MessageChannel s3SendChannel;
    
	@Test
	public void contextLoads() {
	}
	
	@Test
	public void doUploadWithMessageHandler() throws IOException, URISyntaxException { 

	    
	    ClassLoader classLoader = getClass().getClassLoader();
	    File file = new File(classLoader.getResource("uploadData.txt").getFile());
	    
	    
	    Message<?> uploadMessage =
	        MessageBuilder.withPayload(file)
            .setHeader("s3Command", S3MessageHandler.Command.UPLOAD.name())
            .build();

	    //s3MessageHandler.handleMessage(uploadMessage);
	    this.s3SendChannel.send(uploadMessage);
	    
	}

}
