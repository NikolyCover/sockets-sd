package br.unioeste.sd.chat.domain;

import lombok.*;

import java.io.Serializable;

@Builder @Data
@AllArgsConstructor @NoArgsConstructor
public class Message implements Serializable {
    String sender;
    String recipient;
    String content;
}
