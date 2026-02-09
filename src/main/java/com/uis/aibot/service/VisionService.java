package com.uis.aibot.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.ai.content.Media;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class VisionService {
    private final ChatClient visionChatClient;
    private static final Logger logger = LoggerFactory.getLogger(VisionService.class);
    public VisionService(@Qualifier("visionChatClient") ChatClient visionChatClient) {
        this.visionChatClient = visionChatClient;
    }

    // Text-only to vision
    public Flux<String> askText(String prompt) {
        return visionChatClient.prompt(prompt).stream().content();
    }




public UserMessage buildVisionUserMessage(String prompt,
                                              String  imageBase64,
                                              String mimeTypes) {
        var mediaList = new ArrayList<Media>();
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            Resource imageResource = new org.springframework.core.io.ByteArrayResource(imageBytes);
            MimeType type = (mimeTypes != null)
                    ? MimeType.valueOf(mimeTypes)
                    : MimeTypeUtils.IMAGE_JPEG;
            mediaList.add(Media.builder().data(imageResource).mimeType(type).build());
        }

        UserMessage.Builder b = UserMessage.builder().text(prompt);
        if (!mediaList.isEmpty()) {
            b.media(mediaList);
        }
        return b.build();
    }

    // Text + image to vision
    public Mono<String> describeImage(String prompt,
                                      String imageBase64,
                                      String mimeTypes) {
           UserMessage user = buildVisionUserMessage(prompt, imageBase64, mimeTypes);
           return  Mono.fromCallable(() ->
                        visionChatClient.prompt()
                       .messages(user)
                       .call()
                       .content()
                       ).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                       .doOnNext( responseText -> {
                           logger.debug("Vision model response text: {}", responseText);
                   });
    }
}
