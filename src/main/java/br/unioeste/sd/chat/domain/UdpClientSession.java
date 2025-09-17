package br.unioeste.sd.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UdpClientSession {
    private InetSocketAddress address;
    private SecretKey secretKey;
}