package br.unioeste.sd.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.io.Serializable;

@Data
@AllArgsConstructor
public class HandshakeRequest implements Serializable {
    private String username;
    private String rsaPublicKey;
}