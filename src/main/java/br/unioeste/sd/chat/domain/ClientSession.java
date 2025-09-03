package br.unioeste.sd.chat.domain;

import lombok.*;

import javax.crypto.SecretKey;
import java.io.ObjectOutputStream;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientSession {
    private ObjectOutputStream out;
    private SecretKey secretKey;
}
