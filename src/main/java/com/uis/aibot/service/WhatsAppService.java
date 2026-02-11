package com.uis.aibot.service;

import it.auties.whatsapp.api.QrHandler;
import it.auties.whatsapp.api.Whatsapp;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import it.auties.whatsapp.model.message.standard.TextMessage;

@Service
public class WhatsAppService {

    private Whatsapp whatsapp;

    @PostConstruct
    public void startBot() {
        this.whatsapp = Whatsapp.webBuilder()
                .newConnection()
                .unregistered(QrHandler.toTerminal()) // Prints QR in console
                .addLoggedInListener(api -> System.out.println("Bot is Online!"))
                .addNewChatMessageListener(info -> {
                    // 1. Get the content (the actual 'letter' inside the envelope)
                    var content = info.message().content();

                    // 2. Check if it's actually there and if it's a TextMessage
                    if (content != null && content instanceof TextMessage textMsg) {
                        String text = textMsg.text();
                        System.out.println("Message: " + text);
                    }
                })
                .connect()
                .join();
    }
}